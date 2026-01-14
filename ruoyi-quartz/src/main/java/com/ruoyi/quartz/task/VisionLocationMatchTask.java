package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;
import com.jwzt.modules.experiment.utils.third.manage.DataSender;
import com.jwzt.modules.experiment.vo.EventState;
import com.jwzt.modules.experiment.domain.vo.VisionLocationMatchResult;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.strategy.LoadingStrategyFactory;
import com.jwzt.modules.experiment.strategy.LoadingUnloadingStrategy;
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.VisionLocationMatcher;
import com.jwzt.modules.experiment.utils.third.manage.JobData;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 视觉识别与定位数据匹配定时任务
 * 每分钟执行一次，将视觉识别数据与定位卡数据进行匹配
 * 
 * @author lx
 * @date 2025-01-20
 */
@Component("VisionLocationMatchTask")
public class VisionLocationMatchTask {
    
    private static final Logger log = LoggerFactory.getLogger(VisionLocationMatchTask.class);
    
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;
    
    @Autowired
    private VisionLocationMatcher visionLocationMatcher;
    
    @Autowired
    private JobData jobData;
    
    @Autowired
    private OutlierFilter outlierFilter;
    
    @Autowired
    private LoadingStrategyFactory loadingStrategyFactory;

    @Autowired
    private DataSender dataSender;
    
    @Resource
    private ITakBehaviorRecordsService iTakBehaviorRecordsService;
    
    @Resource
    private ITakBehaviorRecordDetailService iTakBehaviorRecordDetailService;

    /**
     * 获取定位数据（仅异常触发重试，最多3次），指数退避：200ms/400ms/800ms
     * 不对“返回空数据”做重试，避免改变现有语义。
     */
    private List<LocationPoint> fetchLocationWithRetry(DataAcquisition dataAcquisition,
                                                       String cardId,
                                                       String buildId,
                                                       String startTimeStrTag,
                                                       String endTimeStrTag) {
        final int maxAttempts = 3;
        final long baseDelayMs = 200L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return dataAcquisition.getLocationAndUWBData(cardId, buildId, startTimeStrTag, endTimeStrTag);
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    long delayMs = baseDelayMs << (attempt - 1); // 200, 400, 800
                    log.warn("获取定位数据失败，准备重试：cardId={}, attempt={}/{}, delayMs={}, err={}",
                            cardId, attempt, maxAttempts, delayMs, e.toString());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("重试等待被中断，停止重试：cardId={}, attempt={}/{}", cardId, attempt, maxAttempts);
                        return null;
                    }
                } else {
                    log.error("获取定位数据失败（重试耗尽），卡ID: {}", cardId, e);
                }
            }
        }
        return null;
    }
    
    /**
     * 执行匹配任务
     * 每分钟执行一次
     */
    public void executeMatch(String startStr, String endStr) {
        try {
            log.info("========== 视觉识别与定位数据匹配任务开始 ==========");
            
            // 1. 获取当前时间范围（最近1分钟）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            Date startTime = new Date(now.getTime() - 20 * 60 * 1000L); // 当前时间前1分钟
            Date endTime = new Date(now.getTime() - 10 * 60 * 1000L); // 当前时间前1分钟
            String startTimeStr = sdf.format(startTime);
            String endTimeStr = sdf.format(endTime);
            if (startStr != null && !startStr.isEmpty() && endStr != null && !endStr.isEmpty()) {
                startTimeStr = startStr;
                endTimeStr = endStr;
                log.info("指定查询时间范围：{} - {}", startTimeStr, endTimeStr);
            } else {
                log.info("查询时间范围：{} - {}", startTimeStr, endTimeStr);
            }
            final String startTimeStrFinal = startTimeStr;
            final String endTimeStrFinal = endTimeStr;

            // 计算定位数据的时间范围（视觉数据时间范围前后各延展5分钟）
            Date startTimeTag = sdf.parse(startTimeStr);
            Date endTimeTag = sdf.parse(endTimeStr);
            String startTimeStrTag = sdf.format(new Date(startTimeTag.getTime() - 5 * 60 * 1000L)); // 开始时间前推5分钟
            String endTimeStrTag = sdf.format(new Date(endTimeTag.getTime() + 5 * 60 * 1000L));   // 结束时间后延5分钟
            // 2. 获取所有卡ID列表（type=1）
            DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
            List<String> cardIdList = dataAcquisition.getCardIdList(1);
            if (cardIdList == null || cardIdList.isEmpty()) {
                log.warn("没有需要处理的卡，任务跳过");
                return;
            }
            log.info("获取到卡ID列表，共 {} 个卡", cardIdList.size());
            
            // 3. 获取buildId
            String buildId = baseConfig.getJoysuch().getBuildingId();
            if (buildId == null || buildId.isEmpty()) {
                log.error("buildId为空，无法获取定位数据");
                return;
            }
            
            // 4. 获取cameraIds
            List<String> cameraIds = baseConfig.getCardAnalysis().getVisualIdentify().getCameraIds();
            if (cameraIds == null || cameraIds.isEmpty()) {
                log.warn("cameraIds为空，无法获取视觉识别数据");
                return;
            }
            log.info("获取到摄像机ID列表，共 {} 个摄像机", cameraIds.size());
            
            // 5. 获取新的定位数据（所有卡）
            // 5. 并行获取新的定位数据

            if (baseConfig.getLocateDataSources().equals("zq")){
                // 预热：提前获取一次token，避免并发冲突
                try {
                    System.out.println("正在预热，获取AccessToken...");
                    zqOpenApi.getHeaders();
                    System.out.println("✓ AccessToken预热成功");
                    // 等待200ms确保token已缓存
                    Thread.sleep(200);
                } catch (Exception e) {
                    System.err.println("⚠️ AccessToken预热失败，但会继续尝试: " + e.getMessage());
                }
            }

            int cpuCores = Runtime.getRuntime().availableProcessors();
            int threadPoolSize = Math.max(2, Math.min(Math.min(cardIdList.size(), cpuCores * 2), 20));
            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
            Map<String, List<LocationPoint>> cardLocationMap = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            log.info("并行获取定位数据：卡数={}, 线程数={}", cardIdList.size(), threadPoolSize);
            for (String cardId : cardIdList) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        List<LocationPoint> locationPoints = fetchLocationWithRetry(
                                dataAcquisition, cardId, buildId, startTimeStrTag, endTimeStrTag);
//                        List<LocationPoint> locationPoints = new ArrayList<>();
//                        for (LocationPoint currentPoint : locationPointList){
//                            // 判断是否靠近交通车附近
//                            boolean isTrafficCarWithin = tagBeacon.theTagIsCloseToTheBeacon(
//                                    currentPoint,
//                                    baseConfig.getJoysuch().getBuildingId(),
//                                    "交通车",
//                                    null,
//                                    null,
//                                    null);
//                            if (!isTrafficCarWithin) {
//                                locationPoints.add(currentPoint);
//                            }
//                        }
                        if (locationPoints != null && !locationPoints.isEmpty()) {
                            cardLocationMap.put(cardId, locationPoints);
                            log.debug("卡ID: {}, 获取到定位数据 {} 条", cardId, locationPoints.size());
                        }
                    } catch (Exception e) {
                        log.error("获取定位数据失败，卡ID: {}", cardId, e);
                    }
                }, executorService);
                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();

            int totalLocationCount = cardLocationMap.values().stream().mapToInt(List::size).sum();
            log.info("获取到新的定位数据，共 {} 条", totalLocationCount);
            
            // 6. 获取新的视觉识别数据
            List<VisionEvent> newVisionEventList = jobData.getVisionList(startTimeStrFinal, endTimeStrFinal, cameraIds);

            // 过滤出装卸车数据,只取eventType=load的数据
            List<VisionEvent> newVisionEvents = newVisionEventList.stream()
                    .filter(event -> "load".equals(event.getEventType())
                            || "unload".equals(event.getEventType()))
                    .collect(Collectors.toList());
            // 将新视觉事件的 matched 字段全部置为 1
            if (newVisionEvents != null && !newVisionEvents.isEmpty()) {
                newVisionEvents.forEach(event -> event.setMatched(1));
            }
            log.info("获取到新的视觉识别数据，共 {} 条", newVisionEvents != null ? newVisionEvents.size() : 0);
            
            // 7. 追加到历史数据
            if (!cardLocationMap.isEmpty()) {
                // 按卡ID追加
                cardLocationMap.forEach(visionLocationMatcher::appendLocationData);
            }
            
            if (newVisionEvents != null && !newVisionEvents.isEmpty()) {
                visionLocationMatcher.appendVisionData(newVisionEvents);
            }
            
            // 8. 执行匹配算法
            List<VisionLocationMatchResult> matchResults = visionLocationMatcher.matchVisionWithLocation();
            
            // 9. 输出匹配结果
            log.info("========== 匹配完成 ==========");
            log.info("匹配结果组数: {}", matchResults.size());
            for (int i = 0; i < matchResults.size(); i++) {
                VisionLocationMatchResult result = matchResults.get(i);
                log.info("第 {} 组匹配结果：视觉事件数: {}, 匹配定位点数: {}", 
                        i + 1, 
                        result.getVisionEventGroup().size(),
                        result.getMatchedLocationPoints().size());
            }
            
            // 10. 输出统计信息
            java.util.Map<String, Object> stats = visionLocationMatcher.getStatistics();
            log.info("历史数据统计：视觉事件: {}, 定位卡数: {}, 定位点总数: {}, 视觉组数: {}", 
                    stats.get("visionEventCount"),
                    stats.get("locationCardCount"),
                    stats.get("totalLocationPoints"),
                    stats.get("visionGroupCount"));

            // 11. 对结果中的数据进行处理
            // 按 cardId 分组，每组内按 timestamp 升序排序
            Map<String, List<VisionLocationMatchResult.MatchedLocationPoint>> groupedResults = matchResults.stream()
                    .flatMap(result -> result.getMatchedLocationPoints().stream())
                    .filter(mp -> mp.getCardId() != null && mp.getLocationPoint() != null && mp.getLocationPoint().getTimestamp() != null)
                    .filter(mp -> mp.getVisionEvent().getMatched() != 0)
                    .collect(Collectors.groupingBy(
                            VisionLocationMatchResult.MatchedLocationPoint::getCardId,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        list.sort(Comparator.comparing(mp -> mp.getLocationPoint().getTimestamp()));
                                        return list;
                                    }
                            )
                    ));

            log.info("按 cardId 分组完成，共 {} 个不同的卡", groupedResults.size());
            groupedResults.forEach((cardId, points) ->
                    log.info("卡ID: {}, 匹配点数: {}", cardId, points.size())
            );

            // 12、对识别数据按卡进行处理，采用异步方式处理，以卡id异步处理
            if (!groupedResults.isEmpty()) {
                processMatchedPointsByCard(groupedResults);
            }

            // 13、初始化历史数据
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour == 3) { // 凌晨3点
                log.info("开始执行清理历史数据任务");
                visionLocationMatcher.initHistoryData();
            }

        } catch (Exception e) {
            log.error("视觉识别与定位数据匹配任务执行异常", e);
        }
    }
    
    /**
     * 对每个卡的匹配数据进行处理
     * 采用异步方式处理，以卡id异步处理
     */
    private void processMatchedPointsByCard(Map<String, List<VisionLocationMatchResult.MatchedLocationPoint>> groupedResults) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int threadPoolSize = Math.max(2, Math.min(Math.min(groupedResults.size(), cpuCores * 2), 20));
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
//        log.info("开始异步处理匹配数据：卡数={}, 线程数={}", groupedResults.size(), threadPoolSize);
//
//        for (Map.Entry<String, List<VisionLocationMatchResult.MatchedLocationPoint>> entry : groupedResults.entrySet()) {
//            String cardId = entry.getKey();
//            List<VisionLocationMatchResult.MatchedLocationPoint> matchedPoints = entry.getValue();
//
//            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//                try {
//                    processCardMatchedPoints(cardId, matchedPoints);
//                } catch (Exception e) {
//                    log.error("处理卡 {} 的匹配数据失败", cardId, e);
//                }
//            }, executorService);
//            futures.add(future);
//        }
//
//        // 等待所有任务完成
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//        executorService.shutdown();
        for (Map.Entry<String, List<VisionLocationMatchResult.MatchedLocationPoint>> entry : groupedResults.entrySet()) {
            String cardId = entry.getKey();
            List<VisionLocationMatchResult.MatchedLocationPoint> matchedPoints = entry.getValue();
            try {
                processCardMatchedPoints(cardId, matchedPoints);
            } catch (Exception e) {
                log.error("处理卡 {} 的匹配数据失败", cardId, e);
            }
        }
        log.info("所有卡的匹配数据处理完成");
    }
    
    /**
     * 处理单个卡的匹配数据
     * 对每个 MatchedLocationPoint，查找其对应的上车点并入库
     */
    private void processCardMatchedPoints(String cardId, List<VisionLocationMatchResult.MatchedLocationPoint> matchedPoints) {
        if (matchedPoints == null || matchedPoints.isEmpty()) {
            return;
        }
        
        // 获取该卡的历史定位数据
        List<LocationPoint> points = visionLocationMatcher.getHistoryLocationDataForCard(cardId);
        List<LocationPoint> historyPoints = preprocessBatch(points);
        if (historyPoints == null || historyPoints.isEmpty()) {
            log.warn("卡ID: {} 的历史定位数据为空，跳过处理", cardId);
            return;
        }
        
        // 默认使用火车策略（可根据需要扩展业务类型判断逻辑）
        LoadingStrategyFactory.VehicleType vehicleType = LoadingStrategyFactory.VehicleType.TRAIN;
        LoadingUnloadingStrategy strategy = loadingStrategyFactory.getStrategy(vehicleType);
        
        // 遍历每个匹配点，查找对应的上车点
        for (int i = 0; i < matchedPoints.size(); i++) {
            VisionLocationMatchResult.MatchedLocationPoint matchedPoint = matchedPoints.get(i);
            LocationPoint dropOffPoint = matchedPoint.getLocationPoint();
            VisionEvent visionEvent = matchedPoint.getVisionEvent();
            if (visionEvent.getId() == 13740L){
                log.info("开始处理卡ID: {} 的第 {} 个上车数据", cardId, i + 1);
            }
            
            if (dropOffPoint == null || dropOffPoint.getTimestamp() == null) {
                log.warn("卡ID: {} 的第 {} 个匹配点数据无效，跳过", cardId, i + 1);
                continue;
            }

            if (visionEvent == null || visionEvent.getEventTime() == null) {
                log.warn("卡ID: {} 的第 {} 个匹配点数据无效，跳过", cardId, i + 1);
                continue;
            }
            try {
                long endTimestamp = dropOffPoint.getTimestamp();
                long startTimestamp = 0l;
                // 装卸类型 0 装车 1 卸车
                int eventType = 0;
                if (visionEvent.getEventType().equals("load")){
                    log.info("开始处理卡ID: {} 的第 {} 个卸车数据", cardId, i + 1);
                    // 计算数据区间
                    endTimestamp = dropOffPoint.getTimestamp();

                    if (i == 0) {
                        // 第一个数据：向前取5分钟的点作为起点
                        startTimestamp = Math.max(0, endTimestamp - 5 * 60 * 1000L);
                    } else {
                        // 找到上一个匹配点的下一个点作为起点
                        LocationPoint prevDropOffPoint = matchedPoints.get(i - 1).getLocationPoint();
                        long prevTimestamp = prevDropOffPoint.getTimestamp();
                        // 找到历史数据中时间戳 > prevTimestamp 的第一个点
                        startTimestamp = findNextPointTimestamp(historyPoints, prevTimestamp);
                        int index = FilterConfig.RECORD_POINTS_SIZE / 2 - 1;
                        if (startTimestamp > 0){
                            startTimestamp = startTimestamp - index * 1000;
                        }
                        if (startTimestamp < 0) {
                            // 如果找不到，使用上一个时间戳 + 1秒
                            startTimestamp = prevTimestamp + 1000 - index * 1000;
                        }
                    }
                } else if (visionEvent.getEventType().equals("unload")) {
                    log.info("开始处理卡ID: {} 的第 {} 个卸车数据", cardId, i + 1);
                    eventType = 1;
                    // startTimestamp 使用当前卸车点的时间戳
                    startTimestamp = dropOffPoint.getTimestamp();
                    
                    // 判断是否为最后一个点
                    if (i == matchedPoints.size() - 1) {
                        // 最后一个点：向后取5分钟
                        endTimestamp = dropOffPoint.getTimestamp() + 5 * 60 * 1000L;
                    } else {
                        // 不是最后一个点：找到下一个匹配点的前一个点
                        LocationPoint nextDropOffPoint = matchedPoints.get(i + 1).getLocationPoint();
                        long nextTimestamp = nextDropOffPoint.getTimestamp();
                        // 找到历史数据中时间戳 < nextTimestamp 的最后一个点
                        long prevTimestamp = findPreviousPointTimestamp(historyPoints, nextTimestamp);
                        int index = FilterConfig.RECORD_POINTS_SIZE / 2 - 1;
                        if (prevTimestamp > 0) {
                            endTimestamp = prevTimestamp - index * 1000;
                        } else {
                            // 如果找不到，使用下一个时间戳 - 1秒作为兜底
                            endTimestamp = nextTimestamp - 1000 + index * 1000;
                        }
                    }
                }
                String start = DateTimeUtils.timestampToDateTimeStr(startTimestamp);
                String end = DateTimeUtils.timestampToDateTimeStr(endTimestamp);
                // 提取区间内的定位点
                List<LocationPoint> intervalPoints = extractIntervalPoints(historyPoints, startTimestamp, endTimestamp);
                if (intervalPoints.isEmpty()) {
                    log.warn("卡ID: {} 的第 {} 个匹配点，区间内无定位点，跳过", cardId, i + 1);
                    continue;
                }
                // 正向查找上车点
                LocationPoint boardingPoint = findBoardingPoint(intervalPoints, historyPoints, strategy, cardId, eventType);
                if (boardingPoint == null) {
                    log.warn("卡ID: {} 的第 {} 个匹配点，未找到上车点，跳过", cardId, i + 1);
                    continue;
                }
                List<LocationPoint> sessionPoints = new ArrayList<>();
                // 提取从上车点到下车点的轨迹点
                if (eventType == 0){
                    sessionPoints = extractSessionPoints(intervalPoints, boardingPoint, dropOffPoint);
                }else if (eventType == 1){
                    sessionPoints = extractSessionPoints(intervalPoints, dropOffPoint, boardingPoint);
                }
                if (sessionPoints.isEmpty()) {
                    log.warn("卡ID: {} 的第 {} 个匹配点，会话轨迹点为空，跳过", cardId, i + 1);
                    continue;
                }

                // 生成统一的 trackId
                String trackId = IdUtils.fastSimpleUUID();

                // 入库
                int inboundStatus = persistSession(cardId, boardingPoint, dropOffPoint, sessionPoints, vehicleType, eventType, visionEvent, trackId);
                if (inboundStatus == 0){
                    for (VisionEvent visionEventHistory : visionLocationMatcher.getHistoryVisionData())
                        if (visionEventHistory.getId().equals(visionEvent.getId())){
                            visionEventHistory.setMatched(0);
                        }
                }

                // 推送数据
                RealTimeDriverTracker.TrackSession sess = new RealTimeDriverTracker.TrackSession();
                sess.sessionId = trackId;
                sess.cardId = cardId;
                sess.startTime = sessionPoints.get(0).getTimestamp();
                sess.startLongitude = sessionPoints.get(0).getLongitude();
                sess.startLatitude = sessionPoints.get(0).getLatitude();
                sess.endTime = sessionPoints.get(sessionPoints.size() - 1).getTimestamp();
                sess.endLongitude = sessionPoints.get(sessionPoints.size() - 1).getLongitude();
                sess.endLatitude = sessionPoints.get(sessionPoints.size() - 1).getLatitude();
                sess.points.addAll(sessionPoints);
                sess.vin = visionEvent.getVin();
                if (visionEvent.getEventType().equals("load")){
                    sess.kind = RealTimeDriverTracker.EventKind.SEND;
                    // 发运
                    dataSender.outParkPush(sess, RealTimeDriverTracker.VehicleType.CAR);
                    for (LocationPoint p : sessionPoints){
                        dataSender.trackPush(null, p, sess, RealTimeDriverTracker.VehicleType.CAR);
                    }
                    dataSender.outYardPush(sess, RealTimeDriverTracker.VehicleType.CAR);
                } else if (visionEvent.getEventType().equals("unload")){
                    sess.kind = RealTimeDriverTracker.EventKind.ARRIVED;
                    // 到达
                    dataSender.inYardPush(sess, RealTimeDriverTracker.VehicleType.CAR);
                    for (LocationPoint p : sessionPoints){
                        dataSender.trackPush(null, p, sess, RealTimeDriverTracker.VehicleType.CAR);
                    }
                    dataSender.inParkPush(sess, RealTimeDriverTracker.VehicleType.CAR);
                }

                log.info("卡ID: {} 的第 {} 个匹配点处理完成，上车点时间: {}, 下车点时间: {}, 轨迹点数: {}", 
                        cardId, i + 1, 
                        DateTimeUtils.timestampToDateTimeStr(boardingPoint.getTimestamp()),
                        DateTimeUtils.timestampToDateTimeStr(dropOffPoint.getTimestamp()),
                        sessionPoints.size());
                
            } catch (Exception e) {
                log.error("处理卡ID: {} 的第 {} 个匹配点时发生异常", cardId, i + 1, e);
            }
        }
    }

    /**
     * 找到历史数据中时间戳大于指定时间戳的第一个点的时间戳
     */
    private long findNextPointTimestamp(List<LocationPoint> historyPoints, long timestamp) {
        for (LocationPoint point : historyPoints) {
            if (point.getTimestamp() != null && point.getTimestamp() > timestamp) {
                return point.getTimestamp();
            }
        }
        return -1;
    }
    
    /**
     * 找到历史数据中时间戳小于指定时间戳的最后一个点的时间戳
     */
    private long findPreviousPointTimestamp(List<LocationPoint> historyPoints, long timestamp) {
        long result = -1;
        for (LocationPoint point : historyPoints) {
            if (point.getTimestamp() != null && point.getTimestamp() < timestamp) {
                result = point.getTimestamp();
            } else {
                break; // 由于历史数据已按时间戳排序，找到第一个 >= timestamp 的点即可停止
            }
        }
        return result;
    }
    
    /**
     * 提取指定时间区间内的定位点
     */
    private List<LocationPoint> extractIntervalPoints(List<LocationPoint> historyPoints, long startTimestamp, long endTimestamp) {
        List<LocationPoint> result = new ArrayList<>();
        for (LocationPoint point : historyPoints) {
            if (point.getTimestamp() != null 
                    && point.getTimestamp() >= startTimestamp 
                    && point.getTimestamp() <= endTimestamp) {
                result.add(point);
            }
        }
        // 按时间戳排序
        result.sort(Comparator.comparingLong(LocationPoint::getTimestamp));
        return result;
    }
    
    /**
     * 正向查找上车点
     * 从区间起点开始，逐点构造窗口，调用策略检测事件
     */
    private LocationPoint findBoardingPoint(List<LocationPoint> intervalPoints,
                                            List<LocationPoint> historyPoints,
                                            LoadingUnloadingStrategy strategy,
                                            String cardId,
                                            int eventType) {
        if (intervalPoints == null || intervalPoints.isEmpty()) {
            return null;
        }
        
        int recordPointsSize = FilterConfig.RECORD_POINTS_SIZE;
        
        // 从起点开始，逐点构造窗口
        for (int i = 0; i < intervalPoints.size(); i++) {
            // 构造窗口：以当前点为中心，前后各取 recordPointsSize/2 个点
            int windowStart = Math.max(0, i - recordPointsSize / 2);
            int windowEnd = Math.min(intervalPoints.size(), windowStart + recordPointsSize);
            
            // 如果窗口大小不足，调整窗口起始位置
            if (windowEnd - windowStart < recordPointsSize) {
                windowStart = Math.max(0, windowEnd - recordPointsSize);
            }
            
            if (windowEnd - windowStart < recordPointsSize) {
                // 窗口大小仍然不足，跳过
                continue;
            }
            
            List<LocationPoint> window = new ArrayList<>(intervalPoints.subList(windowStart, windowEnd));
            
            // 使用 OutlierFilter 进行异常过滤
            List<LocationPoint> filteredPoints = outlierFilter.stateAnalysis(window);
            if (filteredPoints == null || filteredPoints.size() < recordPointsSize) {
                continue;
            }
            
            // 调用策略检测事件
            try {
                EventState eventState = strategy.detectEventAlready(filteredPoints, historyPoints, eventType);
                if (eventState != null && eventState.getEvent() != null) {
                    BoardingDetector.Event event = eventState.getEvent();
                    // 检查是否是上车事件
                    if (event == BoardingDetector.Event.SEND_BOARDING
                            || event == BoardingDetector.Event.ARRIVED_DROPPING
                            || event == BoardingDetector.Event.CAR_SEND_BOARDING
                            || event == BoardingDetector.Event.CAR_SEND_DROPPING
                            || event == BoardingDetector.Event.TRUCK_SEND_BOARDING
                            || event == BoardingDetector.Event.TRUCK_ARRIVED_DROPPING) {
                        // 找到上车点，返回窗口中间的点
                        int centerIndex = windowStart + recordPointsSize / 2;
                        if (centerIndex < intervalPoints.size()) {
                            return intervalPoints.get(centerIndex);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("卡ID: {} 在检测事件时发生异常，跳过该窗口", cardId, e);
                continue;
            }
        }
        
        // 如果未找到明确的上车事件，尝试使用策略的 isInParkingArea 方法查找第一个在停车区域的点
        for (LocationPoint point : intervalPoints) {
            if (strategy.isInParkingArea(point)) {
                return point;
            }
        }

        // 如果还是没有上下车点，则取intervalPoints的第一个点
        if (intervalPoints.size() > (recordPointsSize / 2)){
            return intervalPoints.get(recordPointsSize / 2);
        }
        return intervalPoints.get(0);
    }
    
    /**
     * 提取从上车点到下车点之间的轨迹点
     */
    private List<LocationPoint> extractSessionPoints(List<LocationPoint> intervalPoints, 
                                                     LocationPoint boardingPoint, 
                                                     LocationPoint dropOffPoint) {
        List<LocationPoint> result = new ArrayList<>();
        long boardingTimestamp = boardingPoint.getTimestamp();
        long dropOffTimestamp = dropOffPoint.getTimestamp();
        
        for (LocationPoint point : intervalPoints) {
            if (point.getTimestamp() != null 
                    && point.getTimestamp() >= boardingTimestamp 
                    && point.getTimestamp() <= dropOffTimestamp) {
                result.add(point);
            }
        }
        
        // 去重并按时间排序
        return result.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                        m -> new ArrayList<>(m.values())));
    }
    
    /**
     * 入库会话数据
     */
    private int persistSession(String cardId,
                                LocationPoint boardingPoint,
                                LocationPoint dropOffPoint,
                                List<LocationPoint> sessionPoints,
                                LoadingStrategyFactory.VehicleType vehicleType,
                                int eventType,
                                VisionEvent visionEvent,
                                String trackId) {
        if (sessionPoints == null || sessionPoints.isEmpty()) {
            log.warn("卡ID: {} 的会话轨迹点为空，无法入库", cardId);
            return 1;
        }
        
        try {
            // 1) 详情表
            List<TakBehaviorRecordDetail> detailList = new ArrayList<>(sessionPoints.size());
            for (LocationPoint p : sessionPoints) {
                if (p == null) {
                    continue;
                }
                TakBehaviorRecordDetail d = new TakBehaviorRecordDetail();
                d.setCardId(cardId);
                d.setTrackId(trackId);
                d.setRecordTime(new Date(p.getTimestamp()));
                d.setTimestampMs(p.getTimestamp());
                d.setLongitude(p.getLongitude());
                d.setLatitude(p.getLatitude());
                d.setSpeed(p.getSpeed());
                detailList.add(d);
            }
            
            if (detailList.isEmpty()) {
                log.warn("卡ID: {} 的详情列表为空，无法入库", cardId);
                return 1;
            }
            
            // 2) 主表
            TakBehaviorRecords rec = new TakBehaviorRecords();
            rec.setCardId(cardId);
            rec.setYardId(baseConfig.getYardName());
            rec.setTrackId(trackId);
            rec.setStartTime(new Date(boardingPoint.getTimestamp()));
            rec.setEndTime(new Date(dropOffPoint.getTimestamp()));
            rec.setPointCount((long) detailList.size());
            rec.setVisionId(visionEvent.getId());
            rec.setVehicleCode(visionEvent.getVin());
            // 根据车辆类型和事件类型确定 type（这里默认使用发运装车类型，可根据实际需求调整）
            // 0 到达卸车 1 发运装车 2 轿运车装车 3 轿运车卸车 4地跑入库 5 地跑出库
            long type = 1L; // 默认发运装车
            if (vehicleType == LoadingStrategyFactory.VehicleType.TRAIN){
                if (eventType == 0){
                    type = 1L;
                }else{
                    type = 0L;
                }
            } else if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED) {
                if (eventType == 0){
                    type = 3L;
                }else{
                    type = 2L;
                }
            } else if (vehicleType == LoadingStrategyFactory.VehicleType.GROUND_VEHICLE) {
                if (eventType == 0){
                    type = 5L;
                }else{
                    type = 4L;
                }
            }
            rec.setType(type);
            
            rec.setDuration(DateTimeUtils.calculateTimeDifference(boardingPoint.getTimestamp(), dropOffPoint.getTimestamp()));
            rec.setState("完成");
            
            // 统计信标信息
            BeaconStatistics beaconStats = getMostFrequentBeaconInSession(sessionPoints, vehicleType);
            rec.setBeaconName(beaconStats.zoneName);
            rec.setRfidName(beaconStats.zoneNameRfid);
            rec.setArea(beaconStats.zone);
            
            rec.setTakBehaviorRecordDetailList(detailList);
            
            // 3) 入库
            iTakBehaviorRecordsService.insertTakBehaviorRecords(rec);
            iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetailAll(detailList);
            
            log.info("卡ID: {} 的会话数据入库成功，trackId: {}, 点数: {}", cardId, trackId, detailList.size());
            return 0;
        } catch (Exception e) {
            log.error("卡ID: {} 的会话数据入库失败", cardId, e);
            return 2;
        }
    }
    
    /**
     * 获取会话期间出现次数最多的信标信息
     */
    private BeaconStatistics getMostFrequentBeaconInSession(List<LocationPoint> sessionPoints, 
                                                           LoadingStrategyFactory.VehicleType vehicleType) {
        BeaconStatistics stats = new BeaconStatistics();
        
        if (sessionPoints == null || sessionPoints.isEmpty()) {
            return stats;
        }
        
        // 根据车辆类型确定信标类型和查询参数
        String beaconType;
        String location = null;
        String area = null;
        List<TakBeaconInfo> beaconsInRange = new ArrayList<>();
        
        if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED) {
            // 板车作业区
            beaconType = "板车作业区";
        } else if (vehicleType == LoadingStrategyFactory.VehicleType.GROUND_VEHICLE) {
            // 地跑
            beaconType = "地跑";
        } else {
            // 火车作业区（货运线作业台）
            beaconType = "货运线作业台";
            // 获取地跑信标
            List<TakBeaconInfo> groundBeacons = tagBeacon.getBeaconsInRangeForPoints(
                    sessionPoints,
                    baseConfig.getJoysuch().getBuildingId(),
                    "地跑",
                    location,
                    area
            );
            if (groundBeacons != null) {
                beaconsInRange.addAll(groundBeacons);
            }
        }
        
        // 获取信标列表
        List<TakBeaconInfo> beacons = tagBeacon.getBeaconsInRangeForPoints(
                sessionPoints,
                baseConfig.getJoysuch().getBuildingId(),
                beaconType,
                location,
                area
        );
        if (beacons != null) {
            beaconsInRange.addAll(beacons);
        }
        
        if (beaconsInRange.isEmpty()) {
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
        
        if (maxCountInfo != null && maxCountInfo.count >= 1 && maxCountInfo.beacon != null) {
            TakBeaconInfo mostFrequentBeacon = maxCountInfo.beacon;
            stats.zoneName = mostFrequentBeacon.getName();
            stats.zoneNameRfid = mostFrequentBeacon.getRfidName();
            stats.zone = mostFrequentBeacon.getArea();
        }
        
        return stats;
    }
    
    /**
     * 信标统计结果
     */
    private static class BeaconStatistics {
        String zoneName;
        String zoneNameRfid;
        String zone;
    }
    
    /**
     * 信标计数信息
     */
    private static class BeaconCountInfo {
        TakBeaconInfo beacon;
        int count;
    }


    private List<LocationPoint> preprocessBatch(List<LocationPoint> batch) {
        List<LocationPoint> normal = new ArrayList<>();
        batch.sort((p1, p2) -> Long.compare(p1.getTimestamp(), p2.getTimestamp()));
        for (LocationPoint raw : batch) {
            if (DateTimeUtils.dateTimeSSSStrToDateTimeStr(raw.getAcceptTime()).equals("2025-11-17 11:07:00")){
                System.out.println("⚠️ 检测到车辆已进入地跑区域（地跑）");
            }
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
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(LocationPoint::getTimestamp, x -> x, (a, b) -> a, TreeMap::new),
                        m -> new ArrayList<>(m.values())));
    }
}

