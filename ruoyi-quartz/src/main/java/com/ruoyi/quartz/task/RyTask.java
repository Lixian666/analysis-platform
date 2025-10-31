package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.DriverTracker;
import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private DriverTracker tracker;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private CenterWorkHttpUtils centerWorkHttpUtils;

    @Autowired
    private ApplicationContext applicationContext;  // 用于获取 prototype bean
    
    // 注意：批处理任务使用 prototype，每次获取新实例
    // 实时任务仍然需要共享实例，所以需要延迟初始化一个共享 tracker
    private RealTimeDriverTracker sharedTracker;  // 用于实时任务的共享实例

    private BoardingDetector detector = new BoardingDetector();

    OutlierFilter outlierFilter = new OutlierFilter();

    private Deque<LocationPoint> recordWindow = new ArrayDeque<>(FilterConfig.RECORD_POINTS_SIZE);
    private List<LocationPoint>  recordPoints = new ArrayList<>();
    
    // 实时流式处理的线程池（全局共享，避免重复创建）
    private ScheduledExecutorService realtimeExecutorService;
    
    // 控制实时任务的运行状态
    private volatile boolean isRealtimeTaskRunning = false;
    
    // 记录每个卡最后处理的时间（线程安全）
    private ConcurrentHashMap<String, LocalDateTime> lastProcessTimeMap = new ConcurrentHashMap<>();
    
    // 统计信息（线程安全）
    private ConcurrentHashMap<String, AtomicInteger> cardProcessCountMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> cardErrorCountMap = new ConcurrentHashMap<>();
    
    // 每个卡独立的 tracker 实例（用于测试方法，线程安全）
    private ConcurrentHashMap<String, RealTimeDriverTracker> cardTrackerMap = new ConcurrentHashMap<>();

    private String startTime;
    private String endTime;
    /**
     * 实时任务（需要共享状态）
     */
    public void realDriverTrackerZQ() throws ParseException {
        // 延迟初始化共享 tracker（实时任务需要保持状态）
        if (sharedTracker == null) {
            sharedTracker = applicationContext.getBean(RealTimeDriverTracker.class);
        }

        String data = baseConfig.LOCATION_CARD_TYPE;
        String date = "未获取到日期";

//        JSONObject jsonObject = ZQOpenApi.getListOfCards();
//        JSONArray points = jsonObject.getJSONArray("data");
//        if (points != null && !points.isEmpty()) {
//
//        }
        String cardId = "1918B3000BA3";
        String buildId = baseConfig.getJoysuch().getBuildingId();
        String startTimeStr = "2025-08-12 15:50:00";
        String endTimeStr = "2025-08-12 17:00:00";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);


        // 循环从 start 到 end，每次加 10 秒
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            // 时间 +10 秒
            LocalDateTime startCurrent = current;
            LocalDateTime endCurrent = current.plusSeconds(10);
            current = endCurrent;
            String startStr = DateTimeUtils.localDateTime2String(startCurrent);
            String endStr = DateTimeUtils.localDateTime2String(endCurrent);
            JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startStr, endStr));
            JSONArray points = jsonObject.getJSONArray("data");
            List<LocationPoint> LocationPoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    if (date.equals("未获取到日期")){
                        date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                    }
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
            if (LocationPoints.size() > 0){
                sharedTracker.ingest(LocationPoints);
            }
//            List<LocationPoint> normalPoints = new ArrayList<>();
//            for (LocationPoint rawPoint : LocationPoints){
//                int state = outlierFilter.isValid(rawPoint);
//                if (!(state == 0)) {
//                    if (state == 1){
//                        System.out.println("⚠️  时间间隔异常定位点已剔除：" + rawPoint);
//                    }
////                    else if (state == 2) {
////                    System.out.println("⚠️  速度异常定位点已剔除：" + rawPoint);
////                }
//                    else if (state == 3) {
//                        System.out.println("⚠️  定位异常定位点已剔除：" + rawPoint);
//                    } else {
//                        System.out.println("⚠️  异常定位点已剔除：" + rawPoint);
//                    }
//                }else {
//                    // 正常点
//                    normalPoints.add(rawPoint);
//                }
//            }
//            for (LocationPoint point : normalPoints){
//                recordPoints.add(point);
//                if (recordPoints.size() >= FilterConfig.RECORD_POINTS_SIZE){
//                    List<LocationPoint> newPoints = new OutlierFilter().fixTheData(recordPoints);
//                    if (newPoints.size() < FilterConfig.RECORD_POINTS_SIZE){
//                        recordWindow = new ArrayDeque<>(newPoints);
//                        continue;
//                    }
//                    tracker.realHandleNewRawPoint(newPoints);
//                }
//            }
//            for (LocationPoint point : normalPoints){
//                recordWindow.addLast(point);
//                while (recordWindow.size() > FilterConfig.RECORD_POINTS_SIZE) recordWindow.removeFirst();
//                if (recordWindow.size() == FilterConfig.RECORD_POINTS_SIZE){
//                    List<LocationPoint> window = new ArrayList<>(recordWindow);
//                    List<LocationPoint> newPoints = new OutlierFilter().fixTheData(window);
//                    if (newPoints.size() < FilterConfig.RECORD_POINTS_SIZE){
//                        recordWindow = new ArrayDeque<>(newPoints);
//                        continue;
//                    }
//                    tracker.realHandleNewRawPoint(newPoints);
//                }
//            }
            String shpFilePath = baseConfig.getOutputShpPath() + "/" + date + "/" + data + "/";
            DriverTracker.cardId = "1918B3000BA3";
            DriverTracker.shpFilePath = shpFilePath;
        }
    }

    /**
     * 实时任务测试（历史数据模拟）多线程
     * 支持处理大量卡数据（几百甚至上千），自动根据可用资源调整并发数
     * 模拟实时处理：每次处理10秒的数据
     */
    public void realDriverTrackerZQTestMultitasking(){

        List<String> cards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000A79",
                        "1918B3000BA3",
                        "1918B30005D6",
                        "1918B3000561"
                )
        );

        String startTimeStr = "2025-10-17 16:50:00";
        String endTimeStr = "2025-10-17 19:00:00";
        
        // 根据CPU核心数和卡数量智能计算线程池大小
        // 可用CPU核心数 * 2（考虑IO密集型任务），最少5个，最多30个（考虑数据库连接池限制）
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int optimalThreadCount = Math.max(5, Math.min(availableProcessors * 2, 30));
        int threadPoolSize = Math.min(cards.size(), optimalThreadCount);
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        
        // 用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(cards.size());
        
        // 用于收集异常信息（线程安全）
        ConcurrentHashMap<String, Exception> errorMap = new ConcurrentHashMap<>();
        
        // 成功处理计数器（线程安全）
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger totalProcessed = new AtomicInteger(0);
        
        System.out.println("========================================");
        System.out.println("开始多线程处理卡数据");
        System.out.println("总卡数: " + cards.size());
        System.out.println("CPU核心数: " + availableProcessors);
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("预计批次: " + (int)Math.ceil((double)cards.size() / threadPoolSize));
        System.out.println("提示: 使用错峰启动避免并发token冲突");
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
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
        
        // 为每个卡创建一个处理任务，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            final int startDelay = delayIndex * 100; // 每个任务延迟100ms启动
            delayIndex++;
            
            executorService.submit(() -> {
                RealTimeDriverTracker tracker = null;
                try {
                    // 错峰启动，避免同时请求x
                    if (startDelay > 0) {
                        Thread.sleep(startDelay);
                    }
                    // 每个线程获取独立的 tracker 实例（prototype 模式）
                    tracker = applicationContext.getBean(RealTimeDriverTracker.class);
                    processCardDataWithRetry(cardId, startTimeStr, endTimeStr, 3, tracker);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    System.err.println("处理卡 " + cardId + " 时发生异常: " + errorMsg);
                    e.printStackTrace();
                    errorMap.put(cardId, e);
                } finally {
                    int processed = totalProcessed.incrementAndGet();
                    // 每处理10%输出一次进度
                    if (processed % Math.max(1, cards.size() / 10) == 0 || processed == cards.size()) {
                        System.out.println(String.format("进度: %d/%d (%.1f%%) - 成功: %d, 失败: %d", 
                            processed, cards.size(), 
                            (processed * 100.0 / cards.size()),
                            successCount.get(),
                            errorMap.size()));
                    }
                    latch.countDown();
                }
            });
        }
        
        try {
            // 根据卡数量动态调整超时时间：每100个卡增加30分钟
            long timeoutMinutes = 30 + (cards.size() / 100) * 30;
            boolean completed = latch.await(timeoutMinutes, TimeUnit.MINUTES);
            
            long endTime = System.currentTimeMillis();
            long totalTimeSeconds = (endTime - startTime) / 1000;
            
            System.out.println("========================================");
            if (!completed) {
                System.err.println("警告：部分任务在 " + timeoutMinutes + " 分钟内未完成");
            } else {
                System.out.println("所有卡的数据处理完成！");
            }
            
            // 输出详细统计
            System.out.println("处理总数: " + totalProcessed.get() + " / " + cards.size());
            System.out.println("成功: " + successCount.get());
            System.out.println("失败: " + errorMap.size());
            System.out.println("总耗时: " + DateTimeUtils.formatTime(totalTimeSeconds));
            if (successCount.get() > 0) {
                System.out.println("平均处理速度: " + String.format("%.2f", totalTimeSeconds * 1.0 / successCount.get()) + " 秒/卡");
            }
            
            // 输出错误详情
            if (!errorMap.isEmpty()) {
                System.err.println("\n处理失败的卡列表:");
                errorMap.forEach((cardId, exception) -> {
                    System.err.println("  - 卡号: " + cardId + ", 错误: " + exception.getMessage());
                });
            }
            System.out.println("========================================");
            
        } catch (InterruptedException e) {
            System.err.println("等待任务完成时被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // 关闭线程池
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // 不需要手动清理，每个线程的 tracker 实例会被 GC 回收
            System.out.println("✅ 多线程批处理任务完成");
        }
    }
    
    /**
     * 自定义时间范围的实时流式数据处理测试方法（持续运行，每10秒获取一次数据）
     * 支持自定义开始结束时间，用于测试场景
     * 
     * 使用方法：
     * 1. 启动：realDriverTrackerZQRealtimeTestWithCustomTime(cards, "2025-10-17 16:59:00", "2025-10-17 19:27:30")
     * 2. 停止：realDriverTrackerZQRealtimeStop()
     * 3. 查看状态：realDriverTrackerZQRealtimeStatus()
     */
    public void realDriverTrackerZQRealtimeTestWithCustomTime() {
        List<String> cards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000561",
                        "1918B3000978")
        );
//        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.CAR;
        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.TRUCK;
        String startTimeStr = "2025-10-23 18:04:00";
        String endTimeStr = "2025-10-23 18:19:00";
        if (isRealtimeTaskRunning) {
            System.out.println("实时任务已在运行中，无需重复启动");
            return;
        }
        
        // 参数验证
        if (cards == null || cards.isEmpty()) {
            System.err.println("❌ 错误：卡号列表不能为空");
            return;
        }
        
        try {
            LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
            LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
            
            if (startTime.isAfter(endTime)) {
                System.err.println("❌ 错误：开始时间不能晚于结束时间");
                return;
            }
        } catch (Exception e) {
            System.err.println("❌ 错误：时间格式不正确，请使用格式 yyyy-MM-dd HH:mm:ss");
            return;
        }
        
        // 根据CPU核心数智能计算线程池大小
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadPoolSize = Math.max(5, Math.min(availableProcessors * 2, 30));
        
        // 创建定时任务线程池
        realtimeExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
        isRealtimeTaskRunning = true;
        
        // 初始化统计信息
        lastProcessTimeMap.clear();
        cardProcessCountMap.clear();
        cardErrorCountMap.clear();
        cardTrackerMap.clear();
        
        // 设置全局时间范围（用于判断是否到达结束时间）
        this.startTime = startTimeStr;
        this.endTime = endTimeStr;
        
        System.out.println("========================================");
        System.out.println("启动自定义时间范围的实时流式数据处理测试");
        System.out.println("总卡数: " + cards.size());
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("数据获取间隔: 10秒");
        System.out.println("开始时间: " + startTimeStr);
        System.out.println("结束时间: " + endTimeStr);
        System.out.println("========================================");
        
        // 预热：提前获取token
        try {
            System.out.println("正在预热，获取AccessToken...");
            zqOpenApi.getHeaders();
            System.out.println("✓ AccessToken预热成功");
            Thread.sleep(200);
        } catch (Exception e) {
            System.err.println("⚠️ AccessToken预热失败: " + e.getMessage());
        }
        
        // 为每个卡创建定时任务，每10秒执行一次，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            // 初始化统计
            cardProcessCountMap.put(cardId, new AtomicInteger(0));
            cardErrorCountMap.put(cardId, new AtomicInteger(0));
            // 设置每个卡的初始处理时间为用户指定的开始时间
            lastProcessTimeMap.put(cardId, DateTimeUtils.str2DateTime(startTimeStr));
            
            // 为每个卡创建独立的 tracker 实例（在整个任务运行期间持续使用）
            RealTimeDriverTracker cardTracker = applicationContext.getBean(RealTimeDriverTracker.class);
            cardTrackerMap.put(cardId, cardTracker);
            System.out.println("✓ 为卡 " + cardId + " 创建独立的 RealTimeDriverTracker 实例");
            
            // 错峰启动：第1个卡立即开始，后续每个卡延迟200ms
            final long initialDelay = delayIndex * 200;
            delayIndex++;
            
            // 延迟initialDelay毫秒开始，每10秒执行一次
            realtimeExecutorService.scheduleAtFixedRate(() -> {
                // 检查是否超过结束时间
                LocalDateTime currentLastTime = lastProcessTimeMap.get(cardId);
                if (currentLastTime.isAfter(DateTimeUtils.str2DateTime(this.endTime))) {
                    System.out.println("✓ 卡 " + cardId + " 已处理到结束时间，停止处理");
                    
                    // 检查是否所有卡都已完成
                    boolean allCompleted = true;
                    for (String id : cards) {
                        if (!lastProcessTimeMap.get(id).isAfter(DateTimeUtils.str2DateTime(this.endTime))) {
                            allCompleted = false;
                            break;
                        }
                    }
                    
                    if (allCompleted) {
                        System.out.println("✓ 所有卡都已处理到结束时间，准备停止任务...");
                        // 延迟停止，让其他线程完成当前操作
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                realDriverTrackerZQRealtimeStop();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                    return;
                }
                
                if (!isRealtimeTaskRunning) {
                    return; // 如果任务已停止，不再处理
                }
                
                try {
                    processCardDataRealtimeTestWithRetry(cardId, vt, 2); // 最多重试2次
                    cardProcessCountMap.get(cardId).incrementAndGet();
                } catch (Exception e) {
                    cardErrorCountMap.get(cardId).incrementAndGet();
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                    System.err.println("[" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) + 
                                     "] 处理卡 " + cardId + " 失败: " + errorMsg);
                }
            }, initialDelay, 10000, TimeUnit.MILLISECONDS);
        }
        
        // 启动监控线程，每分钟输出一次统计信息
        realtimeExecutorService.scheduleAtFixedRate(() -> {
            if (isRealtimeTaskRunning) {
                printRealtimeStatus();
            }
        }, 60, 60, TimeUnit.SECONDS);
        
        System.out.println("✓ 测试任务已启动！使用 realDriverTrackerZQRealtimeStop() 停止任务");
    }
    
    /**
     * 带重试机制的实时数据处理（用于自定义时间测试）
     */
    private void processCardDataRealtimeTestWithRetry(String cardId, RealTimeDriverTracker.VehicleType vt, int maxRetries) throws Exception {
        Exception lastException = null;
        
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                if (retry > 0) {
                    // 重试前短暂等待：500ms、1000ms
                    long waitTime = 500 * retry;
                    Thread.sleep(waitTime);
                }
                
                processCardDataRealtimeTest(cardId, vt);
                return; // 成功则返回
                
            } catch (Exception e) {
                lastException = e;
                if (retry < maxRetries - 1) {
                    // 不是最后一次，继续重试
                    continue;
                }
            }
        }
        
        // 所有重试都失败，抛出异常
        throw new Exception("测试处理卡 " + cardId + " 失败", lastException);
    }
    
    /**
     * 处理单个卡的实时数据（用于自定义时间测试）
     * 每次处理从上次结束位置开始的10秒数据
     * 使用该卡独立的 tracker 实例来维护状态
     */
    private void processCardDataRealtimeTest(String cardId, RealTimeDriverTracker.VehicleType vt) {
        String buildId = baseConfig.getJoysuch().getBuildingId();
        LocalDateTime lastTime = lastProcessTimeMap.get(cardId);
        LocalDateTime currentTime = lastTime.plusSeconds(10);
        
        // 获取该卡专属的 tracker 实例
        RealTimeDriverTracker cardTracker = cardTrackerMap.get(cardId);
        if (cardTracker == null) {
            throw new RuntimeException("未找到卡 " + cardId + " 的 tracker 实例");
        }
        
        // 获取从lastTime到currentTime的数据（10秒窗口）
        String startTimeStr = DateTimeUtils.localDateTime2String(lastTime);
        String endTimeStr = DateTimeUtils.localDateTime2String(currentTime);
        
        // 获取位置点数据 - 添加空值检查
        String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
        if (pointsResponse == null) {
            throw new RuntimeException("获取位置点数据返回null");
        }
        JSONObject jsonObject = JSONObject.parseObject(pointsResponse);
        
        String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, 
                DateTimeUtils.localDateTime2String(lastTime.minusSeconds(2)), 
                DateTimeUtils.localDateTime2String(currentTime.plusSeconds(2)));
        if (tagResponse == null) {
            throw new RuntimeException("获取标签状态数据返回null");
        }
        JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);
        
        JSONArray points = jsonObject.getJSONArray("data");
        JSONArray tagData = tagJsonObject.getJSONArray("data");
        
        List<LocationPoint> LocationPoints = new ArrayList<>();
        if (points != null && !points.isEmpty()) {
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                if (plist != null) {
                    for (int j = 0; j < plist.size(); j++){
                        LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                        LocationPoint point1 = new LocationPoint(
                                cardId,
                                point.getLongitude(),
                                point.getLatitude(),
                                DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                                Long.parseLong(point.getTime()));
                        LocationPoints.add(point1);
                    }
                }
            }
        }
        
        // 融合位置数据和标签数据
        if (!LocationPoints.isEmpty()) {
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints, tagData);
            
            if (LocationPoints.size() > 0){
                // 使用该卡独立的 tracker 实例处理数据（维持状态连续性）
                cardTracker.replayHistorical(LocationPoints, vt);
                System.out.println("[" + DateTimeUtils.localDateTime2String(currentTime) + 
                                 "] ✓ 卡 " + cardId + " 处理了 " + LocationPoints.size() + " 个点 (时间窗口: " + 
                                 startTimeStr + " ~ " + endTimeStr + ")");
            } else {
                System.out.println("[" + DateTimeUtils.localDateTime2String(currentTime) + 
                                 "] ⚠️ 卡 " + cardId + " 无有效数据点 (时间窗口: " + 
                                 startTimeStr + " ~ " + endTimeStr + ")");
            }
        }
        
        // 更新最后处理时间为当前窗口结束时间
        lastProcessTimeMap.put(cardId, currentTime);
    }
    
    /**
     * 实时流式数据处理（持续运行，每10秒获取一次数据）
     * 适用于实际生产环境，支持多卡并发处理
     * 
     * 使用方法：
     * 1. 启动：realDriverTrackerZQRealtimeStart()
     * 2. 停止：realDriverTrackerZQRealtimeStop()
     * 3. 查看状态：realDriverTrackerZQRealtimeStatus()
     */
    public void realDriverTrackerZQRealtimeStart() {
        if (isRealtimeTaskRunning) {
            System.out.println("实时任务已在运行中，无需重复启动");
            return;
        }
        
        List<String> cards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000BA8",
                        "1918B3000A79",
                        "1918B3000BA3",
                        "1918B30005D6",
                        "1918B3000561"
                )
        );
        
        // 根据CPU核心数智能计算线程池大小（限制最大值避免数据库连接池耗尽）
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadPoolSize = Math.max(5, Math.min(availableProcessors * 2, 30));
        
        // 创建定时任务线程池
        realtimeExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
        isRealtimeTaskRunning = true;
        
        // 初始化统计信息
        lastProcessTimeMap.clear();
        cardProcessCountMap.clear();
        cardErrorCountMap.clear();

//        String startTime = DateTimeUtils.localDateTime2String(LocalDateTime.now());
//        String endTime = DateTimeUtils.localDateTime2String(LocalDateTime.now().plusDays(1).withHour(3).withMinute(0).withSecond(0));
        startTime = "2025-10-17 16:59:00";
        endTime = "2025-10-17 19:27:30";

        System.out.println("========================================");
        System.out.println("启动实时流式数据处理任务");
        System.out.println("总卡数: " + cards.size());
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("数据获取间隔: 10秒");
        System.out.println("开始时间: " + startTime);
        System.out.println("========================================");
        
        // 预热：提前获取token
        try {
            System.out.println("正在预热，获取AccessToken...");
            zqOpenApi.getHeaders();
            System.out.println("✓ AccessToken预热成功");
            Thread.sleep(200);
        } catch (Exception e) {
            System.err.println("⚠️ AccessToken预热失败: " + e.getMessage());
        }
        
        // 为每个卡创建定时任务，每10秒执行一次，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            // 初始化统计
            cardProcessCountMap.put(cardId, new AtomicInteger(0));
            cardErrorCountMap.put(cardId, new AtomicInteger(0));
            lastProcessTimeMap.put(cardId, LocalDateTime.now());
            
            // 错峰启动：第1个卡立即开始，后续每个卡延迟200ms
            final long initialDelay = delayIndex * 200;
            delayIndex++;
            
            // 延迟initialDelay毫秒开始，每10秒执行一次
            realtimeExecutorService.scheduleAtFixedRate(() -> {
                if (DateTimeUtils.str2DateTime(startTime).isAfter(DateTimeUtils.str2DateTime(endTime))){
                    realDriverTrackerZQRealtimeStop();
                }
                if (!isRealtimeTaskRunning) {
                    return; // 如果任务已停止，不再处理
                }
                
                try {
                    processCardDataRealtimeWithRetry(cardId, 2); // 最多重试2次
                    cardProcessCountMap.get(cardId).incrementAndGet();
                } catch (Exception e) {
                    cardErrorCountMap.get(cardId).incrementAndGet();
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                    System.err.println("[" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) + 
                                     "] 处理卡 " + cardId + " 失败: " + errorMsg);
                }
            }, initialDelay, 10000, TimeUnit.MILLISECONDS);
        }
        
        // 启动监控线程，每分钟输出一次统计信息
        realtimeExecutorService.scheduleAtFixedRate(() -> {
            if (isRealtimeTaskRunning) {
                printRealtimeStatus();
            }
        }, 60, 60, TimeUnit.SECONDS);
        
        System.out.println("✓ 实时任务已启动！使用 realDriverTrackerZQRealtimeStop() 停止任务");
    }
    
    /**
     * 停止实时流式数据处理
     */
    public void realDriverTrackerZQRealtimeStop() {
        if (!isRealtimeTaskRunning) {
            System.out.println("实时任务未运行");
            return;
        }
        
        System.out.println("========================================");
        System.out.println("正在停止实时任务...");
        isRealtimeTaskRunning = false;
        
        if (realtimeExecutorService != null) {
            realtimeExecutorService.shutdown();
            try {
                if (!realtimeExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    realtimeExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                realtimeExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 输出最终统计
        printRealtimeStatus();
        
        // 清理 tracker 实例
        if (!cardTrackerMap.isEmpty()) {
            System.out.println("清理 " + cardTrackerMap.size() + " 个 tracker 实例...");
            cardTrackerMap.clear();
        }
        
        System.out.println("停止时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("✓ 实时任务已停止");
        System.out.println("========================================");
    }
    
    /**
     * 查看实时任务状态
     */
    public void realDriverTrackerZQRealtimeStatus() {
        System.out.println("========================================");
        System.out.println("实时任务状态");
        System.out.println("运行状态: " + (isRealtimeTaskRunning ? "运行中" : "已停止"));
        System.out.println("当前时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("========================================");
        
        if (isRealtimeTaskRunning) {
            printRealtimeStatus();
        }
    }
    
    /**
     * 打印实时统计信息
     */
    private void printRealtimeStatus() {
        System.out.println("\n--- 实时处理统计 [" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) + "] ---");
        
        int totalSuccess = 0;
        int totalError = 0;
        
        for (String cardId : cardProcessCountMap.keySet()) {
            int successCount = cardProcessCountMap.get(cardId).get();
            int errorCount = cardErrorCountMap.get(cardId).get();
            LocalDateTime lastTime = lastProcessTimeMap.get(cardId);
            
            totalSuccess += successCount;
            totalError += errorCount;
            
            System.out.println(String.format("  卡 %s: 成功=%d, 失败=%d, 最后处理=%s", 
                cardId, successCount, errorCount, 
                DateTimeUtils.localDateTime2String(lastTime)));
        }
        
        System.out.println(String.format("总计: 成功=%d, 失败=%d, 成功率=%.1f%%", 
            totalSuccess, totalError, 
            totalSuccess > 0 ? (totalSuccess * 100.0 / (totalSuccess + totalError)) : 0));
        System.out.println("-----------------------------------------------\n");
    }
    
    /**
     * 带重试机制的实时数据处理
     */
    private void processCardDataRealtimeWithRetry(String cardId, int maxRetries) throws Exception {
        Exception lastException = null;
        
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                if (retry > 0) {
                    // 重试前短暂等待：500ms、1000ms
                    long waitTime = 500 * retry;
                    Thread.sleep(waitTime);
                }
                
                processCardDataRealtime(cardId);
                return; // 成功则返回
                
            } catch (Exception e) {
                lastException = e;
                if (retry < maxRetries - 1) {
                    // 不是最后一次，继续重试
                    continue;
                }
            }
        }
        
        // 所有重试都失败，抛出异常
        throw new Exception("实时处理卡 " + cardId + " 失败", lastException);
    }
    
    /**
     * 处理单个卡的实时数据（每10秒调用一次）
     */
    private void processCardDataRealtime(String cardId) {
        String buildId = baseConfig.getJoysuch().getBuildingId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastProcessTimeMap.get(cardId);
        
        // 获取最近10秒的数据
        String startTimeStr = DateTimeUtils.localDateTime2String(lastTime);
        String endTimeStr = DateTimeUtils.localDateTime2String(now);
        
        // 获取位置点数据 - 添加空值检查
        String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
        if (pointsResponse == null) {
            throw new RuntimeException("获取位置点数据返回null");
        }
        JSONObject jsonObject = JSONObject.parseObject(pointsResponse);
        
        String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, 
                DateTimeUtils.localDateTime2String(lastTime.minusSeconds(2)), 
                DateTimeUtils.localDateTime2String(now.plusSeconds(2)));
        if (tagResponse == null) {
            throw new RuntimeException("获取标签状态数据返回null");
        }
        JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);
        
        JSONArray points = jsonObject.getJSONArray("data");
        JSONArray tagData = tagJsonObject.getJSONArray("data");
        
        List<LocationPoint> LocationPoints = new ArrayList<>();
        if (points != null && !points.isEmpty()) {
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                if (plist != null) {
                    for (int j = 0; j < plist.size(); j++){
                        LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                        LocationPoint point1 = new LocationPoint(
                                cardId,
                                point.getLongitude(),
                                point.getLatitude(),
                                DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                                Long.parseLong(point.getTime()));
                        LocationPoints.add(point1);
                    }
                }
            }
        }
        
        // 融合位置数据和标签数据
        if (!LocationPoints.isEmpty()) {
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints, tagData);
            
            if (LocationPoints.size() > 0){
                sharedTracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.CAR);
                System.out.println("[" + DateTimeUtils.localDateTime2String(now) + 
                                 "] ✓ 卡 " + cardId + " 处理了 " + LocationPoints.size() + " 个点");
            }
        }
        
        // 更新最后处理时间
        lastProcessTimeMap.put(cardId, now);
    }
    
    /**
     * 带重试机制的卡数据处理（解决并发token冲突问题）
     */
    private void processCardDataWithRetry(String cardId, String startTimeStr, String endTimeStr, int maxRetries, RealTimeDriverTracker tracker) throws Exception {
        Exception lastException = null;
        
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                if (retry > 0) {
                    // 重试前等待，使用指数退避策略：1秒、2秒、4秒...
                    long waitTime = (long) Math.pow(2, retry - 1) * 1000;
                    System.out.println("卡 " + cardId + " 第 " + (retry + 1) + " 次尝试（等待" + waitTime + "ms后重试）");
                    Thread.sleep(waitTime);
                }
                
                processCardData(cardId, startTimeStr, endTimeStr, tracker);
                return; // 成功则返回
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                
                if (retry < maxRetries - 1) {
                    System.err.println("⚠️ 卡 " + cardId + " 处理失败: " + errorMsg + "，将重试...");
                } else {
                    System.err.println("❌ 卡 " + cardId + " 处理失败，已达最大重试次数(" + maxRetries + ")");
                }
            }
        }
        
        // 所有重试都失败
        throw new Exception("处理卡 " + cardId + " 失败，已重试 " + maxRetries + " 次", lastException);
    }
    
    /**
     * 处理单个卡的数据（线程安全）- 用于历史数据批量处理
     * 模拟实时处理：每次处理10秒的数据，而不是一次性处理全部
     * 
     * @param tracker 该线程独立的 RealTimeDriverTracker 实例
     */
    private void processCardData(String cardId, String startTimeStr, String endTimeStr, RealTimeDriverTracker tracker) {
        System.out.println("线程 " + Thread.currentThread().getName() + " 开始处理卡: " + cardId + " (一次查询，分批处理)");
        
        String buildId = baseConfig.getJoysuch().getBuildingId();
        String date = "未获取到日期";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
        
        try {
            // 【优化】一次性获取整个时间段的所有数据
            String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
            if (pointsResponse == null) {
                System.err.println("⚠️ 线程 " + Thread.currentThread().getName() + 
                                 " 处理卡 " + cardId + " 未获取到数据");
                return;
            }
            
            JSONObject jsonObject = JSONObject.parseObject(pointsResponse);
            String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId,
                    DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)),
                    DateTimeUtils.localDateTime2String(endTime.plusSeconds(2)));
            if (tagResponse == null) {
                throw new RuntimeException("获取标签状态数据返回null");
            }
            JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);

            JSONArray points = jsonObject.getJSONArray("data");
            JSONArray tagData = tagJsonObject.getJSONArray("data");
            if (points == null || points.isEmpty()) {
                System.out.println("⚠️ 线程 " + Thread.currentThread().getName() + 
                                 " 处理卡 " + cardId + " 数据为空");
                return;
            }
            
            // 解析所有位置点到内存
            List<LocationPoint> allPoints = new ArrayList<>();
            if (points != null && !points.isEmpty()) {
                for (int i = 0; i < points.size(); i++){
                    JSONObject js = (JSONObject) points.get(i);
                    JSONArray plist = js.getJSONArray("points");
                    if (plist != null) {
                        for (int j = 0; j < plist.size(); j++){
                            LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                            if (date.equals("未获取到日期")){
                                date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                            }
                            LocationPoint point1 = new LocationPoint(
                                    cardId,
                                    point.getLongitude(),
                                    point.getLatitude(),
                                    DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                                    Long.parseLong(point.getTime()));
                            allPoints.add(point1);
                        }
                    }
                }
            }
            if (allPoints.isEmpty()) {
                                System.out.println("⚠️ 线程 " + Thread.currentThread().getName() +
                                 " 处理卡 " + cardId + " 解析后数据为空");
                return;
            }
            // 融合位置数据和标签数据
            if (!allPoints.isEmpty()) {
                // 融合位置数据和标签数据
                allPoints = FusionData.processesFusionLocationDataAndTagData(allPoints, tagData);
            }
            
            // 按时间戳排序
            allPoints.sort((p1, p2) -> Long.compare(p1.getTimestamp(), p2.getTimestamp()));
            
            // 【优化】按10秒时间窗口分批处理（在内存中分批，不再调用API）
            int totalBatches = 0;
            int totalPoints = allPoints.size();
            
            LocalDateTime current = startTime;
            int pointIndex = 0;
            
            while (!current.isAfter(endTime) && pointIndex < allPoints.size()) {
                LocalDateTime batchEnd = current.plusSeconds(10);
                long batchEndTimestamp = DateTimeUtils.convertToTimestamp(DateTimeUtils.localDateTime2String(batchEnd));
                
                // 收集这10秒内的所有点
                List<LocationPoint> batch = new ArrayList<>();
                while (pointIndex < allPoints.size() && 
                       allPoints.get(pointIndex).getTimestamp() < batchEndTimestamp) {
                    batch.add(allPoints.get(pointIndex));
                    pointIndex++;
                }
                
                // 处理这一批（模拟实时处理）
                if (!batch.isEmpty()) {
                    tracker.ingest(batch);
                    totalBatches++;
                }
                
                current = batchEnd;
            }
            
            System.out.println("✓ 线程 " + Thread.currentThread().getName() + 
                             " 完成处理卡: " + cardId + 
                             ", 总批次: " + totalBatches + 
                             ", 总点数: " + totalPoints);
            
        } catch (Exception e) {
            System.err.println("❌ 线程 " + Thread.currentThread().getName() + 
                             " 处理卡 " + cardId + " 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 实时任务测试（行为分析历史数据模拟）单线程
     */
    public void realDriverTrackerZQTest(){
        // 获取独立的 tracker 实例
        RealTimeDriverTracker tracker = applicationContext.getBean(RealTimeDriverTracker.class);
        
        try {
            String data = baseConfig.LOCATION_CARD_TYPE;
            String date = "未获取到日期";
            String buildId = baseConfig.getJoysuch().getBuildingId();
//        String cardId = "1918B3000BA3";
//        String startTimeStr = "2025-08-06 18:20:00";
//        String endTimeStr = "2025-08-06 21:00:00";
//        String cardId = "1918B3000BA8";
//        String startTimeStr = "2025-09-24 18:00:00";
//        String endTimeStr = "2025-09-24 19:40:00";
            String cardId = "1918B3000BA3";
//            String cardId = "1918B3000A79";
        String startTimeStr = "2025-09-28 17:00:00";
        String endTimeStr = "2025-09-28 19:00:00";
//            String startTimeStr = "2025-10-17 16:50:00";
//            String endTimeStr = "2025-10-17 19:00:00";
            LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
            LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
//        List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(baseConfig.getSwCenter().getTenantId(), startTimeStr + " 000", endTimeStr + " 000");
            JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
            JSONObject tagJsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)), DateTimeUtils.localDateTime2String(endTime.plusSeconds(2))));
            JSONArray points = jsonObject.getJSONArray("data");
            JSONArray tagData = tagJsonObject.getJSONArray("data");
            List<LocationPoint> LocationPoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    if (date.equals("未获取到日期")){
                        date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                    }
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints,tagData);
            if (LocationPoints.size() > 0){
//            int batchSize = 10;
//            for (int i = 0; i < LocationPoints.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, LocationPoints.size());
//                List<LocationPoint> batch = LocationPoints.subList(i, end);
//                tracker.ingest(batch);
//            }
                tracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.CAR);
            }
        } finally {
            // 不需要手动清理，tracker 实例会被 GC 回收
            System.out.println("✅ 批处理任务完成");
        }
    }
    /**
     * 实时任务测试有参（历史数据模拟）板车 单线程
     */
    public void realDriverTrackerZQTruckTestParams(String cardId, String startTimeStr, String endTimeStr){
        // 获取独立的 tracker 实例
        RealTimeDriverTracker tracker = applicationContext.getBean(RealTimeDriverTracker.class);

        try {
            String data = baseConfig.LOCATION_CARD_TYPE;
            String date = "未获取到日期";
            String buildId = baseConfig.getJoysuch().getBuildingId();
//            String cardId = "1918B3000561";
//            String startTimeStr = "2025-10-29 18:39:00";
//            String endTimeStr = "2025-10-29 19:50:00";
//            String startTimeStr = "2025-10-16 18:25:00";
//            String endTimeStr = "2025-10-16 19:50:00";
            LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
            LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
//            List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(baseConfig.getSwCenter().getTenantId(), startTimeStr + " 000", endTimeStr + " 000");
            JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
            JSONObject tagJsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)), DateTimeUtils.localDateTime2String(endTime.plusSeconds(2))));
            JSONArray points = jsonObject.getJSONArray("data");
            JSONArray tagData = tagJsonObject.getJSONArray("data");
            List<LocationPoint> LocationPoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    if (date.equals("未获取到日期")){
                        date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                    }
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints,tagData);
            if (LocationPoints.size() > 0){
//            int batchSize = 10;
//            for (int i = 0; i < LocationPoints.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, LocationPoints.size());
//                List<LocationPoint> batch = LocationPoints.subList(i, end);
//                tracker.ingest(batch);
//            }
                tracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.TRUCK);
            }
        } finally {
            // 不需要手动清理，tracker 实例会被 GC 回收
            System.out.println("✅ 批处理任务完成");
        }
    }
    /**
     * 实时任务测试（历史数据模拟）板车 单线程
     */
    public void realDriverTrackerZQTruckTest(){
        // 获取独立的 tracker 实例
        RealTimeDriverTracker tracker = applicationContext.getBean(RealTimeDriverTracker.class);
        
        try {
            String data = baseConfig.LOCATION_CARD_TYPE;
            String date = "未获取到日期";
            String buildId = baseConfig.getJoysuch().getBuildingId();
            String cardId = "1918B3000561";
//            String startTimeStr = "2025-10-29 18:39:00";
//            String endTimeStr = "2025-10-29 19:50:00";
            String startTimeStr = "2025-10-16 18:25:00";
            String endTimeStr = "2025-10-16 19:50:00";
            LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
            LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
//            List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(baseConfig.getSwCenter().getTenantId(), startTimeStr + " 000", endTimeStr + " 000");
            JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
            JSONObject tagJsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)), DateTimeUtils.localDateTime2String(endTime.plusSeconds(2))));
            JSONArray points = jsonObject.getJSONArray("data");
            JSONArray tagData = tagJsonObject.getJSONArray("data");
            List<LocationPoint> LocationPoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    if (date.equals("未获取到日期")){
                        date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                    }
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints,tagData);
            if (LocationPoints.size() > 0){
//            int batchSize = 10;
//            for (int i = 0; i < LocationPoints.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, LocationPoints.size());
//                List<LocationPoint> batch = LocationPoints.subList(i, end);
//                tracker.ingest(batch);
//            }
                tracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.TRUCK);
            }
        } finally {
            // 不需要手动清理，tracker 实例会被 GC 回收
            System.out.println("✅ 批处理任务完成");
        }
    }

    public void test() {
        System.out.println(baseConfig.isStayVerify());
    }


    public void driverTrackerZQ() {
        test();
//        String data = BaseConfig.LOCATION_CARD_TYPE;
//        String date = "未获取到日期";
//
////        JSONObject jsonObject = ZQOpenApi.getListOfCards();
////        JSONArray points = jsonObject.getJSONArray("data");
////        if (points != null && !points.isEmpty()) {
////
////        }
//        String cardId = "1918B3000BA3";
//        String buildId = "209885";
//        String startTime = "2025-08-06 16:00:00";
//        String endTime = "2025-08-06 21:00:00";
//        JSONObject jsonObject = JSONObject.parseObject(getListOfPoints(cardId, buildId, startTime, endTime));
//        JSONArray points = jsonObject.getJSONArray("data");
//        List<LocationPoint> LocationPoints = new ArrayList<>();
//        for (int i = 0; i < points.size(); i++){
//            JSONObject js = (JSONObject) points.get(i);
//            JSONArray plist = js.getJSONArray("points");
//            for (int j = 0; j < plist.size(); j++){
//                LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
//                if (date.equals("未获取到日期")){
//                    date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
//                }
//                LocationPoint point1 = new LocationPoint(
//                        cardId,
//                        point.getLongitude(),
//                        point.getLatitude(),
//                        DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
//                        Long.parseLong(point.getTime()));
//                LocationPoints.add(point1);
//            }
//        }
//        String shpFilePath = outputShpPath + "/" + date + "/" + data + "/";
//        DriverTracker.cardId = "1918B3000BA3";
//        DriverTracker.shpFilePath = shpFilePath;
//        // 生成原始点位数据和时间序列清洗过的数据shp文件
//        DriverTracker.processWithAnchorDataZQ(LocationPoints, data);
//        // 再次根据点位、是否时间一样、是否漂移清洗数据
//        List<LocationPoint> newPoints = new OutlierFilter().fixTheData(LocationPoints);
//        if (baseConfig.isOutputShp()){
//            //清洗过运动或停留数据后生成shp文件
//            DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
//        }
//        // 开始行为分析
//        tracker.handleNewRawPoint(newPoints);

    }

    public void driverTracker() {
        String data = null;
        String date = "未获取到日期";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250710.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250710定位卡63856RTK.json";1
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250724.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250729.json";
        String file = "D:\\PlatformData\\定位卡数据\\鱼嘴\\20250729.json";
        JSONObject jsonObject = JsonUtils.loadJson(file);
        JSONArray points = jsonObject.getJSONArray("data");
        if (points != null && !points.isEmpty()) {
            JSONObject firstObj = points.getJSONObject(0);
            if (firstObj.containsKey("trajectoryId")) {
                // 存在 trajectoryId
                data = BaseConfig.OTHER;
            } else {
                // 不存在 trajectoryId
                data = BaseConfig.RTK;
            }
            if (firstObj.containsKey("recordTimeLength")){
                // 存在 acceptTime
                date = DateTimeUtils.timestampToDateStr(Long.parseLong(firstObj.getString("recordTimeLength")));
            } else if (firstObj.containsKey("timestamp")) {
                date = DateTimeUtils.timestampToDateStr(Long.parseLong(firstObj.getString("timestamp")));
            }
        }
        if (data == null){
            return;
        }
        String shpFilePath = baseConfig.getOutputShpPath() + "/" + date + "/" + data + "/";
        DriverTracker.shpFilePath = shpFilePath;
        List<LocationPoint> LocationPoints = new DriverTracker().processWithAnchorData(points, data);
        // 按卡号分组
        if (data.equals("rtk")){
            Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardId));
            for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (baseConfig.isOutputShp()){
                    //清洗过运动或停留数据后生成shp文件
                    DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
                }
                DriverTracker.cardId = String.valueOf(entry.getKey());
                // 开始行为分析
                tracker.handleNewRawPoint(newPoints);
//                DriverTracker tracker = new DriverTracker();
                // 开始行为分析
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }
            }
        } else if (data.equals("other")){
            Map<String, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardUUID));
            for (Map.Entry<String, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (baseConfig.isOutputShp()){
                    //清洗过运动或停留数据后生成shp文件
                    DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
                }
                DriverTracker.cardId = entry.getKey();
                // 开始行为分析
                tracker.handleNewRawPoint(newPoints);
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }

            }
        }
    }
}
