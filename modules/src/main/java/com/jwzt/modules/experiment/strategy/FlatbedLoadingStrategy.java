package com.jwzt.modules.experiment.strategy;

import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.MovementAnalyzer;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.vo.EventState;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.jwzt.modules.experiment.config.FilterConfig.ADJACENT_POINTS_TIME_INTERVAL_MS;

/**
 * 板车装卸策略实现
 * 使用 updateStateTruck 方法进行事件检测
 */
@Component
@Scope("prototype")  // 改为原型模式，每次获取新实例，避免多线程竞争
public class FlatbedLoadingStrategy implements LoadingUnloadingStrategy {
    
    @Autowired
    private BaseConfig baseConfig;
    
    @Autowired
    private ZoneChecker zoneChecker;
    
    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;
    
    @Autowired
    private OutlierFilter outlierFilter;
    
    private TheUWBRecordsTruck theUWBRecordsTruck = new TheUWBRecordsTruck();
    
    private BoardingDetector.Event lastEvent = BoardingDetector.Event.NONE;
    private BoardingDetector.Event currentEvent = BoardingDetector.Event.NONE;
    private LocationPoint curPoint = null;
    private EventState lastEventState = new EventState();
    private EventState sendOutLastEventState = null;
    private EventState sendInLastEventState = null;
    private EventState carSendOutLastEventState = null;
    private EventState carSendInLastEventState = null;
    private int parkingTags = 0;

    private int driverTags = 0;
    private boolean isDriving = false;
    
    @Data
    private static class TheUWBRecordsTruck {
        int theUWBSendDropsRFID = 0;
    }
    private Boolean inTheTrafficCar = false;

    @Override
    public EventState detectEventAlready(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status) {
        // 第一层检查：必须至少有 RECORD_POINTS_SIZE 个点
        if (recordPoints == null || recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: recordPoints 为 null 或大小不足: " +
                    (recordPoints == null ? "null" : recordPoints.size()) +
                    ", 需要至少 " + FilterConfig.RECORD_POINTS_SIZE);
            return new EventState();
        }

        // 第二层检查：确保可以安全访问 halfSize 索引
        int halfSize = FilterConfig.RECORD_POINTS_SIZE / 2;
        if (recordPoints.size() <= halfSize) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: recordPoints 大小不足以访问中间点: " +
                    recordPoints.size() + ", halfSize=" + halfSize);
            return new EventState();
        }

        // 安全地获取子列表和中间点，使用边界保护
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, Math.min(halfSize, recordPoints.size()));
        int currentIndex = Math.min(halfSize, recordPoints.size() - 1);
        LocationPoint currentPoint = recordPoints.get(currentIndex);

        // 确保计算 theLastTenPoints 时不越界
        int lastStart = Math.max(0, recordPoints.size() - halfSize);
        int lastEnd = recordPoints.size();
        if (lastStart >= lastEnd) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: lastStart >= lastEnd: " + lastStart + " >= " + lastEnd);
            return new EventState();
        }
        List<LocationPoint> theLastTenPoints = recordPoints.subList(lastStart, lastEnd);
        // 判断是否在货运线区域（到达上车区域）
        //        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        boolean inTheTrafficCar = isInTheTrafficCar(theFirstTenPoints, theLastTenPoints);
        // 板车发运上车事件
        if (status == 0
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && isnParkingArea
                && !inTheTrafficCar) {

            System.out.println("⚠️ 检测到车辆已进入发运上车区域（板车）");
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;

            // 判断发运上车点前10个点状态
            for (LocationPoint point : theFirstTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedFirstTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }

            // 判断发运上车点后10个点状态
            for (LocationPoint point : theLastTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.WALKING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
                    arrivedDrivingTag++;
                }
            }

            // 判断状态标签数量是否满足发运区域上车条件
            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_UP_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.SEND_AFTER_UP_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                System.out.println("⚠️ 检测到发运已上车（板车）");
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.TRUCK_SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                carSendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }

        // 板车到达下车事件
        // 检测到达下车事件
        if (status == 1
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && isnParkingArea
                && !inTheTrafficCar) {

            System.out.println("⚠️ 检测到车辆已进入到达下车区域（板车）");
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            int parkTags = 0;
            boolean isParking = false;

            // 判断到达下车点前10个点状态
            for (LocationPoint point : theFirstTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedFirstTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedDrivingTag++;
                }
            }

            // 判断到达下车点后10个点状态
            for (LocationPoint point : theLastTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            // 判断到达下车点前5个点状态
            for (int i = 0; i < 5; i++) {
                LocationPoint point = theLastTenPoints.get(i);
                if (point.getSpeed() < FilterConfig.MIN_WALKING_SPEED){
                    parkTags++;
                }else {
                    parkTags = 0;
                }
                if (!isParking && parkTags >= 2){
                    isParking = true;
                }
            }

            // 判断状态标签数量是否满足到达区域下车条件
            if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_DOWN_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_DOWN_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE
                    && isParking) {
                System.out.println("⚠️ 检测到到达已下车（板车）");
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.TRUCK_ARRIVED_DROPPING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                carSendInLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }
        return new EventState();
    }


    @Override
    public EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status) {
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) {
            return new EventState();
        }
        
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, FilterConfig.RECORD_POINTS_SIZE / 2);
        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
        speedJudgment(currentPoint);
        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - (FilterConfig.RECORD_POINTS_SIZE / 2), recordPoints.size());
        
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        
        // 统计最后10个点是否接近板车作业区附近
        int theLastTenPointsNotInRFIDCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "板车作业区",
                null,
                null,
                null,
                true);
        
        // 判断是否靠近作业台RFID附近
        boolean isRFIDWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "板车作业区",
                null,
                null,
                0);

        if (currentPoint.getAcceptTime().equals("2025-11-05 17:58:16")){
            System.out.println("rfid数（板车）" + theUWBRecordsTruck.theUWBSendDropsRFID);
        }

        // 监测驾驶状态
        if (currentEvent == BoardingDetector.Event.TRUCK_ARRIVED_BOARDING && currentPoint.getSpeed() > FilterConfig.MAX_RUNING_SPEED){
            driverTags++;
        }else {
            driverTags = 0;
        }
        if (!isDriving && driverTags >= 3){
            isDriving = true;
        }

        // 信标识别数
        if (isRFIDWithin) {
            theUWBRecordsTruck.theUWBSendDropsRFID++;
        }else {
            theUWBRecordsTruck.theUWBSendDropsRFID = 0;
        }
        System.out.println("rfid数（板车）" + theUWBRecordsTruck.theUWBSendDropsRFID);
        
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = BoardingDetector.Event.NONE;
            currentEvent = BoardingDetector.Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(), 1);
        }

        if (currentPoint.getAcceptTime().equals("2025-11-05 17:57:53")){
            System.out.println("rfid数（板车）" + theUWBRecordsTruck.theUWBSendDropsRFID);
        }
        // 监测板车卸车上车事件
        if ((sendInLastEventState == null || currentEvent == BoardingDetector.Event.TRUCK_ARRIVED_BOARDING) && theUWBRecordsTruck.theUWBSendDropsRFID >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE) {
            if (theLastTenPointsNotInRFIDCount <= 3) {
                // 检测板车上车事件（离开RFID范围就算上车）
                if (lastEvent == BoardingDetector.Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入板车卸车上车区域");
                    lastEvent = BoardingDetector.Event.TRUCK_ARRIVED_BOARDING;
                    currentEvent = BoardingDetector.Event.TRUCK_ARRIVED_BOARDING;
                    sendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
        }
        if (currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED){
            parkingTags++;
        } else {
            parkingTags = 0;
        }
        
        // 检测板车卸车下车事件
        if (sendInLastEventState != null 
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                && isnParkingArea
                && isDriving) {
            
            System.out.println("⚠️ 检测到车辆已进入板车卸车下车区域");
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            
            // 判断板车卸车下车点前10个点状态
            for (LocationPoint point : theFirstTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.WALKING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
                    arrivedFirstTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
                    arrivedDrivingTag++;
                }
            }
            
            // 判断板车卸车下车点后10个点状态
            for (LocationPoint point : theLastTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            
            // 判断状态标签数量是否满足板车区域下车条件
            if (parkingTags >= FilterConfig.CONTINUED_STOPPED_STATE_SIZE
                    && arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_UP_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_UP_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                System.out.println("⚠️ 检测到板车卸车下车");
                resetInternalState();
                curPoint = currentPoint;
                isDriving = false;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.TRUCK_ARRIVED_DROPPING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                sendInLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }
        
        return new EventState();
    }
    
    @Override
    public void resetState() {
        resetInternalState();
        lastEvent = BoardingDetector.Event.NONE;
        currentEvent = BoardingDetector.Event.NONE;
        curPoint = null;
        lastEventState = new EventState();
        sendInLastEventState = null;
    }
    
    private void resetInternalState() {
        theUWBRecordsTruck.theUWBSendDropsRFID = 0;
    }
    
    @Override
    public String getStrategyName() {
        return "板车装卸策略";
    }
    
    @Override
    public boolean isInParkingArea(LocationPoint currentPoint) {
        return zoneChecker.isInParkingZone(currentPoint);
    }
    
    private void speedJudgment(LocationPoint point) {
        if (point.getState() == MovementAnalyzer.MovementState.DRIVING) {
            System.out.println("🚗 当前正在驾驶，时间为：" + point.getAcceptTime() + " 速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING) {
            System.out.println("🚗🐢 当前正在低速驾驶，时间为：" + point.getAcceptTime() + " 速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.WALKING) {
            System.out.println("🚶 当前在步行，时间为：" + point.getAcceptTime() + " 速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.RUNNING) {
            System.out.println("🏃 当前在小跑，时间为：" + point.getAcceptTime() + " 速度为：" + point.getSpeed() + "m/s");
        } else {
            System.out.println("⛔ 当前静止，时间为：" + point.getAcceptTime());
        }
    }

    public void resetSendSessionState(RealTimeDriverTracker.EventKind kind) {
        resetInternalState();
        lastEvent = BoardingDetector.Event.NONE;
        currentEvent = BoardingDetector.Event.TRUCK_SEND_BOARDING;
        lastEventState = new EventState(currentEvent, curPoint.getTimestamp(), curPoint.getAcceptTime());
        sendOutLastEventState = null;
        carSendOutLastEventState = null;
    }

    private boolean isInTheTrafficCar(List<LocationPoint> theFirstTenPoints, List<LocationPoint> theLastTenPoints) {
        List<LocationPoint> theFirstJTCList = theFirstTenPoints.subList(theFirstTenPoints.size() - 5, theFirstTenPoints.size());
        List<LocationPoint> theLastJTCList = theLastTenPoints.subList(0, 10);
        List<LocationPoint> theJTCList = new ArrayList<>();
//        theJTCList.addAll(theFirstJTCList);
        theJTCList.addAll(theLastJTCList);
        int theLastTenPointsNotInTOJTCCount = tagBeacon.countTagsCloseToBeacons(
                theJTCList,
                baseConfig.getJoysuch().getBuildingId(),
                "交通车",
                null,
                null,
                null,
                true);
        // 获取交通车数
        if (carSendOutLastEventState == null || carSendInLastEventState != null) {
            // 判断是否在交通车上
            if (theLastTenPointsNotInTOJTCCount >= FilterConfig.TRAFFICCAR_STATE_SIZE) {
                inTheTrafficCar = true;
            } else {
                inTheTrafficCar = false;
            }
        }
        return inTheTrafficCar;
    }

}

