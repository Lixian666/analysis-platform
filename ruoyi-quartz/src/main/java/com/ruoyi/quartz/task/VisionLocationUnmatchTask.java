package com.ruoyi.quartz.task;

import com.alibaba.fastjson.JSONObject;
import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.*;
import com.jwzt.modules.experiment.domain.vo.VisionLocationMatchResult;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.strategy.LoadingStrategyFactory;
import com.jwzt.modules.experiment.strategy.LoadingUnloadingStrategy;
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.VisionLocationMatcher;
import com.jwzt.modules.experiment.utils.third.manage.DataSender;
import com.jwzt.modules.experiment.utils.third.manage.JobData;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.vo.EventState;
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

import static com.jwzt.modules.experiment.domain.MovementAnalyzer.observeState;

/**
 * 视觉识别与定位数据匹配定时任务
 * 每分钟执行一次，将视觉识别数据与定位卡数据进行匹配
 * 
 * @author lx
 * @date 2025-01-20
 */
@Component("VisionLocationUnmatchTask")
public class VisionLocationUnmatchTask {
    
    private static final Logger log = LoggerFactory.getLogger(VisionLocationUnmatchTask.class);

    @Autowired
    private BaseConfig baseConfig;
    
    @Autowired
    private JobData jobData;

    @Autowired
    private DataSender dataSender;
    
    @Resource
    private ITakBehaviorRecordsService iTakBehaviorRecordsService;
    
    /**
     * 执行匹配任务
     * 每分钟执行一次
     */
    public void executeTask(String startStr, String endStr) {
        try {
            log.info("========== 视觉识别与定位数据匹配任务开始 ==========");
            
            // 1. 获取当前时间范围（最近1分钟）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            Date startTime = new Date(now.getTime() - 1 * 60 * 1000L); // 当前时间前1分钟
            Date endTime = new Date(now.getTime()); // 当前时间前1分钟
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
            
            // 2. 获取buildId
            String buildId = baseConfig.getJoysuch().getBuildingId();
            if (buildId == null || buildId.isEmpty()) {
                log.error("buildId为空，无法获取定位数据");
                return;
            }
            
            // 3. 获取cameraIds、eventTypes
            List<String> cameraIds = baseConfig.getCardAnalysis().getVisualIdentify().getCameraIds();
            if (cameraIds == null || cameraIds.isEmpty()) {
                log.warn("cameraIds为空，无法获取视觉识别数据");
                return;
            }
            List<String> unmatchEventTypes = baseConfig.getCardAnalysis().getVisualIdentify().getUnmatchEventTypes();
            if (unmatchEventTypes == null || unmatchEventTypes.isEmpty()) {
                log.warn("unmatchEventTypes为空，无法获取视觉识别数据");
                return;
            }
            log.info("获取到摄像机ID列表，共 {} 个摄像机", cameraIds.size());
            
            // 6. 获取新的视觉识别数据
            List<VisionEvent> newVisionEventList = jobData.getVisionList(startTimeStrFinal, endTimeStrFinal, cameraIds);
            // debug使用json文件
//            List<VisionEvent> newVisionEventList = jobData.getVisionList4json(startTimeStrFinal, endTimeStrFinal, cameraIds);

            // 过滤出装卸车数据
            // 火车：load/unload
            // 地跑：gateCommodityVehicleInput/gateCommodityVehicleOutput
            // 板车：gateBancheInput/gateBancheOutput
            List<VisionEvent> newVisionEvents = newVisionEventList.stream()
                    .filter(event -> {
                        String eventType = event.getEventType();
                        return eventType != null && unmatchEventTypes.contains(eventType);
                    })
                    .collect(Collectors.toList());
            // 将新视觉事件的 matched 字段全部置为 1
            if (newVisionEvents == null && newVisionEvents.isEmpty()) {
                log.warn("newVisionEvents为空，无法获取视觉识别数据");
                return;
            }
            log.info("获取到新的视觉识别数据，共 {} 条", newVisionEvents != null ? newVisionEvents.size() : 0);
            processCardUnmatchedPoints(newVisionEvents);


        } catch (Exception e) {
            log.error("视觉识别与定位数据匹配任务执行异常", e);
        }
    }
    
    /**
     * 处理单个卡的匹配数据
     * 对每个 MatchedLocationPoint，查找其对应的上车点并入库
     */
    private void processCardUnmatchedPoints(List<VisionEvent> matchedPoints) {
        if (matchedPoints == null || matchedPoints.isEmpty()) {
            return;
        }
        // 遍历每个匹配点，查找对应的上车点
        for (int i = 0; i < matchedPoints.size(); i++) {
            VisionEvent visionEvent = matchedPoints.get(i);
            if (visionEvent == null || visionEvent.getEventType() == null) {
                log.warn("ID: {} 的第 {} 个匹配数据无效，跳过", visionEvent.getId(), i + 1);
                continue;
            }
            
            try {
                // 根据 event 字段判断车辆类型和装卸类型
                String eventType = visionEvent.getEventType();
                LoadingStrategyFactory.VehicleType vehicleType;
                int loadUnloadType; // 0 装车 1 卸车
                
                if ("load".equals(eventType)) {
                    // 火车装车
                    vehicleType = LoadingStrategyFactory.VehicleType.TRAIN;
                    loadUnloadType = 0;
                } else if ("unload".equals(eventType)) {
                    // 火车卸车
                    vehicleType = LoadingStrategyFactory.VehicleType.TRAIN;
                    loadUnloadType = 1;
                } else if ("gateCommodityVehicleInput".equals(eventType)) {
                    // 地跑进（卸车）
                    vehicleType = LoadingStrategyFactory.VehicleType.GROUND_VEHICLE;
                    loadUnloadType = 1;
                } else if ("gateCommodityVehicleOutput".equals(eventType)) {
                    // 地跑出（装车）
                    vehicleType = LoadingStrategyFactory.VehicleType.GROUND_VEHICLE;
                    loadUnloadType = 0;
                } else if ("gateBancheInput".equals(eventType)) {
                    // 板车进（卸车）
                    vehicleType = LoadingStrategyFactory.VehicleType.FLATBED;
                    loadUnloadType = 1;
                } else if ("gateBancheOutput".equals(eventType)) {
                    // 板车出（装车）
                    vehicleType = LoadingStrategyFactory.VehicleType.FLATBED;
                    loadUnloadType = 0;
                } else {
                    log.warn("ID: {} 的第 {} 个匹配，未知的事件类型: {}，跳过", visionEvent.getId(), i + 1, eventType);
                    continue;
                }
                // 生成统一的 trackId
                String trackId = IdUtils.fastSimpleUUID();

                // 入库
                int inboundStatus = persistSession("1", vehicleType, loadUnloadType, visionEvent, trackId);
                // 推送数据
                Long eventTime = DateTimeUtils.convertToTimestamp(visionEvent.getEventTime());
                RealTimeDriverTracker.TrackSession sess = new RealTimeDriverTracker.TrackSession();
                sess.sessionId = trackId;
                sess.cardId = "1";
                sess.startTime = eventTime;
                sess.endTime = eventTime;
                sess.vin = visionEvent.getVin();
                if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED){
                    sess.plateNum = visionEvent.getCarNumber();
                }
                int pushStatus = 1;
                RealTimeDriverTracker.VehicleType pushVehicleType = convertToPushVehicleType(vehicleType);
                
                if (loadUnloadType == 0){
                    // 装车（发运）
                    if (vehicleType == LoadingStrategyFactory.VehicleType.TRAIN){
                        sess.kind = RealTimeDriverTracker.EventKind.SEND;
                    }else if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED){
                        sess.kind = RealTimeDriverTracker.EventKind.TRUCK_SEND;
                    }else if (vehicleType == LoadingStrategyFactory.VehicleType.GROUND_VEHICLE){
                        sess.kind = RealTimeDriverTracker.EventKind.CAR_SEND;
                    }
                    if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED){
                        if (visionEvent.getCommodityVehicleCount() > 0){
                            for (int j = 0; j < visionEvent.getCommodityVehicleCount(); j++) {
                                sess.sessionId = trackId;
                                JSONObject outParkPushResult = dataSender.outParkPush(sess, pushVehicleType);
                                JSONObject outYardPushResult = dataSender.outYardPush(sess, pushVehicleType);
                                trackId = IdUtils.fastSimpleUUID();
                                if (outParkPushResult.getIntValue("code") == 20000 && outYardPushResult.getIntValue("code") == 20000){
                                    pushStatus = 0;
                                }
                            }
                        }else {
                            JSONObject outParkPushResult = dataSender.outParkPush(sess, pushVehicleType);
                            JSONObject outYardPushResult = dataSender.outYardPush(sess, pushVehicleType);
                            if (outParkPushResult.getIntValue("code") == 20000 && outYardPushResult.getIntValue("code") == 20000){
                                pushStatus = 0;
                            }
                        }
                    }else {
                        JSONObject outParkPushResult = dataSender.outParkPush(sess, pushVehicleType);
                        JSONObject outYardPushResult = dataSender.outYardPush(sess, pushVehicleType);
                        if (outParkPushResult.getIntValue("code") == 20000 && outYardPushResult.getIntValue("code") == 20000){
                            pushStatus = 0;
                        }
                    }
                } else {
                    // 卸车（到达）
                    if (vehicleType == LoadingStrategyFactory.VehicleType.TRAIN){
                        sess.kind = RealTimeDriverTracker.EventKind.ARRIVED;;
                    }else if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED){
                        sess.kind = RealTimeDriverTracker.EventKind.TRUCK_ARRIVED;
                    }else if (vehicleType == LoadingStrategyFactory.VehicleType.GROUND_VEHICLE){
                        sess.kind = RealTimeDriverTracker.EventKind.CAR_ARRIVED;
                    }
                    if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED){
                        if (visionEvent.getCommodityVehicleCount() > 0){
                            for (int j = 0; j < visionEvent.getCommodityVehicleCount(); j++) {
                                sess.sessionId = trackId;
                                JSONObject inYardPushResult = dataSender.inYardPush(sess, pushVehicleType);
                                JSONObject inParkPushResult = dataSender.inParkPush(sess, pushVehicleType);
                                trackId = IdUtils.fastSimpleUUID();
                                if (inYardPushResult.getIntValue("code") == 20000 && inParkPushResult.getIntValue("code") == 20000){
                                    pushStatus = 0;
                                }
                            }
                        }else {
                            JSONObject inYardPushResult = dataSender.inYardPush(sess, pushVehicleType);
                            JSONObject inParkPushResult = dataSender.inParkPush(sess, pushVehicleType);
                            if (inYardPushResult.getIntValue("code") == 20000 && inParkPushResult.getIntValue("code") == 20000){
                                pushStatus = 0;
                            }
                        }
                    }else {
                        JSONObject inYardPushResult = dataSender.inYardPush(sess, pushVehicleType);
                        JSONObject inParkPushResult = dataSender.inParkPush(sess, pushVehicleType);
                        if (inYardPushResult.getIntValue("code") == 20000 && inParkPushResult.getIntValue("code") == 20000){
                            pushStatus = 0;
                        }
                    }

                }

                // 更新推送状态
                if (pushStatus == 0){
                    iTakBehaviorRecordsService.updatePushStatus(trackId, 0);
                }

                log.info("ID: {} 的第 {} 个匹配点处理完成，车辆类型: {}, 装卸类型: {}, 上车点时间: {}, 下车点时间: {}, 轨迹点数: 0",
                        visionEvent.getId(), i + 1, vehicleType,
                        loadUnloadType == 0 ? "装车" : "卸车",
                        eventTime,
                        eventTime);
                
            } catch (Exception e) {
                log.error("处理视觉ID: {} 的第 {} 个匹配点时发生异常", visionEvent.getId(), i + 1, e);
            }
        }
    }

    /**
     * 转换为推送接口使用的车辆类型
     */
    private RealTimeDriverTracker.VehicleType convertToPushVehicleType(LoadingStrategyFactory.VehicleType vehicleType) {
        if (vehicleType == LoadingStrategyFactory.VehicleType.TRAIN) {
            return RealTimeDriverTracker.VehicleType.CAR;
        } else if (vehicleType == LoadingStrategyFactory.VehicleType.GROUND_VEHICLE) {
            return RealTimeDriverTracker.VehicleType.CAR;
        } else if (vehicleType == LoadingStrategyFactory.VehicleType.FLATBED) {
            return RealTimeDriverTracker.VehicleType.TRUCK;
        }
        return RealTimeDriverTracker.VehicleType.CAR;
    }
    
    /**
     * 入库会话数据
     */
    private int persistSession(String cardId,
                                LoadingStrategyFactory.VehicleType vehicleType,
                                int eventType,
                                VisionEvent visionEvent,
                                String trackId) {
        
        try {
            // 1) 主表
            TakBehaviorRecords rec = new TakBehaviorRecords();
            rec.setCardId(cardId);
            rec.setYardId(baseConfig.getYardName());
            rec.setTrackId(trackId);
            if (eventType == 0) {
                rec.setStartTime(new Date(DateTimeUtils.convertToTimestamp(visionEvent.getEventTime())));
                rec.setEndTime(new Date(DateTimeUtils.convertToTimestamp(visionEvent.getEventTime())));
            } else if (eventType == 1){
                rec.setStartTime(new Date(DateTimeUtils.convertToTimestamp(visionEvent.getEventTime())));
                rec.setEndTime(new Date(DateTimeUtils.convertToTimestamp(visionEvent.getEventTime())));
            }
            rec.setPointCount(0L);
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
            
            rec.setDuration("0");
            rec.setState("完成");
            
            // 3) 入库
            iTakBehaviorRecordsService.insertTakBehaviorRecords(rec);
            
            log.info("ID: {} 的会话数据入库成功，trackId: {}, 点数: {}", visionEvent.getId(), trackId, 0);
            return 0;
        } catch (Exception e) {
            log.error("ID: {} 的会话数据入库失败", visionEvent.getId(), e);
            return 2;
        }
    }
}

