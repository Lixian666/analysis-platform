package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.vo.VisionLocationMatchResult;
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.VisionLocationMatcher;
import com.jwzt.modules.experiment.utils.third.manage.JobData;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
            Date startTime = new Date(now.getTime() - 60 * 1000L); // 当前时间前1分钟
            String startTimeStr = sdf.format(startTime);
            String endTimeStr = sdf.format(now);
            if (startStr != null && !startStr.isEmpty() && endStr != null && !endStr.isEmpty()) {
                startTimeStr = startStr;
                endTimeStr = endStr;
                log.info("指定查询时间范围：{} - {}", startTimeStr, endTimeStr);
            } else {
                log.info("查询时间范围：{} - {}", startTimeStr, endTimeStr);
            }
            final String startTimeStrFinal = startTimeStr;
            final String endTimeStrFinal = endTimeStr;
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
                        List<LocationPoint> locationPointList = dataAcquisition.getLocationAndUWBData(
                                cardId, buildId, startTimeStrFinal, endTimeStrFinal);
                        List<LocationPoint> locationPoints = new ArrayList<>();
                        for (LocationPoint currentPoint : locationPointList){
                            // 判断是否靠近交通车附近
                            boolean isTrafficCarWithin = tagBeacon.theTagIsCloseToTheBeacon(
                                    currentPoint,
                                    baseConfig.getJoysuch().getBuildingId(),
                                    "交通车",
                                    null,
                                    null,
                                    null);
                            if (!isTrafficCarWithin) {
                                locationPoints.add(currentPoint);
                            }
                        }
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
                    .filter(event -> "load".equals(event.getEventType()))
                    .collect(Collectors.toList());
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

            
        } catch (Exception e) {
            log.error("视觉识别与定位数据匹配任务执行异常", e);
        }
    }
}

