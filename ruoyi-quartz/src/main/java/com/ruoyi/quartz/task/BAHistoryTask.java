package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 定时任务调度测试
 *
 * @author ruoyi
 */
@Component("BAHistoryTask")
public class BAHistoryTask
{
    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private ApplicationContext applicationContext;  // 用于获取 prototype bean

    /**
     * 实时任务测试（历史数据模拟）单线程
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
            String startTimeStr = "2025-10-16 18:25:00";
            String endTimeStr = "2025-10-16 19:40:00";
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
                tracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.TRUCK);
            }
        } finally {
            // 不需要手动清理，tracker 实例会被 GC 回收
            System.out.println("✅ 批处理任务完成");
        }
    }

    /**
     * 实时任务测试（历史数据模拟）多线程 - 支持设置车辆类型
     * 支持处理大量卡数据（几百甚至上千），自动根据可用资源调整并发数
     * 模拟实时处理：每次处理10秒的数据
     */
    public void realDriverTrackerZQTestMultitaskingWithVehicleType(){
        DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
        List<String> cards = dataAcquisition.getCardIdList(1);

        String startTimeStr = "2025-11-19 09:50:00";
        String endTimeStr = "2025-11-19 11:20:00";
        Map<String, RealTimeDriverTracker.VehicleType> vehicleTypeMap = null;
        // 如果没有提供vehicleTypeMap，创建空Map
        if (vehicleTypeMap == null) {
            vehicleTypeMap = new HashMap<>();
        }
        
        // 默认车辆类型：CAR（火车装卸）
        final RealTimeDriverTracker.VehicleType defaultVehicleType = RealTimeDriverTracker.VehicleType.CAR;
//        final RealTimeDriverTracker.VehicleType defaultVehicleType = RealTimeDriverTracker.VehicleType.TRUCK;

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
        System.out.println("开始多线程处理卡数据（支持车辆类型设置）");
        System.out.println("总卡数: " + cards.size());
        System.out.println("CPU核心数: " + availableProcessors);
        System.out.println("线程池大小: " + threadPoolSize);
        System.out.println("预计批次: " + (int)Math.ceil((double)cards.size() / threadPoolSize));
        System.out.println("提示: 使用错峰启动避免并发token冲突");
        // 输出车辆类型配置信息
        System.out.println("车辆类型配置:");
        for (String cardId : cards) {
            RealTimeDriverTracker.VehicleType vt = vehicleTypeMap.getOrDefault(cardId, defaultVehicleType);
            System.out.println("  - 卡 " + cardId + ": " + (vt == RealTimeDriverTracker.VehicleType.TRUCK ? "板车(TRUCK)" : "火车/地跑(CAR)"));
        }
        System.out.println("========================================");

        long startTime = System.currentTimeMillis();

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

        // 为每个卡创建一个处理任务，使用错峰启动
        int delayIndex = 0;
        for (String cardId : cards) {
            final int startDelay = delayIndex * 100; // 每个任务延迟100ms启动
            final String finalCardId = cardId; // 用于lambda表达式的final变量
            // 获取该卡的车辆类型，如果未指定则使用默认类型
            final RealTimeDriverTracker.VehicleType vehicleType = vehicleTypeMap.getOrDefault(cardId, defaultVehicleType);
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
                    // 为当前卡设置车辆类型
                    tracker.upsertVehicleType(finalCardId, vehicleType);
                    processCardDataWithRetry(finalCardId, startTimeStr, endTimeStr, 3, tracker, vehicleType);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    System.err.println("处理卡 " + finalCardId + " 时发生异常: " + errorMsg);
                    e.printStackTrace();
                    errorMap.put(finalCardId, e);
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
     * 带重试机制的卡数据处理（解决并发token冲突问题）
     */
    private void processCardDataWithRetry(String cardId, String startTimeStr, String endTimeStr, int maxRetries, RealTimeDriverTracker tracker) throws Exception {
        processCardDataWithRetry(cardId, startTimeStr, endTimeStr, maxRetries, tracker, RealTimeDriverTracker.VehicleType.CAR);
    }

    /**
     * 带重试机制的卡数据处理（解决并发token冲突问题）- 支持车辆类型
     * 
     * @param cardId 卡号
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @param maxRetries 最大重试次数
     * @param tracker RealTimeDriverTracker实例
     * @param vehicleType 车辆类型
     */
    private void processCardDataWithRetry(String cardId, String startTimeStr, String endTimeStr, int maxRetries, RealTimeDriverTracker tracker, RealTimeDriverTracker.VehicleType vehicleType) throws Exception {
        Exception lastException = null;

        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                if (retry > 0) {
                    // 重试前等待，使用指数退避策略：1秒、2秒、4秒...
                    long waitTime = (long) Math.pow(2, retry - 1) * 1000;
                    System.out.println("卡 " + cardId + " 第 " + (retry + 1) + " 次尝试（等待" + waitTime + "ms后重试）");
                    Thread.sleep(waitTime);
                }

                processCardData(cardId, startTimeStr, endTimeStr, tracker, vehicleType);
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
        processCardData(cardId, startTimeStr, endTimeStr, tracker, RealTimeDriverTracker.VehicleType.CAR);
    }

    /**
     * 处理单个卡的数据（线程安全）- 用于历史数据批量处理，支持车辆类型
     * 模拟实时处理：每次处理10秒的数据，而不是一次性处理全部
     *
     * @param cardId 卡号
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @param tracker 该线程独立的 RealTimeDriverTracker 实例
     * @param vehicleType 车辆类型
     */
    private void processCardData(String cardId, String startTimeStr, String endTimeStr, RealTimeDriverTracker tracker, RealTimeDriverTracker.VehicleType vehicleType) {
        System.out.println("线程 " + Thread.currentThread().getName() + " 开始处理卡: " + cardId + " (一次查询，分批处理, 车辆类型: " + 
                (vehicleType == RealTimeDriverTracker.VehicleType.TRUCK ? "板车(TRUCK)" : "火车/地跑(CAR)") + ")");

        String buildId = baseConfig.getJoysuch().getBuildingId();
        String date = "未获取到日期";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
        
        // 确保车辆类型已设置
        tracker.upsertVehicleType(cardId, vehicleType);

        try {
            // 【优化】一次性获取整个时间段的所有数据
            DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
            List<LocationPoint> allPoints1 = dataAcquisition.getLocationAndUWBData(cardId, buildId, startTimeStr, endTimeStr);
            List<LocationPoint> allPoints = GeoUtils.processMultiplePointsPerSecondByUwb(allPoints1);
            List<LocationPoint> allPoints123 = GeoUtils.processMultiplePointsPerSecond(allPoints1);
            System.out.println("处理数据点数：" + allPoints.size());
//            String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
//            if (pointsResponse == null) {
//                System.err.println("⚠️ 线程 " + Thread.currentThread().getName() +
//                        " 处理卡 " + cardId + " 未获取到数据");
//                return;
//            }
//
//            JSONObject jsonObject = JSONObject.parseObject(pointsResponse);
//            String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId,
//                    DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)),
//                    DateTimeUtils.localDateTime2String(endTime.plusSeconds(2)));
//            if (tagResponse == null) {
//                throw new RuntimeException("获取标签状态数据返回null");
//            }
//            JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);
//
//            JSONArray points = jsonObject.getJSONArray("data");
//            JSONArray tagData = tagJsonObject.getJSONArray("data");
//            if (points == null || points.isEmpty()) {
//                System.out.println("⚠️ 线程 " + Thread.currentThread().getName() +
//                        " 处理卡 " + cardId + " 数据为空");
//                return;
//            }
//
//            // 解析所有位置点到内存
//            List<LocationPoint> allPoints = new ArrayList<>();
//            if (points != null && !points.isEmpty()) {
//                for (int i = 0; i < points.size(); i++){
//                    JSONObject js = (JSONObject) points.get(i);
//                    JSONArray plist = js.getJSONArray("points");
//                    if (plist != null) {
//                        for (int j = 0; j < plist.size(); j++){
//                            LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
//                            if (date.equals("未获取到日期")){
//                                date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
//                            }
//                            LocationPoint point1 = new LocationPoint(
//                                    cardId,
//                                    point.getLongitude(),
//                                    point.getLatitude(),
//                                    DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
//                                    Long.parseLong(point.getTime()));
//                            allPoints.add(point1);
//                        }
//                    }
//                }
//            }
//            // 融合位置数据和标签数据
//            if (!allPoints.isEmpty()) {
//                // 融合位置数据和标签数据
//                allPoints = FusionData.processesFusionLocationDataAndTagData(allPoints, tagData);
//            }
            if (allPoints.isEmpty()) {
                System.out.println("⚠️ 线程 " + Thread.currentThread().getName() +
                        " 处理卡 " + cardId + " 解析后数据为空");
                return;
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
}

