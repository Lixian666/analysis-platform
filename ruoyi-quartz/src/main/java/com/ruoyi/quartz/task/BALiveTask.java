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
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.http.HttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component("BALiveTask")
public class BALiveTask {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private ApplicationContext applicationContext;  // 用于获取 prototype bean

    // 火车装卸的线程池
    private ScheduledExecutorService carExecutorService;

    // 板车装卸的线程池
    private ScheduledExecutorService truckExecutorService;

    // 控制火车装卸任务的运行状态
    private volatile boolean isCarTaskRunning = false;

    // 控制板车装卸任务的运行状态
    private volatile boolean isTruckTaskRunning = false;

    // 火车装卸：记录每个卡最后处理的时间（线程安全）
    private ConcurrentHashMap<String, LocalDateTime> carLastProcessTimeMap = new ConcurrentHashMap<>();

    // 板车装卸：记录每个卡最后处理的时间（线程安全）
    private ConcurrentHashMap<String, LocalDateTime> truckLastProcessTimeMap = new ConcurrentHashMap<>();

    // 火车装卸：统计信息（线程安全）
    private ConcurrentHashMap<String, AtomicInteger> carCardProcessCountMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> carCardErrorCountMap = new ConcurrentHashMap<>();

    // 板车装卸：统计信息（线程安全）
    private ConcurrentHashMap<String, AtomicInteger> truckCardProcessCountMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> truckCardErrorCountMap = new ConcurrentHashMap<>();

    // 火车装卸：每个卡独立的 tracker 实例（用于测试方法，线程安全）
    private ConcurrentHashMap<String, RealTimeDriverTracker> carCardTrackerMap = new ConcurrentHashMap<>();

    // 板车装卸：每个卡独立的 tracker 实例（用于测试方法，线程安全）
    private ConcurrentHashMap<String, RealTimeDriverTracker> truckCardTrackerMap = new ConcurrentHashMap<>();

    // 火车装卸时间范围
    private String carStartTime;
    private String carEndTime;

    // 板车装卸时间范围
    private String truckStartTime;
    private String truckEndTime;

    /**
     * 启动火车装卸的实时任务
     */
    public void realDriverTrackerZQRealtimeWithNowTimeV2() {
        // 火车装卸的卡号列表
        DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
        List<String> cards = dataAcquisition.getCardIdList(1);
        if (cards == null || cards.isEmpty()) {
            System.out.println("没有需要处理的卡，任务跳过");
            return;
        }

        // 开始时间：当前时间
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        // 结束时间：下一个凌晨3点
        // 如果当前时间还没到今天的凌晨3点，则结束时间是今天的凌晨3点
        // 如果当前时间已经过了今天的凌晨3点，则结束时间是明天的凌晨3点
        LocalDateTime today3am = now.withHour(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow3am = now.plusDays(1).withHour(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = now.isBefore(today3am) ? today3am : tomorrow3am;

        String startTimeStr = DateTimeUtils.localDateTime2String(now);
        String endTimeStr = DateTimeUtils.localDateTime2String(endTime);
        if (startTimeStr == null || endTimeStr == null){
            System.out.println("开始结束时间为null，任务跳过");
            return;
        }

        // 火车装卸
        if (!cards.isEmpty()) {
            startRealtimeTaskForCards(cards, RealTimeDriverTracker.VehicleType.CAR, startTimeStr, endTimeStr, "火车装卸");
        }
    }

    /**
     * 启动板车装卸的实时任务
     */
    public void realDriverTrackerZQRealtimeTruckWithNowTimeV2() {
        // 板车装卸的卡号列表
        List<String> truckCards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000561",
                        "1918B3000BA3",
                        "1918B3000978")
        );

        // 开始时间：当前时间
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        // 结束时间：下一个凌晨3点
        // 如果当前时间还没到今天的凌晨3点，则结束时间是今天的凌晨3点
        // 如果当前时间已经过了今天的凌晨3点，则结束时间是明天的凌晨3点
        LocalDateTime today3am = now.withHour(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow3am = now.plusDays(1).withHour(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = now.isBefore(today3am) ? today3am : tomorrow3am;

        String startTimeStr = DateTimeUtils.localDateTime2String(now);
        String endTimeStr = DateTimeUtils.localDateTime2String(endTime);

        System.out.println("当前时间: " + startTimeStr);
        System.out.println("结束时间: " + endTimeStr);

        // 板车装卸
        if (!truckCards.isEmpty()) {
            startRealtimeTaskForCards(truckCards, RealTimeDriverTracker.VehicleType.TRUCK, startTimeStr, endTimeStr, "板车装卸");
        }
    }



    private void startRealtimeTaskForCards(List<String> cards, RealTimeDriverTracker.VehicleType vt,
                                           String startTimeStr, String endTimeStr, String businessType) {
        // 根据车辆类型选择对应的状态变量
        boolean isTaskRunning = (vt == RealTimeDriverTracker.VehicleType.CAR) ? isCarTaskRunning : isTruckTaskRunning;
        
        if (isTaskRunning) {
            System.out.println(businessType + "实时任务已在运行中，无需重复启动");
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

        // 根据车辆类型初始化对应的资源
        ScheduledExecutorService executorService;
        ConcurrentHashMap<String, LocalDateTime> lastProcessTimeMap;
        ConcurrentHashMap<String, AtomicInteger> cardProcessCountMap;
        ConcurrentHashMap<String, AtomicInteger> cardErrorCountMap;
        ConcurrentHashMap<String, RealTimeDriverTracker> cardTrackerMap;
        
        if (vt == RealTimeDriverTracker.VehicleType.CAR) {
            // 火车装卸
            carExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
            executorService = carExecutorService;
            isCarTaskRunning = true;
            
            carLastProcessTimeMap.clear();
            carCardProcessCountMap.clear();
            carCardErrorCountMap.clear();
            carCardTrackerMap.clear();
            
            lastProcessTimeMap = carLastProcessTimeMap;
            cardProcessCountMap = carCardProcessCountMap;
            cardErrorCountMap = carCardErrorCountMap;
            cardTrackerMap = carCardTrackerMap;
            
            // 设置时间范围
            this.carStartTime = startTimeStr;
            this.carEndTime = endTimeStr;
        } else {
            // 板车装卸
            truckExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
            executorService = truckExecutorService;
            isTruckTaskRunning = true;
            
            truckLastProcessTimeMap.clear();
            truckCardProcessCountMap.clear();
            truckCardErrorCountMap.clear();
            truckCardTrackerMap.clear();
            
            lastProcessTimeMap = truckLastProcessTimeMap;
            cardProcessCountMap = truckCardProcessCountMap;
            cardErrorCountMap = truckCardErrorCountMap;
            cardTrackerMap = truckCardTrackerMap;
            
            // 设置时间范围
            this.truckStartTime = startTimeStr;
            this.truckEndTime = endTimeStr;
        }

        System.out.println("========================================");
        System.out.println("启动" + businessType + "实时流式数据处理任务");
        System.out.println("总卡数: " + cards.size());
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("数据获取间隔: 10秒");
        System.out.println("开始时间: " + startTimeStr);
        System.out.println("结束时间: " + endTimeStr);
        System.out.println("========================================");

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

        // 获取结束时间字符串（根据车辆类型）
        final String endTimeForCheck = (vt == RealTimeDriverTracker.VehicleType.CAR) ? this.carEndTime : this.truckEndTime;
        
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
            executorService.scheduleAtFixedRate(() -> {
                // 检查是否超过结束时间
                LocalDateTime currentLastTime = lastProcessTimeMap.get(cardId);
                if (currentLastTime.isAfter(DateTimeUtils.str2DateTime(endTimeForCheck))) {
                    System.out.println("✓ 卡 " + cardId + " 已处理到结束时间，停止处理");

                    // 检查是否所有卡都已完成
                    boolean allCompleted = true;
                    for (String id : cards) {
                        if (!lastProcessTimeMap.get(id).isAfter(DateTimeUtils.str2DateTime(endTimeForCheck))) {
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
                                // 根据车辆类型停止对应的任务
                                if (vt == RealTimeDriverTracker.VehicleType.CAR) {
                                    realDriverTrackerZQRealtimeStopCar();
                                } else {
                                    realDriverTrackerZQRealtimeStopTruck();
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                    return;
                }

                // 检查任务是否已停止
                boolean isRunning = (vt == RealTimeDriverTracker.VehicleType.CAR) ? isCarTaskRunning : isTruckTaskRunning;
                if (!isRunning) {
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
        executorService.scheduleAtFixedRate(() -> {
            boolean isRunning = (vt == RealTimeDriverTracker.VehicleType.CAR) ? isCarTaskRunning : isTruckTaskRunning;
            if (isRunning) {
                printRealtimeStatus(vt, businessType);
            }
        }, 60, 60, TimeUnit.SECONDS);
        System.out.println("✓ " + businessType + "任务已启动！使用 realDriverTrackerZQRealtimeStop() 停止所有任务");
    }








        /**
         * 测试入口
         * 自定义时间范围的实时流式数据处理测试方法（火车）（持续运行，每10秒获取一次数据）
         * 支持自定义开始结束时间，用于测试场景
         *
         * 使用方法：
         * 1. 启动：realDriverTrackerZQRealtimeWithNewTime(cards, "2025-10-17 16:59:00", "2025-10-17 19:27:30")
         * 2. 停止：realDriverTrackerZQRealtimeStop()
         * 3. 查看状态：realDriverTrackerZQRealtimeStatus()
         */
    public void realDriverTrackerZQRealtimeWithNowTime() {
//        List<String> cards = new ArrayList<>(
//                Arrays.asList(
//                        "1918B3000561",
//                        "1918B3000978")
//        );
        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.CAR;
//        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.TRUCK;
//        String startTimeStr = "2025-10-23 18:04:00";
//        String endTimeStr = "2025-10-23 18:19:00";
        String startTimeStr = "2025-10-31 20:00:00";
        String endTimeStr = "2025-10-31 20:10:00";
        List<String> cards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000561")
        );
        if (isCarTaskRunning) {
            System.out.println("火车装卸测试任务已在运行中，无需重复启动");
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
        carExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
        isCarTaskRunning = true;

        // 初始化统计信息
        carLastProcessTimeMap.clear();
        carCardProcessCountMap.clear();
        carCardErrorCountMap.clear();
        carCardTrackerMap.clear();

        // 设置全局时间范围（用于判断是否到达结束时间）
        this.carStartTime = startTimeStr;
        this.carEndTime = endTimeStr;

        System.out.println("========================================");
        System.out.println("启动自定义时间范围的实时流式数据处理测试");
        System.out.println("总卡数: " + cards.size());
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("数据获取间隔: 10秒");
        System.out.println("开始时间: " + startTimeStr);
        System.out.println("结束时间: " + endTimeStr);
        System.out.println("========================================");

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

        // 为每个卡创建定时任务，每10秒执行一次，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            // 初始化统计
            carCardProcessCountMap.put(cardId, new AtomicInteger(0));
            carCardErrorCountMap.put(cardId, new AtomicInteger(0));
            // 设置每个卡的初始处理时间为用户指定的开始时间
            carLastProcessTimeMap.put(cardId, DateTimeUtils.str2DateTime(startTimeStr));

            // 为每个卡创建独立的 tracker 实例（在整个任务运行期间持续使用）
            RealTimeDriverTracker cardTracker = applicationContext.getBean(RealTimeDriverTracker.class);
            carCardTrackerMap.put(cardId, cardTracker);
            System.out.println("✓ 为卡 " + cardId + " 创建独立的 RealTimeDriverTracker 实例");

            // 错峰启动：第1个卡立即开始，后续每个卡延迟200ms
            final long initialDelay = delayIndex * 200;
            delayIndex++;

            // 延迟initialDelay毫秒开始，每10秒执行一次
            carExecutorService.scheduleAtFixedRate(() -> {
                // 检查是否超过结束时间
                LocalDateTime currentLastTime = carLastProcessTimeMap.get(cardId);
                if (currentLastTime.isAfter(DateTimeUtils.str2DateTime(this.carEndTime))) {
                    System.out.println("✓ 卡 " + cardId + " 已处理到结束时间，停止处理");

                    // 检查是否所有卡都已完成
                    boolean allCompleted = true;
                    for (String id : cards) {
                        if (!carLastProcessTimeMap.get(id).isAfter(DateTimeUtils.str2DateTime(this.carEndTime))) {
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
                                realDriverTrackerZQRealtimeStopCar();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                    return;
                }

                if (!isCarTaskRunning) {
                    return; // 如果任务已停止，不再处理
                }

                try {
                    processCardDataRealtimeTestWithRetry(cardId, vt, 2); // 最多重试2次
                    carCardProcessCountMap.get(cardId).incrementAndGet();
                } catch (Exception e) {
                    carCardErrorCountMap.get(cardId).incrementAndGet();
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                    System.err.println("[" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) +
                            "] 处理卡 " + cardId + " 失败: " + errorMsg);
                }
            }, initialDelay, 10000, TimeUnit.MILLISECONDS);
        }

        // 启动监控线程，每分钟输出一次统计信息
        carExecutorService.scheduleAtFixedRate(() -> {
            if (isCarTaskRunning) {
                printRealtimeStatus(RealTimeDriverTracker.VehicleType.CAR, "火车装卸");
            }
        }, 60, 60, TimeUnit.SECONDS);

        System.out.println("✓ 测试任务已启动！使用 realDriverTrackerZQRealtimeStopCar() 停止火车任务");
    }

    /**
     * 自定义时间范围的实时流式数据处理测试方法（板车）（持续运行，每10秒获取一次数据）
     * 支持自定义开始结束时间，用于测试场景
     *
     * 使用方法：
     * 1. 启动：realDriverTrackerZQRealtimeWithNewTime(cards, "2025-10-17 16:59:00", "2025-10-17 19:27:30")
     * 2. 停止：realDriverTrackerZQRealtimeStop()
     * 3. 查看状态：realDriverTrackerZQRealtimeStatus()
     */
    public void realDriverTrackerZQRealtimeTruckWithNowTime() {
//        List<String> cards = new ArrayList<>(
//                Arrays.asList(
//                        "1918B3000561",
//                        "1918B3000978")
//        );
////        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.CAR;
        RealTimeDriverTracker.VehicleType vt = RealTimeDriverTracker.VehicleType.TRUCK;
//        String startTimeStr = "2025-10-23 18:04:00";
//        String endTimeStr = "2025-10-23 18:19:00";
        String startTimeStr = "2025-10-31 20:00:00";
        String endTimeStr = "2025-10-31 20:10:00";
        List<String> cards = new ArrayList<>(
                Arrays.asList(
                        "1918B3000561")
        );
        if (isTruckTaskRunning) {
            System.out.println("板车装卸测试任务已在运行中，无需重复启动");
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
        truckExecutorService = Executors.newScheduledThreadPool(threadPoolSize);
        isTruckTaskRunning = true;

        // 初始化统计信息
        truckLastProcessTimeMap.clear();
        truckCardProcessCountMap.clear();
        truckCardErrorCountMap.clear();
        truckCardTrackerMap.clear();

        // 设置全局时间范围（用于判断是否到达结束时间）
        this.truckStartTime = startTimeStr;
        this.truckEndTime = endTimeStr;

        System.out.println("========================================");
        System.out.println("启动自定义时间范围的实时流式数据处理测试");
        System.out.println("总卡数: " + cards.size());
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("数据获取间隔: 10秒");
        System.out.println("开始时间: " + startTimeStr);
        System.out.println("结束时间: " + endTimeStr);
        System.out.println("========================================");

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

        // 为每个卡创建定时任务，每10秒执行一次，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            // 初始化统计
            truckCardProcessCountMap.put(cardId, new AtomicInteger(0));
            truckCardErrorCountMap.put(cardId, new AtomicInteger(0));
            // 设置每个卡的初始处理时间为用户指定的开始时间
            truckLastProcessTimeMap.put(cardId, DateTimeUtils.str2DateTime(startTimeStr));

            // 为每个卡创建独立的 tracker 实例（在整个任务运行期间持续使用）
            RealTimeDriverTracker cardTracker = applicationContext.getBean(RealTimeDriverTracker.class);
            truckCardTrackerMap.put(cardId, cardTracker);
            System.out.println("✓ 为卡 " + cardId + " 创建独立的 RealTimeDriverTracker 实例");

            // 错峰启动：第1个卡立即开始，后续每个卡延迟200ms
            final long initialDelay = delayIndex * 200;
            delayIndex++;

            // 延迟initialDelay毫秒开始，每10秒执行一次
            truckExecutorService.scheduleAtFixedRate(() -> {
                // 检查是否超过结束时间
                LocalDateTime currentLastTime = truckLastProcessTimeMap.get(cardId);
                if (currentLastTime.isAfter(DateTimeUtils.str2DateTime(this.truckEndTime))) {
                    System.out.println("✓ 卡 " + cardId + " 已处理到结束时间，停止处理");

                    // 检查是否所有卡都已完成
                    boolean allCompleted = true;
                    for (String id : cards) {
                        if (!truckLastProcessTimeMap.get(id).isAfter(DateTimeUtils.str2DateTime(this.truckEndTime))) {
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
                                realDriverTrackerZQRealtimeStopTruck();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                    return;
                }

                if (!isTruckTaskRunning) {
                    return; // 如果任务已停止，不再处理
                }

                try {
                    processCardDataRealtimeTestWithRetry(cardId, vt, 2); // 最多重试2次
                    truckCardProcessCountMap.get(cardId).incrementAndGet();
                } catch (Exception e) {
                    truckCardErrorCountMap.get(cardId).incrementAndGet();
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                    System.err.println("[" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) +
                            "] 处理卡 " + cardId + " 失败: " + errorMsg);
                }
            }, initialDelay, 10000, TimeUnit.MILLISECONDS);
        }

        // 启动监控线程，每分钟输出一次统计信息
        truckExecutorService.scheduleAtFixedRate(() -> {
            if (isTruckTaskRunning) {
                printRealtimeStatus(RealTimeDriverTracker.VehicleType.TRUCK, "板车装卸");
            }
        }, 60, 60, TimeUnit.SECONDS);

        System.out.println("✓ 测试任务已启动！使用 realDriverTrackerZQRealtimeStopTruck() 停止板车任务");
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
        // 根据车辆类型选择对应的 map
        ConcurrentHashMap<String, LocalDateTime> lastProcessTimeMap;
        ConcurrentHashMap<String, RealTimeDriverTracker> cardTrackerMap;
        
        if (vt == RealTimeDriverTracker.VehicleType.CAR) {
            lastProcessTimeMap = carLastProcessTimeMap;
            cardTrackerMap = carCardTrackerMap;
        } else {
            lastProcessTimeMap = truckLastProcessTimeMap;
            cardTrackerMap = truckCardTrackerMap;
        }
        
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

//        // 获取位置点数据 - 添加空值检查
//        String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
//        if (pointsResponse == null) {
//            throw new RuntimeException("获取位置点数据返回null");
//        }
//        JSONObject jsonObject = JSONObject.parseObject(pointsResponse);
//
//        String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId,
//                DateTimeUtils.localDateTime2String(lastTime.minusSeconds(2)),
//                DateTimeUtils.localDateTime2String(currentTime.plusSeconds(2)));
//        if (tagResponse == null) {
//            throw new RuntimeException("获取标签状态数据返回null");
//        }
//        JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);
//
//        JSONArray points = jsonObject.getJSONArray("data");
//        JSONArray tagData = tagJsonObject.getJSONArray("data");
//
//        if (points.size() == 0 || tagData.size() == 0){
//            log.info("标签：[{}] , {} - {} 未获取到数据，跳过处理", cardId, startTimeStr, endTimeStr);
//            lastProcessTimeMap.put(cardId, currentTime);
//            return;
//        }
//        List<LocationPoint> LocationPoints = new ArrayList<>();
//        if (points != null && !points.isEmpty()) {
//            for (int i = 0; i < points.size(); i++){
//                JSONObject js = (JSONObject) points.get(i);
//                JSONArray plist = js.getJSONArray("points");
//                if (plist != null) {
//                    for (int j = 0; j < plist.size(); j++){
//                        LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
//                        LocationPoint point1 = new LocationPoint(
//                                cardId,
//                                point.getLongitude(),
//                                point.getLatitude(),
//                                DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
//                                Long.parseLong(point.getTime()));
//                        LocationPoints.add(point1);
//                    }
//                }
//            }
//        }
        DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
        List<LocationPoint> allPoints1 = dataAcquisition.getLocationAndUWBData(cardId, buildId, startTimeStr, endTimeStr);
        List<LocationPoint> allPoints = GeoUtils.processMultiplePointsPerSecondByUwb(allPoints1);
        if (allPoints.isEmpty()) {
            System.out.println("⚠️ 线程 " + Thread.currentThread().getName() +
                    " 处理卡 " + cardId + " 解析后数据为空");
            return;
        }
        // 按时间戳排序
        allPoints.sort((p1, p2) -> Long.compare(p1.getTimestamp(), p2.getTimestamp()));
        // 融合位置数据和标签数据
        if (!allPoints.isEmpty()) {
//            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints, tagData);

            if (allPoints.size() > 0){
                // 使用该卡独立的 tracker 实例处理数据（维持状态连续性）
                cardTracker.replayHistorical(allPoints, vt);
                System.out.println("[" + DateTimeUtils.localDateTime2String(currentTime) +
                        "] ✓ 卡 " + cardId + " 处理了 " + allPoints.size() + " 个点 (时间窗口: " +
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
     * 停止所有实时流式数据处理任务（火车和板车）
     */
    public void realDriverTrackerZQRealtimeStop() {
        boolean hasRunningTask = isCarTaskRunning || isTruckTaskRunning;
        
        if (!hasRunningTask) {
            System.out.println("没有运行中的实时任务");
            return;
        }

        System.out.println("========================================");
        System.out.println("正在停止所有实时任务...");
        
        // 停止火车任务
        if (isCarTaskRunning) {
            System.out.println("正在停止火车装卸任务...");
            stopTask(RealTimeDriverTracker.VehicleType.CAR);
        }
        
        // 停止板车任务
        if (isTruckTaskRunning) {
            System.out.println("正在停止板车装卸任务...");
            stopTask(RealTimeDriverTracker.VehicleType.TRUCK);
        }

        System.out.println("停止时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("✓ 所有实时任务已停止");
        System.out.println("========================================");
    }

    /**
     * 仅停止火车装卸实时任务
     */
    public void realDriverTrackerZQRealtimeStopCar() {
        if (!isCarTaskRunning) {
            System.out.println("火车装卸任务未运行");
            return;
        }

        System.out.println("========================================");
        System.out.println("正在停止火车装卸任务...");
        stopTask(RealTimeDriverTracker.VehicleType.CAR);
        System.out.println("停止时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("✓ 火车装卸任务已停止");
        System.out.println("========================================");
    }

    /**
     * 仅停止板车装卸实时任务
     */
    public void realDriverTrackerZQRealtimeStopTruck() {
        if (!isTruckTaskRunning) {
            System.out.println("板车装卸任务未运行");
            return;
        }

        System.out.println("========================================");
        System.out.println("正在停止板车装卸任务...");
        stopTask(RealTimeDriverTracker.VehicleType.TRUCK);
        System.out.println("停止时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("✓ 板车装卸任务已停止");
        System.out.println("========================================");
    }

    /**
     * 停止指定类型的任务（内部方法）
     */
    private void stopTask(RealTimeDriverTracker.VehicleType vt) {
        ScheduledExecutorService executorService;
        ConcurrentHashMap<String, RealTimeDriverTracker> cardTrackerMap;
        String taskType;
        
        if (vt == RealTimeDriverTracker.VehicleType.CAR) {
            executorService = carExecutorService;
            cardTrackerMap = carCardTrackerMap;
            taskType = "火车装卸";
            isCarTaskRunning = false;
        } else {
            executorService = truckExecutorService;
            cardTrackerMap = truckCardTrackerMap;
            taskType = "板车装卸";
            isTruckTaskRunning = false;
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 输出最终统计
        printRealtimeStatus(vt, taskType);

        // 清理 tracker 实例
        if (!cardTrackerMap.isEmpty()) {
            System.out.println("清理 " + cardTrackerMap.size() + " 个 " + taskType + " tracker 实例...");
            cardTrackerMap.clear();
        }
    }

    /**
     * 查看所有实时任务状态
     */
    public void realDriverTrackerZQRealtimeStatus() {
        System.out.println("========================================");
        System.out.println("实时任务状态总览");
        System.out.println("当前时间: " + DateTimeUtils.localDateTime2String(LocalDateTime.now()));
        System.out.println("----------------------------------------");
        System.out.println("火车装卸任务: " + (isCarTaskRunning ? "运行中" : "已停止"));
        System.out.println("板车装卸任务: " + (isTruckTaskRunning ? "运行中" : "已停止"));
        System.out.println("========================================");

        if (isCarTaskRunning) {
            System.out.println("\n【火车装卸任务详情】");
            printRealtimeStatus(RealTimeDriverTracker.VehicleType.CAR, "火车装卸");
        }
        
        if (isTruckTaskRunning) {
            System.out.println("\n【板车装卸任务详情】");
            printRealtimeStatus(RealTimeDriverTracker.VehicleType.TRUCK, "板车装卸");
        }
        
        if (!isCarTaskRunning && !isTruckTaskRunning) {
            System.out.println("当前没有运行中的任务");
        }
    }

    /**
     * 打印实时统计信息（根据任务类型）
     */
    private void printRealtimeStatus(RealTimeDriverTracker.VehicleType vt, String taskType) {
        ConcurrentHashMap<String, AtomicInteger> cardProcessCountMap;
        ConcurrentHashMap<String, AtomicInteger> cardErrorCountMap;
        ConcurrentHashMap<String, LocalDateTime> lastProcessTimeMap;
        
        if (vt == RealTimeDriverTracker.VehicleType.CAR) {
            cardProcessCountMap = carCardProcessCountMap;
            cardErrorCountMap = carCardErrorCountMap;
            lastProcessTimeMap = carLastProcessTimeMap;
        } else {
            cardProcessCountMap = truckCardProcessCountMap;
            cardErrorCountMap = truckCardErrorCountMap;
            lastProcessTimeMap = truckLastProcessTimeMap;
        }
        
        System.out.println("\n--- " + taskType + "实时处理统计 [" + DateTimeUtils.localDateTime2String(LocalDateTime.now()) + "] ---");

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
}
