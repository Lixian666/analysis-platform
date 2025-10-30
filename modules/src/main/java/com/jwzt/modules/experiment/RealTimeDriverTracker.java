package com.jwzt.modules.experiment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.*;
import com.jwzt.modules.experiment.filter.LocationSmoother;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.strategy.LoadingStrategyFactory;
import com.jwzt.modules.experiment.strategy.LoadingUnloadingStrategy;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.geo.ShapefileWriter;
import com.jwzt.modules.experiment.utils.third.manage.DataSender;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.vo.EventState;
import com.ruoyi.common.utils.uuid.IdUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.jwzt.modules.experiment.utils.FileUtils.ensureFilePathExists;

/**
 * 实时轨迹增量分析（10s/批等），与批处理 DriverTracker 并行。
 * 重构说明：使用策略模式管理不同装卸业务（火车、板车、地跑等），提高代码可维护性和扩展性。
 * 修改点：增加了"先下后上"的回溯识别逻辑（针对发运业务 SEND），
 *       当检测到下车但未检测到上车时，会回溯历史轨迹查找上车点并将上下车段落入库。
 */
@Service
@Scope("prototype")  // 改为原型模式，支持多线程独立实例，避免状态共享
public class RealTimeDriverTracker {

    @Autowired
    private DataSender dataSender;

    @Autowired
    private BaseConfig baseConfig;
    @Autowired
    private FilePathConfig filePathConfig;
    
    // —— 依赖与基础工具 ——
    @Autowired
    private OutlierFilter outlierFilter;

    @Autowired
    private BoardingDetector detector;
    
    @Autowired
    private LoadingStrategyFactory loadingStrategyFactory;
    
    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;
    
    private final LocationSmoother smoother = new LocationSmoother();

    @Resource
    private ITakBehaviorRecordsService iTakBehaviorRecordsService;
    @Resource
    private ITakBehaviorRecordDetailService iTakBehaviorRecordDetailService;

    // —— 运行参数 ——
    private final int recordPointsSize = FilterConfig.RECORD_POINTS_SIZE;  // 事件识别窗口大小
    private final int backfillHalf = Math.max(1, recordPointsSize / 2);    // 启动会话时向后回填窗口的一半

    // 回溯查找上车点的最大时间（毫秒），避免无限回溯；可按需调整或放到配置中
    private final long SEND_LOOKBACK_MS = 30L * 60L * 1000L; // 30 分钟
    
    // 历史点数量限制（避免内存无限增长）
    // 根据 30 分钟回溯时间，假设每秒1个点 = 1800 个点，保留 3000 个点作为安全值
    // 实时任务运行期间会持续累积，但有上限保护
    private final int MAX_HISTORY_POINTS = 60000;

    // —— 运行态：按卡维护 ——
    private final Map<String, PerCardState> stateByCard = new ConcurrentHashMap<>();
    private final Map<String, VehicleType> vehicleTypeByCard = new ConcurrentHashMap<>();
    
    // 策略实例缓存：每个 tracker 实例持有自己的策略实例，避免多线程串扰，同时保持状态连续性
    private final Map<VehicleType, LoadingUnloadingStrategy> strategyCache = new ConcurrentHashMap<>();

    // shp 输出根目录（沿用你的静态字段/配置方式）
    public static String shpFileRoot = DriverTracker.shpFilePath; // 与现有保持一致

    /**
     * 车辆类型枚举（保持向后兼容）
     * CAR - 火车装卸
     * TRUCK - 板车装卸
     */
    public enum VehicleType { 
        /** 板车装卸 */
        TRUCK, 
        /** 火车装卸 */
        CAR 
    }

    /** 每张卡的运行态 */
    private static class PerCardState {
        Deque<LocationPoint> window = new ArrayDeque<>();
        List<LocationPoint> historyPoints = new ArrayList<>();
        long sendLastEventTime = Long.MIN_VALUE;
        String sendLastEventTimeStr = "";
        TrackSession activeSession; // null 表示当前不在一段会话中
        long lastSeenTs = Long.MIN_VALUE; // 去重/乱序截断

    }

    /** 上/下车会话（成对闭环） */
    public static class TrackSession {
        public String sessionId;
        public String cardId;
        public long startTime;
        public double startLongitude;
        public double startLatitude;
        public long endTime;
        public double endLongitude;
        public double endLatitude;
        EventKind kind; // ARRIVED 或 SEND
        public String beaconName; // 最常命中的信标名称
        public String rfidName; // 最常命中的信标RFID名称
        public String area; // 最常命中的信标区域
        public final List<LocationPoint> points = new ArrayList<>();
    }

    /** 事件类型归并：到达业务 与 发运业务 */
    private enum EventKind { TRUCK_ARRIVED, TRUCK_SEND, ARRIVED, SEND }

    @PostConstruct
    public void init() {
        // 这里可做 detector 阈值的全局初始化
    }

    /** 注册/更新某张卡的车辆类型（可选调用，不调用则默认 CAR） */
    public void upsertVehicleType(String cardId, VehicleType type) {
        vehicleTypeByCard.put(cardId, type == null ? VehicleType.CAR : type);
    }

    /**
     * 实时入口：同一批里可以是多卡，或仅一张卡。
     * 要求：points 内的 cardId 字段有值（minhang: int；yuzui: UUID 字符串）
     */
    public void ingest(List<LocationPoint> points) {
        if (points == null || points.isEmpty()) return;

        // 分组：每张卡独立流
        Map<String, List<LocationPoint>> byCard = points.stream()
                .collect(Collectors.groupingBy(this::resolveCardKey));
        byCard.forEach((cardKey, list) -> ingestForCard(cardKey, list));
    }

    /**
     * 实时入口（按卡）：一批10秒的新点（可能乱序/重复）
     */
    public void ingestForCard(String cardKey, List<LocationPoint> batch) {
        if (batch == null || batch.isEmpty()) return;
        PerCardState st = stateByCard.computeIfAbsent(cardKey, k -> new PerCardState());
        
        // 使用局部变量，避免多线程竞态条件
        VehicleType vehicleType = vehicleTypeByCard.getOrDefault(cardKey, VehicleType.CAR);
        
        if (baseConfig.isDevelopEnvironment()){
            st.lastSeenTs = Long.MIN_VALUE;
        }
        // 1) 预处理：时间戳、去异常、排序、去重（<= lastSeenTs）
        List<LocationPoint> cleaned = preprocessBatch(batch, st.lastSeenTs);

        if (cleaned.isEmpty()) return;

        // 更新 lastSeenTs
        st.lastSeenTs = Math.max(st.lastSeenTs, cleaned.get(cleaned.size() - 1).getTimestamp());

        // 2) 逐点推进窗口与会话
        for (LocationPoint p : cleaned) {
            // 滑动窗口推进
            st.window.addLast(p);
            if (st.window.size() > recordPointsSize) {
                st.window.removeFirst();
            }
            
            // 添加到历史点，并限制大小避免无限内存增长
            st.historyPoints.add(p);
            if (st.historyPoints.size() > MAX_HISTORY_POINTS) {
                // 移除最旧的点，保持在限制范围内
                // 这样即使实时任务长时间运行，也不会导致内存溢出
                st.historyPoints.remove(0);
            }
            
            // 窗口未满，不触发检测
            if (st.window.size() < recordPointsSize) {
                // 如果会话已开启，仍要接着收点
                if (st.activeSession != null) {
                    st.activeSession.points.add(p);
                }
                continue;
            }
            // 定位修增
            List<LocationPoint> win = new ArrayList<>(st.window);
            // 调用原有检测器（窗口大小固定）
            List<LocationPoint> newPoints = outlierFilter.stateAnalysis(win);
            
            // 使用策略模式进行事件检测（使用局部变量避免多线程竞态）
            LoadingUnloadingStrategy strategy = getStrategyForVehicleType(vehicleType);
            EventState es = strategy.detectEvent(newPoints, st.historyPoints);

            if (es == null || es.getEvent() == null) {
                if (st.activeSession != null) st.activeSession.points.add(p);
                continue;
            }
            if (st.activeSession != null){
                // 推送实时轨迹
                dataSender.trackPush(es, newPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2), st.activeSession, vehicleType);
            }

            switch (es.getEvent()) {
                case TRUCK_ARRIVED_BOARDING:
                    onStart(cardKey, st, EventKind.TRUCK_ARRIVED, es, win);
                    break;
                case TRUCK_ARRIVED_DROPPING:
                    onEnd(cardKey, st, EventKind.TRUCK_ARRIVED, es, win);
                    break;
                case ARRIVED_BOARDING:
                    onStart(cardKey, st, EventKind.ARRIVED, es, win);
                    break;
                case ARRIVED_DROPPING:
                    onEnd(cardKey, st, EventKind.ARRIVED, es, win);
                    break;

                case SEND_BOARDING:
//                    onStart(cardKey, st, EventKind.SEND, es, win);
                    break;

                case SEND_DROPPING:
                    // **修改点**：如果已有活动会话按原来的 onEnd；如果没有活动会话（未检测到上车），尝试回溯历史查找上车并直接入库
                    if (st.activeSession != null && st.activeSession.kind == EventKind.SEND) {
                        onEnd(cardKey, st, EventKind.SEND, es, win);
                    } else {
                        // 回溯并持久化发运段（先下后上）
                        backfillAndPersistSendSession(cardKey, st, es, vehicleType);
                    }
                    break;

                default:
                    // 其它状态：仅在活动会话中持续累积
                    if (st.activeSession != null) st.activeSession.points.add(p);
            }
        }
    }

    /** 事件起点：开启会话并回填窗口的一半点，保证事件前后的完整性 */
    private void onStart(String cardKey, PerCardState st, EventKind kind, EventState es, List<LocationPoint> win) {
        // 若已有未闭合会话，先丢弃/重置（或可实现异常闭合策略）
        st.activeSession = new TrackSession();
        st.activeSession.sessionId = IdUtils.fastSimpleUUID();
        st.activeSession.cardId = cardKey;
        st.activeSession.startTime = es.getTimestamp();
        st.activeSession.startLongitude = win.get(0).getLongitude();
        st.activeSession.startLatitude = win.get(0).getLatitude();
        st.activeSession.kind = kind;

        // 回填窗口后一半（包含触发点附近的历史）
        int from = Math.max(0, win.size() - backfillHalf) - 1;
        if (from < 0) from = 0;
        st.activeSession.points.addAll(win.subList(from, win.size()));
        dataSender.inYardPush(st.activeSession, vehicleTypeByCard.getOrDefault(cardKey, VehicleType.CAR));
        System.out.println("📥 [" + cardKey + "] 启动会话 " + kind + " @" + new Date(es.getTimestamp()));
    }

    /** 事件终点：闭合会话，落数据（整段会话的全部点），并可选输出 shp */
    private void onEnd(String cardKey, PerCardState st, EventKind expectKind, EventState es, List<LocationPoint> win) {
        if (st.activeSession == null || st.activeSession.kind != expectKind) {
            // 未有匹配的起点，忽略或记录异常
            System.out.println("⚠️ [" + cardKey + "] 终点事件无匹配会话，忽略：" + expectKind);
            return;
        }
        // 完结前把窗口中的后半段也补齐（减少尾部截断）
        int from = Math.max(0, win.size() - backfillHalf) - 2;
        if (from < 0) from = 0;

        List<LocationPoint> frontPoints = new ArrayList<>(
                st.activeSession.points.subList(0, Math.max(0, st.activeSession.points.size() - from))
        );

        // 去重 & 按时间排序（会话内可能因回填/累积产生重复）
        List<LocationPoint> sessionPoints = frontPoints.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                        m -> new ArrayList<>(m.values())));


        st.activeSession.endTime = es.getTimestamp();
        try {
            st.activeSession.endLongitude = sessionPoints.get(sessionPoints.size() - 1).getLongitude();
            st.activeSession.endLatitude = sessionPoints.get(sessionPoints.size() - 1).getLatitude();
            
            // -------统计整个会话期间出现最多的信标信息---------
            VehicleType vehicleType = vehicleTypeByCard.getOrDefault(cardKey, VehicleType.CAR);
            BeaconStatistics beaconStats = getMostFrequentBeaconInSession(st.activeSession.points, vehicleType);
            st.activeSession.beaconName = beaconStats.zoneName;
            st.activeSession.rfidName = beaconStats.zoneNameRfid;
            st.activeSession.area = beaconStats.zone;
            // --------------------------------------
            persistSession(st.activeSession, es.getTimestamp(), sessionPoints);
            dataSender.inParkPush(st.activeSession, vehicleTypeByCard.getOrDefault(cardKey, VehicleType.CAR));
        } catch (Exception e) {
            e.printStackTrace();
        }
//        // 可选：输出 shp（整段轨迹）
//        if (baseConfig.isOutputShp()) {
//            String shp = ensureShpPath(shpFileRoot, st.activeSession.sessionId, expectKind);
//            outputVectorFiles(sessionPoints, shp);
//        }

        System.out.println("📤 [" + cardKey + "] 完成会话 " + expectKind +
                " 点数=" + sessionPoints.size() +
                " 起=" + new Date(st.activeSession.startTime) +
                " 止=" + new Date(es.getTimestamp()));

        // 清空会话
        st.activeSession = null;
    }

    /**
     * 回溯历史轨迹查找发运上车点，并将 上车->本次下车 之间的完整轨迹段入库。
     * 说明：
     *  - 以 st.historyPoints 为数据源向后回溯；
     *  - 对候选历史点构造窗口（recordPointsSize 大小）并再次调用 detector.updateState(...)，
     *    若返回 SEND_BOARDING 则视为上车点。
     * 
     * @param cardKey 卡号
     * @param st 卡的状态
     * @param downEs 下车事件状态
     * @param vehicleType 车辆类型（用于选择正确的策略）
     */
    private void backfillAndPersistSendSession(String cardKey, PerCardState st, EventState downEs, VehicleType vehicleType) {
        try {
            List<LocationPoint> history = st.historyPoints;
            if (history == null || history.isEmpty()) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：history 为空");
                return;
            }
            // 增加最小点数检查，避免后续窗口构造出错
            if (history.size() < 2) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：history 点数太少 (" + history.size() + ")，至少需要2个点");
                return;
            }
            long dropTs = downEs.getTimestamp();
            // 找到历史中与当前下车时间对应的索引（最近的 <= dropTs）
            int endIndex = -1;
            for (int i = history.size() - 1; i >= 0; i--) {
                if (history.get(i).getTimestamp() <= dropTs) {
                    endIndex = i;
                    break;
                }
            }
            if (endIndex < 0) {
                System.out.println("⚠️ [" + cardKey + "] 回溯失败：未在 history 找到对应下车点时间");
                return;
            }
            int listStartIndex = -1;
            for (int i = 0; i < history.size(); i++) {
                if (history.get(i).getTimestamp() > st.sendLastEventTime) {
                    listStartIndex = i;
                    break;
                }
            }
            if (listStartIndex < 0) {
                System.out.println("⚠️ [" + cardKey + "] 回溯失败：未在 history 找到对应上车识别开始时间");
                return;
            }
            
            // 边界检查：确保 listStartIndex 在有效范围内
            if (listStartIndex >= history.size()) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：listStartIndex 超出范围: " + listStartIndex + " >= " + history.size());
                return;
            }
            
            // 向前回溯查找上车（限制最大回溯时间）
            long earliestAllowedTs = Math.max(0L, st.sendLastEventTime);
            int foundStartWindowStartIndex = -1;
            EventState foundStartEventState = null;

            // 从 endIndex 向前扫描，寻找一个 candidate 做为“当前点”去重建窗口
            for (int candidate = listStartIndex; candidate >= 0; candidate++) {
                LocationPoint candPoint = history.get(candidate);
                if (candPoint.getTimestamp() < earliestAllowedTs) break; // 超过回溯上限

                // 构造窗口：以 candidate 为窗口中的中间偏移（尽量保证窗口长度为 recordPointsSize）
                int windowStart = candidate - (recordPointsSize / 2);
                if (windowStart < 0) windowStart = 0;
                int windowEnd = windowStart + recordPointsSize;
                if (windowEnd > history.size()) {
                    // 如果末尾超出，尝试把窗口向前移
                    windowStart = Math.max(0, history.size() - recordPointsSize);
                    windowEnd = windowStart + recordPointsSize;
                }
                if (windowEnd > history.size() || windowEnd - windowStart < recordPointsSize) {
                    // 无法构建完整窗口，跳过
                    continue;
                }
                List<LocationPoint> candidateWindow = new ArrayList<>(history.subList(windowStart, windowEnd));
                // 预处理（去异常/修正），保持与实时一致
                List<LocationPoint> newPoints = outlierFilter.stateAnalysis(candidateWindow);
                
                // 使用策略模式进行事件检测（使用传入的vehicleType，避免多线程竞态）
                LoadingUnloadingStrategy strategy = getStrategyForVehicleType(vehicleType);
                EventState es = null;
                try {
                    es = strategy.detectEvent(newPoints, history);
                } catch (IndexOutOfBoundsException e) {
                    // 详细日志：记录调用前的参数状态
                    System.out.println("🔍 [" + cardKey + "] 准备调用 detectEvent: " +
                            "newPoints.size=" + newPoints.size() +
                            ", history.size=" + history.size() +
                            ", candidate=" + candidate +
                            ", windowStart=" + windowStart +
                            ", windowEnd=" + windowEnd);
                    System.err.println("异常日志 ❌ [" + cardKey + "] detectEvent异常 索引越界: " + e.getMessage());
                    System.err.println("  newPoints.size=" + newPoints.size() + 
                        ", history.size=" + history.size());
                    e.printStackTrace();
                    continue; // 检测异常，跳过
                } catch (Exception e) {
                    // 详细日志：记录调用前的参数状态
                    System.out.println("🔍 [" + cardKey + "] 准备调用 detectEvent: " +
                            "newPoints.size=" + newPoints.size() +
                            ", history.size=" + history.size() +
                            ", candidate=" + candidate +
                            ", windowStart=" + windowStart +
                            ", windowEnd=" + windowEnd);
                    System.err.println("异常日志 ⚠️ [" + cardKey + "] detectEvent异常: " + e.getMessage() +
                        ", newPoints.size=" + newPoints.size());
                    e.printStackTrace();
                    continue; // 检测异常，跳过
                }
                
                if (es != null && es.getEvent() != null && es.getEvent() == BoardingDetector.Event.SEND_BOARDING) {
                    // 找到上车事件
                    foundStartWindowStartIndex = windowStart;
                    foundStartEventState = es;
                    break;
                }
            }

            if (foundStartWindowStartIndex < 0) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯未找到发运上车点（回溯时段内未检测到 SEND_BOARDING），以上一个流程结束的点位的后一个点为上车点");

                // 计算“候选上车点时间”
                long fallbackTs = st.sendLastEventTime + 1000; // 回溯时间结束点 +1秒

                // 在历史点中找到最接近 fallbackTs 的点（时间 >= fallbackTs）
                LocationPoint fallbackPoint = null;
                LoadingUnloadingStrategy strategy = getStrategyForVehicleType(vehicleType);
                for (LocationPoint p : history) {
                    if (p.getTimestamp() >= fallbackTs) {
                        // 判断是否在停车区域（发运上车区域）
                        if (strategy.isInParkingArea(p)){
                            fallbackPoint = p;
                        }
                        break;
                    }
                }

                if (fallbackPoint == null) {
                    // 如果仍然没找到，则使用最后一个点兜底
                    fallbackPoint = history.get(history.size() - 1);
                    System.out.println("警告日志 ⚠️ [" + cardKey + "] 未找到 fallbackTs 对应点，使用最后一个点兜底");
                }

                // 构造一个模拟的 SEND_BOARDING 事件
                EventState es = new EventState();
                es.setEvent(BoardingDetector.Event.SEND_BOARDING);
                es.setTimestamp(fallbackPoint.getTimestamp());

                foundStartEventState = es;
                foundStartWindowStartIndex = history.indexOf(fallbackPoint);

                System.out.println("✅ [" + cardKey + "] 使用 fallbackTs 对应点作为上车点：" + fallbackPoint.getTimestamp());
            }

            // 确定上车点的 timestamp（使用 foundStartEventState 的时间或窗口中间点）
            long startTs = (foundStartEventState != null && foundStartEventState.getTimestamp() > 0)
                    ? foundStartEventState.getTimestamp()
                    : history.get(foundStartWindowStartIndex + backfillHalf).getTimestamp();

            // 确定上车点在 history 中的索引（取第一个 timestamp >= startTs 的索引）
            int startIndex = -1;
            for (int i = 0; i < history.size(); i++) {
                if (history.get(i).getTimestamp() >= startTs) {
                    startIndex = i;
                    break;
                }
            }
            if (startIndex < 0) startIndex = 0;
            
            // 边界检查：确保 startIndex 和 endIndex 在有效范围内
            if (startIndex >= history.size()) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：startIndex 超出范围");
                return;
            }
            if (endIndex >= history.size()) {
                endIndex = history.size() - 1;
                System.out.println("异常日志 ⚠️ [" + cardKey + "] endIndex 超出范围，已调整为 " + endIndex);
            }
            if (startIndex > endIndex) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：startIndex > endIndex");
                return;
            }

            // 最终截取：从 startIndex 到 endIndex（包含）
            List<LocationPoint> tripPoints = new ArrayList<>(history.subList(startIndex, endIndex + 1));

            // 去重 & 按时间排序
            List<LocationPoint> sessionPoints = tripPoints.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                            m -> new ArrayList<>(m.values())));

            // 验证 sessionPoints
            if (sessionPoints == null || sessionPoints.isEmpty()) {
                System.out.println("异常日志 ⚠️ [" + cardKey + "] 回溯失败：去重后的轨迹点为空");
                return;
            }
            
            System.out.println("📍 [" + cardKey + "] 回溯轨迹段：起始索引=" + startIndex + 
                ", 结束索引=" + endIndex + ", 原始点数=" + tripPoints.size() + 
                ", 去重后点数=" + sessionPoints.size());

            // 构造虚拟会话并持久化（与 persistSession 兼容）
            TrackSession sess = new TrackSession();
            sess.sessionId = IdUtils.fastSimpleUUID();
            sess.cardId = cardKey;
            sess.startTime = startTs;
            sess.startLongitude = sessionPoints.get(0).getLongitude();
            sess.startLatitude = sessionPoints.get(0).getLatitude();
            sess.endTime = dropTs;
            sess.endLongitude = sessionPoints.get(sessionPoints.size() - 1).getLongitude();
            sess.endLatitude = sessionPoints.get(sessionPoints.size() - 1).getLatitude();
            sess.kind = EventKind.SEND;
            sess.points.addAll(sessionPoints);
            persistSession(sess, dropTs, sessionPoints);
            dataSender.outParkPush(sess, vehicleType);
            for (LocationPoint p : sessionPoints){
                dataSender.trackPush(null, p, sess, vehicleType);
            }
            dataSender.outYardPush(sess, vehicleType);
            st.sendLastEventTime = downEs.getTimestamp();
            st.sendLastEventTimeStr = DateTimeUtils.timestampToDateTimeStr(downEs.getTimestamp());
            System.out.println("📤 [" + cardKey + "] 回溯并持久化发运轨迹段 成功 起=" + new Date(startTs) + " 止=" + new Date(dropTs) + " 点数=" + sessionPoints.size());

            // 可选：输出 shp
            if (baseConfig.isOutputShp()) {
                String shp = ensureShpPath(shpFileRoot, sess.sessionId, EventKind.SEND);
                outputVectorFiles(sessionPoints, shp);
            }

        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.out.println("异常日志 ❌ [" + cardKey + "] 回溯持久化过程中发生异常: " + errorMsg);
            System.err.println("详细错误信息:");
            System.err.println("  - 异常类型: " + ex.getClass().getName());
            System.err.println("  - 错误消息: " + errorMsg);
            if (ex instanceof IndexOutOfBoundsException) {
                System.err.println("  - 这是索引越界异常，请检查列表访问");
            } else if (ex instanceof NullPointerException) {
                System.err.println("  - 这是空指针异常，请检查对象是否为 null");
            }
            System.err.println("堆栈跟踪:");
            ex.printStackTrace();
        }
    }

    /** 批次预处理：修正 timestamp、过滤异常、排序、去重 */
    private List<LocationPoint> preprocessBatch(List<LocationPoint> batch, long cutoffTs) {
        List<LocationPoint> normal = new ArrayList<>();
        for (LocationPoint raw : batch) {
            // 兜底：若 timestamp 未赋值，用 acceptTime 转换
            if (raw.getTimestamp() == 0 && raw.getAcceptTime() != null) {
                raw.setTimestamp(DateTimeUtils.convertToTimestamp(raw.getAcceptTime()));
            }
            int state = outlierFilter.isValid(raw);
            if (state == 0) {
                normal.add(raw);
            }
        }
        if (normal.isEmpty()) return normal;

        // 排序 & 去重（移除 <= cutoffTs）
        return normal.stream()
                .filter(p -> p.getTimestamp() > cutoffTs)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                        m -> new ArrayList<>(m.values())));
    }

    /** 成对会话入库（整段点） */
    private void persistSession(TrackSession sess, long endTime, List<LocationPoint> points) {
        // 安全检查
        if (sess == null) {
            System.err.println("异常日志 ⚠️ persistSession: sess 为 null");
            return;
        }
        if (points == null || points.isEmpty()) {
            System.err.println("异常日志 ⚠️ persistSession: points 为空, trackId=" + sess.sessionId);
            return;
        }
        
        try {
            // 1) 详情表
            List<TakBehaviorRecordDetail> detailList = new ArrayList<>(points.size());
            for (LocationPoint p : points) {
                if (p == null) {
                    System.err.println("异常日志 ⚠️ persistSession: 遇到 null 点，跳过");
                    continue;
                }
                TakBehaviorRecordDetail d = new TakBehaviorRecordDetail();
                d.setCardId(resolveCardIdForDB(sess.cardId));
                d.setTrackId(sess.sessionId);
                d.setRecordTime(new Date(p.getTimestamp()));
                d.setTimestampMs(p.getTimestamp());
                d.setLongitude(p.getLongitude());
                d.setLatitude(p.getLatitude());
                d.setSpeed(p.getSpeed());
                detailList.add(d);
            }
            
            if (detailList.isEmpty()) {
                System.err.println("异常日志 ⚠️ persistSession: detailList 为空，无法入库");
                return;
            }

            // 2) 主表
            TakBehaviorRecords rec = new TakBehaviorRecords();
            rec.setCardId(resolveCardIdForDB(sess.cardId));
            rec.setYardId(baseConfig.getYardName()); // 保持与你现有逻辑一致，可抽配置
            rec.setTrackId(sess.sessionId);
            rec.setStartTime(new Date(sess.startTime));
            rec.setEndTime(new Date(endTime));
            rec.setPointCount((long) detailList.size());
            rec.setType(mapEventKindToType(sess.kind));
            rec.setDuration(DateTimeUtils.calculateTimeDifference(sess.startTime, endTime));
            rec.setState("完成");
            rec.setBeaconName(sess.beaconName);
            rec.setBeaconRfidName(sess.rfidName);
            rec.setBeaconArea(sess.area);
            rec.setTakBehaviorRecordDetailList(detailList);

            // 3) 入库（注意：实时服务不主动清表）
            iTakBehaviorRecordsService.insertTakBehaviorRecords(rec);
            iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetailAll(detailList);
            
            System.out.println("✅ persistSession 成功: trackId=" + sess.sessionId + 
                ", 点数=" + detailList.size());
                
        } catch (Exception e) {
            System.err.println("异常日志 ❌ persistSession 异常: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让上层 catch 捕获
        }
    }

    private long mapEventKindToType(EventKind kind) {
        switch (kind) {
            case ARRIVED:   return 0L;
            case SEND:      return 1L;
            case TRUCK_ARRIVED:   return 2L;
            case TRUCK_SEND: return 3L;
            default:        return 9L; // 默认值
        }
    }

    /** shp 输出 */
    public static void outputVectorFiles(List<LocationPoint> points, String shpFilePath) {
        List<Coordinate> coordinates = new ArrayList<>(points.size());
        for (LocationPoint p : points) {
            coordinates.add(new Coordinate(p.getLongitude(), p.getLatitude(), p.getTimestamp()));
        }
        ensureFilePathExists(shpFilePath);
        ShapefileWriter.writeCoordinatesToShapefile(coordinates, shpFilePath);
    }

    private static String ensureShpPath(String root, String trackId, EventKind k) {
        String name = (k == EventKind.ARRIVED ? "arrived_" : "send_") + trackId + ".shp";
        if (root == null) root = "";
        if (!root.endsWith("/") && !root.endsWith("\\")) root = root + "/";
        return root + name;
    }

    /** 将 LocationPoint 的“卡标识”标准化成字符串 key，用于 stateByCard 映射 */
    private String resolveCardKey(LocationPoint p) {
        // 你的数据源有两种：minhang（int cardId）/ yuzui（UUID 字符串）
        // 这里统一转成字符串
        if (p.getCardUUID() != null && !p.getCardUUID().isEmpty()) return p.getCardUUID();
        if (p.getCardId() != null) return String.valueOf(p.getCardId());
        // 若都没有，则用 recordThirdId 之类做兜底
        return Optional.ofNullable(p.getCardUUID()).orElse("UNKNOWN");
    }

    /** 入库字段 cardId，跟 resolveCardKey 的对齐（按你的表结构来） */
    private String resolveCardIdForDB(String cardKey) {
        // 直接使用字符串卡号（yuzui）；若是纯数字字符串也可直接存
        return cardKey;
    }

    // —— 兼容：历史批处理入口若也想调用实时逻辑，可提供包装方法 —— //
    public void replayHistorical(List<LocationPoint> points, VehicleType vt) {
        // 可选：先清理旧数据（按你的批处理逻辑）
        iTakBehaviorRecordsService.deleteByCreationTime(baseConfig.getDeleteDatetime());
        iTakBehaviorRecordDetailService.deleteByCreationTime(baseConfig.getDeleteDatetime());

        upsertVehicleTypeForAll(points, vt);

        // 模拟“顺序实时”喂入
        Map<String, List<LocationPoint>> byCard = points.stream()
                .collect(Collectors.groupingBy(this::resolveCardKey));
        byCard.forEach((card, list) -> {
            List<LocationPoint> sorted = list.stream()
                    .peek(p -> { if (p.getTimestamp() == 0 && p.getAcceptTime() != null)
                        p.setTimestamp(DateTimeUtils.convertToTimestamp(p.getAcceptTime())); })
                    .sorted(Comparator.comparingLong(LocationPoint::getTimestamp))
                    .collect(Collectors.toList());
            ingest(sorted);
        });
    }

    private void upsertVehicleTypeForAll(List<LocationPoint> points, VehicleType vt) {
        if (points == null) return;
        Set<String> keys = points.stream().map(this::resolveCardKey).collect(Collectors.toSet());
        for (String k : keys) upsertVehicleType(k, vt);
    }

    // —— 如需 JSON 文件模拟实时 —— //
    public void ingestFromJson(String filePath, VehicleType vt) {
        JSONObject json = com.jwzt.modules.experiment.utils.JsonUtils.loadJson(filePath);
        JSONArray arr = json.getJSONArray("data");
        List<LocationPoint> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            LocationPoint p = arr.getObject(i, LocationPoint.class);
            if (p.getTimestamp() == 0 && p.getAcceptTime() != null) {
                p.setTimestamp(DateTimeUtils.convertToTimestamp(p.getAcceptTime()));
            }
            list.add(p);
        }
        replayHistorical(list, vt);
    }
    
    /**
     * 清理所有卡的状态数据，释放内存
     * 
     * ⚠️ 注意：
     * - 此方法会清空所有累积的状态数据
     * - 仅在批处理任务结束后调用，不要在实时任务运行期间调用
     * - 实时任务需要保持状态累积，直到任务停止
     */
    public void clearAllState() {
        if (!stateByCard.isEmpty()) {
            // 清理前记录状态，便于排查问题
            int totalCards = stateByCard.size();
            long totalHistoryPoints = stateByCard.values().stream()
                    .mapToLong(state -> state.historyPoints.size())
                    .sum();
            
            System.out.println("🧹 清理内存状态：" + totalCards + " 个卡，共 " + totalHistoryPoints + " 个历史点");
            
            // 清理每个卡的状态
            stateByCard.values().forEach(state -> {
                if (state.window != null) {
                    state.window.clear();
                }
                if (state.historyPoints != null) {
                    state.historyPoints.clear();
                }
                state.activeSession = null;
            });
            
            // 清空整个Map
            stateByCard.clear();
            vehicleTypeByCard.clear();
            
            System.out.println("✅ 内存状态清理完成");
        }
    }
    
    /**
     * 清理指定卡的状态数据
     * 用于单个卡处理完成后的精细化清理
     * 
     * @param cardId 卡号
     */
    public void clearCardState(String cardId) {
        PerCardState state = stateByCard.remove(cardId);
        if (state != null) {
            if (state.window != null) {
                state.window.clear();
            }
            if (state.historyPoints != null) {
                state.historyPoints.clear();
            }
            state.activeSession = null;
        }
        vehicleTypeByCard.remove(cardId);
        System.out.println("🧹 已清理卡 " + cardId + " 的状态");
    }
    
    /**
     * 根据车辆类型获取对应的装卸策略
     * 使用缓存机制：同一个 tracker 实例对同一种车辆类型返回同一个策略实例，保持状态连续性
     * 不同的 tracker 实例之间不共享策略实例，避免多线程串扰
     * 
     * @param vehicleType 车辆类型
     * @return 对应的装卸策略
     */
    private LoadingUnloadingStrategy getStrategyForVehicleType(VehicleType vehicleType) {
        // 使用 computeIfAbsent 确保同一个 tracker 实例对同一种车辆类型只创建一次策略实例
        return strategyCache.computeIfAbsent(vehicleType, vt -> {
            LoadingStrategyFactory.VehicleType strategyType;
            if (vt == VehicleType.TRUCK) {
                strategyType = LoadingStrategyFactory.VehicleType.FLATBED;
            } else {
                // 默认为火车（CAR）
                strategyType = LoadingStrategyFactory.VehicleType.TRAIN;
            }
            return loadingStrategyFactory.getStrategy(strategyType);
        });
    }
    
    /**
     * 内部类：用于存储信标统计结果
     */
    @Data
    private static class BeaconStatistics {
        String zoneName;
        String zoneNameRfid;
        String zone;
    }
    
    /**
     * 内部类：用于统计信标出现次数
     */
    private static class BeaconCountInfo {
        TakBeaconInfo beacon;
        int count;
    }
    
    /**
     * 获取会话期间出现次数最多的信标信息
     * @param sessionPoints 会话期间的所有轨迹点
     * @param vehicleType 车辆类型
     * @return 信标统计结果
     */
    private BeaconStatistics getMostFrequentBeaconInSession(List<LocationPoint> sessionPoints, VehicleType vehicleType) {
        BeaconStatistics stats = new BeaconStatistics();
        
        if (sessionPoints == null || sessionPoints.isEmpty()) {
            System.out.println("⚠️ 会话点为空，无法统计信标信息");
            return stats;
        }
        
        // 根据车辆类型确定信标类型和查询参数
        String beaconType;
        String location;
        String area;
        
        if (vehicleType == VehicleType.TRUCK) {
            // 板车作业区
            beaconType = "板车作业区";
            location = null;
            area = null;
        } else {
            // 火车作业区（货运线作业台）
            beaconType = "货运线作业台";
            location = "2号线";
            area = null; // 不限制区域，A和B都统计
        }
        
        // 获取所有距离判定成功的信标列表（可包含重复）
        List<TakBeaconInfo> beaconsInRange = tagBeacon.getBeaconsInRangeForPoints(
            sessionPoints,
            baseConfig.getJoysuch().getBuildingId(),
            beaconType,
            location,
            area
        );
        
        if (beaconsInRange == null || beaconsInRange.isEmpty()) {
            System.out.println("⚠️ 会话期间未找到距离判定成功的信标信息（车辆类型：" + vehicleType + "）");
            return stats;
        }
        
        // 统计每个信标出现的次数（使用beaconId作为唯一标识）
        Map<String, BeaconCountInfo> beaconCountMap = new HashMap<>();
        
        for (TakBeaconInfo beacon : beaconsInRange) {
            String beaconId = beacon.getBeaconId();
            if (beaconId != null) {
                BeaconCountInfo countInfo = beaconCountMap.get(beaconId);
                if (countInfo == null) {
                    countInfo = new BeaconCountInfo();
                    countInfo.beacon = beacon;
                    countInfo.count = 0;
                    beaconCountMap.put(beaconId, countInfo);
                }
                countInfo.count++;
            }
        }
        
        // 找出出现次数最多的信标
        BeaconCountInfo maxCountInfo = null;
        for (BeaconCountInfo countInfo : beaconCountMap.values()) {
            if (maxCountInfo == null || countInfo.count > maxCountInfo.count) {
                maxCountInfo = countInfo;
            }
        }
        
        if (maxCountInfo != null && maxCountInfo.beacon != null) {
            TakBeaconInfo mostFrequentBeacon = maxCountInfo.beacon;
            stats.zoneName = mostFrequentBeacon.getName();
            stats.zoneNameRfid = mostFrequentBeacon.getRfidName();
            stats.zone = mostFrequentBeacon.getArea();
            
            System.out.println("✅ 会话期间找到出现次数最多的信标：" + 
                "name=" + stats.zoneName + 
                ", rfidName=" + stats.zoneNameRfid + 
                ", area=" + stats.zone + 
                ", 出现次数=" + maxCountInfo.count +
                ", 总点数=" + sessionPoints.size() +
                ", 车辆类型=" + vehicleType);
        } else {
            System.out.println("⚠️ 会话期间未能确定出现次数最多的信标");
        }
        
        return stats;
    }
}
