package com.jwzt.modules.experiment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.*;
import com.jwzt.modules.experiment.filter.LocationSmoother;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.geo.ShapefileWriter;
import com.jwzt.modules.experiment.vo.EventState;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.jwzt.modules.experiment.utils.FileUtils.ensureFilePathExists;

/**
 * 实时轨迹增量分析（10s/批等），与批处理 DriverTracker 并行。
 * 目标：
 *  1）成对事件间的“全部点”入库，不仅是识别窗口；
 *  2）支持多卡并发、乱序与重复；
 *  3）可按车辆类型切换阈值（预留）。
 */
@Service
public class RealTimeDriverTracker {
    @Autowired
    private BaseConfig baseConfig;
//    @Autowired
//    private FilePathConfig filePathConfig;
//    @Autowired
//    private FilterConfig filterConfig;
    // —— 依赖与基础工具 ——
    @Autowired
    private OutlierFilter outlierFilter;

    @Autowired
    private BoardingDetector detector;
//    private final OutlierFilter outlierFilter = new OutlierFilter();
    private final LocationSmoother smoother = new LocationSmoother();
//    private final BoardingDetector detector = new BoardingDetector();

    @Resource
    private ITakBehaviorRecordsService iTakBehaviorRecordsService;
    @Resource
    private ITakBehaviorRecordDetailService iTakBehaviorRecordDetailService;

    // —— 运行参数 ——
    private final int recordPointsSize = FilterConfig.RECORD_POINTS_SIZE;  // 事件识别窗口大小
    private final int backfillHalf = Math.max(1, recordPointsSize / 2);    // 启动会话时向后回填窗口的一半

    // —— 运行态：按卡维护 ——
    private final Map<String, PerCardState> stateByCard = new ConcurrentHashMap<>();
    private final Map<String, VehicleType> vehicleTypeByCard = new ConcurrentHashMap<>();

    // shp 输出根目录（沿用你的静态字段/配置方式）
    public static String shpFileRoot = DriverTracker.shpFilePath; // 与现有保持一致

    public enum VehicleType { TRUCK, CAR }

    /** 每张卡的运行态 */
    private static class PerCardState {
        Deque<LocationPoint> window = new ArrayDeque<>();
        TrackSession activeSession; // null 表示当前不在一段会话中
        long lastSeenTs = Long.MIN_VALUE; // 去重/乱序截断
    }

    /** 上/下车会话（成对闭环） */
    private static class TrackSession {
        String sessionId;
        String cardId;
        long startTime;
        EventKind kind; // ARRIVED 或 SEND
        final List<LocationPoint> points = new ArrayList<>();
    }

    /** 事件类型归并：到达业务 与 发运业务 */
    private enum EventKind { ARRIVED, SEND }

    @PostConstruct
    public void init() {
        // 这里可做 detector 阈值的全局初始化
    }

    /** 注册/更新某张卡的车辆类型（可选调用，不调用则默认 CAR） */
    public void upsertVehicleType(String cardId, VehicleType type) {
        vehicleTypeByCard.put(cardId, type == null ? VehicleType.CAR : type);
        // 若需按类型切换阈值，可在 detector 内部做 thread-safe 配置切换
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
        VehicleType vt = vehicleTypeByCard.getOrDefault(cardKey, VehicleType.CAR);

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
//            List<LocationPoint> fixesPoints = new OutlierFilter().fixTheData(win);
            // 3) 调用原有检测器（窗口大小固定）
            List<LocationPoint> newPoints = outlierFilter.stateAnalysis(win);
            EventState es = detector.updateState(newPoints); // 建议给 detector 增加一个重载支持车辆类型；若暂不支持，仍可用原方法

            if (es == null || es.getEvent() == null) {
                if (st.activeSession != null) st.activeSession.points.add(p);
                continue;
            }

            switch (es.getEvent()) {
                case ARRIVED_BOARDING:
                    onStart(cardKey, st, EventKind.ARRIVED, es, win);
                    break;

                case ARRIVED_DROPPING:
                    onEnd(cardKey, st, EventKind.ARRIVED, es, win);
                    break;

                case SEND_BOARDING:
                    onStart(cardKey, st, EventKind.SEND, es, win);
                    break;

                case SEND_DROPPING:
                    onEnd(cardKey, st, EventKind.SEND, es, win);
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
        st.activeSession.kind = kind;

        // 回填窗口后一半（包含触发点附近的历史）
        int from = Math.max(0, win.size() - backfillHalf);
        st.activeSession.points.addAll(win.subList(from, win.size()));

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
        int from = Math.max(0, win.size() - backfillHalf);
        st.activeSession.points.addAll(win.subList(from, win.size()));

        // 去重 & 按时间排序（会话内可能因回填/累积产生重复）
        List<LocationPoint> sessionPoints = st.activeSession.points.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                        m -> new ArrayList<>(m.values())));

        persistSession(st.activeSession, es.getTimestamp(), sessionPoints);

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
        // 1) 详情表
        List<TakBehaviorRecordDetail> detailList = new ArrayList<>(points.size());
        for (LocationPoint p : points) {
            TakBehaviorRecordDetail d = new TakBehaviorRecordDetail();
            d.setCardId(resolveCardIdForDB(sess.cardId));
            d.setTrackId(sess.sessionId);
            d.setRecordTime(new Date(p.getTimestamp()));
            d.setTimestampMs(p.getTimestamp());
            d.setLongitude(p.getLongitude());
            d.setLatitude(p.getLatitude());
            detailList.add(d);
        }

        // 2) 主表
        TakBehaviorRecords rec = new TakBehaviorRecords();
        rec.setCardId(resolveCardIdForDB(sess.cardId));
        rec.setYardId("YUZUI"); // 保持与你现有逻辑一致，可抽配置
        rec.setTrackId(sess.sessionId);
        rec.setStartTime(new Date(sess.startTime));
        rec.setEndTime(new Date(endTime));
        rec.setPointCount((long) points.size());
        rec.setType(sess.kind == EventKind.ARRIVED ? 0L : 1L); // 复用你原有 type 语义
        rec.setDuration(DateTimeUtils.calculateTimeDifference(sess.startTime, endTime));
        rec.setState("完成");
        rec.setTakBehaviorRecordDetailList(detailList);

        // 3) 入库（注意：实时服务不主动清表）
        iTakBehaviorRecordsService.insertTakBehaviorRecords(rec);
        iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetailAll(detailList);
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
}
