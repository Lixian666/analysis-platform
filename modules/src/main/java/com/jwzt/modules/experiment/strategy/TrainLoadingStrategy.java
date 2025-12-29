package com.jwzt.modules.experiment.strategy;

import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.MovementAnalyzer;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.vo.EventState;
import lombok.Data;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.jwzt.modules.experiment.config.FilterConfig.*;

/**
 * 火车和地跑组合装卸策略实现
 * 说明：火车装卸和地跑装卸在卡号层面无法区分，需根据实时位置的区域动态判断
 * 使用 updateState 方法进行事件检测
 */
@Component
@Scope("prototype")  // 改为原型模式，每次获取新实例，避免多线程竞争
public class TrainLoadingStrategy implements LoadingUnloadingStrategy {
    
    @Autowired
    private BaseConfig baseConfig;
    
    @Autowired
    private ZoneChecker zoneChecker;
    
    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;
    
    @Autowired
    private OutlierFilter outlierFilter;
    
    private TheUWBRecords theUWBRecords = new TheUWBRecords();
    
    private BoardingDetector.Event lastEvent = BoardingDetector.Event.NONE;
    private BoardingDetector.Event currentEvent = BoardingDetector.Event.NONE;
    private LocationPoint curPoint = null;

    private EventState sendLastEventState = new EventState();
    private EventState arrivedLastEventState = new EventState();

    private EventState carSendLastEventState = new EventState();
    private EventState carArrivedLastEventState = new EventState();

    private EventState lastEventState = new EventState();

    private EventState sendOutLastEventState = null;
    private EventState sendInLastEventState = null;
    private EventState carSendOutLastEventState = null;
    private EventState carSendInLastEventState = null;
    private EventState lastCarInLastEventState = null;

    // 统计交通车定位数
    private int theTrafficCarCount = 0;

    private int thePointAnalysisCount = 0;

    private Boolean inTheTrafficCar = false;

    // 统计是否在A区
    private Boolean inTheA = false;

    // 统计是否识别到装卸车
    private Boolean isTrainOutLoading = false;
    private Boolean isTrainInLoading = false;
    private Boolean isCarOutLoading = false;
    private Boolean isCarInLoading = false;
    
    @Data
    private static class TheUWBRecords {
        int theUWBSendDropsZytA = 0;
        int theUWBSendDropsZytASecondary = 0;
        long theUWBSendDropsZytALastTime = 0;
        long theUWBSendDropsZytASecondaryLastTime = 0;
        double theUWBSendDropsZytALastDistance = 9999;
        double theUWBSendDropsZytASecondaryLastDistance = 9999;
        int theUWBDistanceSmallCountA = 0;
        int theUWBDistanceSmallCountASecondary = 0;
        int theUWBDLastTimeLargeCountA = 0;
        int theUWBDLastTimeLargeCountASecondary = 0;
        List<Long> theUWBSendDropsZytAList = new ArrayList<>();
        List<Long> theUWBSendDropsZytASecondaryList = new ArrayList<>();
        int theUWBSendDropsZytB = 0;
        int theUWBSendDropsZytBSecondary = 0;
        long theUWBSendDropsZytBLastTime = 0;
        long theUWBSendDropsZytBSecondaryLastTime = 0;
        double theUWBSendDropsZytBLastDistance = 9999;
        double theUWBSendDropsZytBSecondaryLastDistance = 9999;
        int theUWBDistanceSmallCountB = 0;
        int theUWBDistanceSmallCountBSecondary = 0;
        int theUWBDLastTimeLargeCountB = 0;
        int theUWBDLastTimeLargeCountBSecondary = 0;
        List<Long> theUWBSendDropsZytBList = new ArrayList<>();
        List<Long> theUWBSendDropsZytBSecondaryList = new ArrayList<>();
    }
    
    @Override
    public EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status) {
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
        if (currentPoint.getTagScanUwbData().getUwbBeaconList() != null && currentPoint.getTagScanUwbData().getUwbBeaconList().size() > 0){
            System.out.println("开始处理: " + currentPoint.getAcceptTime() + "\n" + "信标列表" + currentPoint.getTagScanUwbData().getUwbBeaconList());
        }
        if (currentPoint.getAcceptTime().equals("2025-12-05 14:35:51")){
            System.out.println("开始处理: " + currentPoint.getAcceptTime() + "\n" + "信标列表" + currentPoint.getTagScanUwbData().getUwbBeaconList());
        }
        if (currentPoint.getAcceptTime().equals("2025-12-17 14:12:27")){
            System.out.println("开始处理: " + currentPoint.getAcceptTime() + "\n" + "信标列表" + currentPoint.getTagScanUwbData().getUwbBeaconList());
        }
        if (currentPoint.getAcceptTime().equals("2025-12-17 10:05:38")){
            System.out.println("开始处理: " + currentPoint.getAcceptTime() + "\n" + "信标列表" + currentPoint.getTagScanUwbData().getUwbBeaconList());
        }
        // 根据当前点位所在区域判断是火车装卸还是地跑装卸
        if (isInGroundVehicleZone(currentPoint) == 1 || sendOutLastEventState != null || sendInLastEventState != null) {
            System.out.println("开始处理（火车装卸）：" + currentPoint);
            return detectTrainEvent(recordPoints, historyPoints, theFirstTenPoints, currentPoint, theLastTenPoints, status);
        } else if (isInGroundVehicleZone(currentPoint) == 2 || carSendOutLastEventState != null || carSendInLastEventState != null){
            System.out.println("开始处理（地跑装卸）：" + currentPoint);
            return detectGroundVehicleEvent(recordPoints, historyPoints, theFirstTenPoints, currentPoint, theLastTenPoints, status);
        } else {
            return new EventState();
        }
    }
    
    /**
     * 判断是否在地跑作业区域
     * TODO: 根据实际业务配置地跑作业区域的判断逻辑
     */
    private int isInGroundVehicleZone(LocationPoint currentPoint) {
        if (lastEvent == BoardingDetector.Event.NONE){
            // 判断是否靠近作业台J车附近
            boolean isZYTAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "货运线作业台",
                    null,
                    null,
                    1
            );
            if (isZYTAWithin){
                return 1;
            }
            boolean isDPAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "地跑",
                    null,
                    null,
                    0);
            if (isDPAWithin){
                return 2;
            }
        }
        return 0;
        // 示例：可以通过区域配置或者特定的基站来判断
        // return zoneChecker.isInGroundVehicleZone(currentPoint);
        // 暂时返回 false，待实际业务需求时实现
    }
    
    /**
     * 地跑装卸事件检测
     * TODO: 根据实际业务需求实现地跑装卸的具体检测逻辑
     */
    private EventState detectGroundVehicleEvent(List<LocationPoint> recordPoints,
                                                List<LocationPoint> historyPoints,
                                                List<LocationPoint> theFirstTenPoints,
                                                LocationPoint currentPoint,
                                                List<LocationPoint> theLastTenPoints,
                                                Integer status) {
        // 判断是否在货运线区域（到达上车区域）
        // boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        thePointAnalysisCount ++;
        List<LocationPoint> theJTCList = theLastTenPoints.subList(0, 10);
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
        inTheTrafficCar = false;

        if (currentPoint.getAcceptTime().equals("2025-12-04 17:07:00")){
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: " + currentPoint);
        }

        // 统计最后10个点是否在地跑识别位置附近
        int theLastTenPointsNotInTOJCKCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "地跑",
                null,
                "A",
                null,
                true);

        // 统计最后10个点是否接近作业台J车附近
        int theLastTenPointsNotInTrafficCarCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "交通车",
                null,
                "A",
                null,
                true);

        // 判断是否靠近进出口附近
        boolean isJCKAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "地跑",
                null,
                "A",
                0);

        if (isJCKAWithin && carSendOutLastEventState == null && carSendInLastEventState == null) {
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
            double zytALastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "地跑",
                    null,
                    "A",
                    0);
            if (zytALastDistance != -1){
                theUWBRecords.theUWBSendDropsZytALastDistance = zytALastDistance;
            }
            theUWBRecords.theUWBSendDropsZytAList.add(theUWBRecords.theUWBSendDropsZytALastTime);
        }

        // 判断是否靠近作业台fid附近
        boolean isJCKBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "地跑",
                null,
                "B",
                0);

        if (isJCKBWithin && carSendOutLastEventState == null && carSendInLastEventState == null) {
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
            double zytBLastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "地跑",
                    null,
                    "B",
                    0);
            if (zytBLastDistance != -1){
                theUWBRecords.theUWBSendDropsZytBLastDistance = zytBLastDistance;
            }
            theUWBRecords.theUWBSendDropsZytBList.add(theUWBRecords.theUWBSendDropsZytBLastTime);
        }

        if (DateTimeUtils.dateTimeSSSStrToDateTimeStr(currentPoint.getAcceptTime()).equals("2025-11-20 15:07:24")){
            System.out.println("⚠️ 检测到车辆已进入地跑区域（地跑）");
        }

        // 地跑发运下车检测
        if (carSendOutLastEventState == null
                && theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                && !inTheTrafficCar) {

            if (theLastTenPointsNotInTOJCKCount <= 0
                    && theUWBRecords.theUWBSendDropsZytALastTime > theUWBRecords.theUWBSendDropsZytBLastTime
                    && theUWBRecords.theUWBSendDropsZytBLastDistance > theUWBRecords.theUWBSendDropsZytALastDistance) {

                if (lastEvent == BoardingDetector.Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入发运下车区域（地跑）");
                    System.out.println("A附近" + theUWBRecords.theUWBSendDropsZytA + " fid附近" + theUWBRecords.theUWBSendDropsZytB);
                    resetInternalState();
                    curPoint = currentPoint;
                    lastEvent = BoardingDetector.Event.CAR_SEND_DROPPING;
                    currentEvent = BoardingDetector.Event.CAR_SEND_DROPPING;
                    carSendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
        }

        // 地跑发运上车事件
        if (carSendOutLastEventState != null
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && carSendOutLastEventState.timestamp - currentPoint.getTimestamp()  > IDENTIFY_IDENTIFY_TIME_INTERVAL_MS
                && isnParkingArea
                && !inTheTrafficCar) {

            System.out.println("⚠️ 检测到车辆已进入发运上车区域（地跑）");
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
                System.out.println("⚠️ 检测到发运已上车（地跑）");
                resetInternalState();
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.CAR_SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                carSendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }

        // 地跑到达上车车事件
        if (theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                && !inTheTrafficCar) {
            // 到达上车检测
            if (theUWBRecords.theUWBSendDropsZytALastTime < theUWBRecords.theUWBSendDropsZytBLastTime
                    && theUWBRecords.theUWBSendDropsZytALastDistance > theUWBRecords.theUWBSendDropsZytBLastDistance) {
                if (lastEvent == BoardingDetector.Event.NONE) {
                    System.out.println("⚠️ 检测到到达已上车（地跑）");
                    resetInternalState();
                    curPoint = currentPoint;
                    lastEvent = BoardingDetector.Event.CAR_ARRIVED_BOARDING;
                    currentEvent = BoardingDetector.Event.CAR_ARRIVED_BOARDING;
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    if (carSendInLastEventState != null
                            && currentPoint.getTimestamp() - carSendInLastEventState.timestamp > SAME_STATE_IDENTIFY_TIME_INTERVAL_MS){
                        carSendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude(), 1);
                    }
                    carSendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
        }

        // 地跑到达下车事件
        if (carSendInLastEventState != null && !inTheTrafficCar) {
            // 检测到达下车事件
            if (lastEvent == BoardingDetector.Event.CAR_ARRIVED_BOARDING
                    && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                    && currentPoint.getTimestamp() - carSendInLastEventState.timestamp > IDENTIFY_IDENTIFY_TIME_INTERVAL_MS
                    && isnParkingArea) {

                System.out.println("⚠️ 检测到车辆已进入到达下车区域（地跑）");
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
                    System.out.println("⚠️ 检测到到达已下车（地跑）");
                    resetInternalState();
                    curPoint = currentPoint;
                    lastEvent = BoardingDetector.Event.NONE;
                    currentEvent = BoardingDetector.Event.CAR_ARRIVED_DROPPING;
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    carSendInLastEventState = null;
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
        }
        lastInternalState(currentPoint.getTimestamp());
        lastInternalStateNew(currentPoint.getTimestamp());
//        if (theUWBRecords.theUWBSendDropsZytALastTime != 0L && theUWBRecords.theUWBSendDropsZytBLastTime != 0L) {
//            if (currentPoint.getTimestamp() - theUWBRecords.theUWBSendDropsZytALastTime > BEACON_DISTANCE_EFFECTIVE_TIME_INTERVAL_MS
//                    && currentPoint.getTimestamp() - theUWBRecords.theUWBSendDropsZytBLastTime > BEACON_DISTANCE_EFFECTIVE_TIME_INTERVAL_MS){
//                resetInternalState();
//            }
//        }
        return new EventState();
    }

    private void cleanOld(List<Long> list, long nowTs) {
        long threshold = nowTs - 60000; // 60秒之前的全部移除
        list.removeIf(ts -> ts < threshold);
    }

    private void lastInternalState(long nowTs) {
        cleanOld(theUWBRecords.theUWBSendDropsZytAList, nowTs);
        cleanOld(theUWBRecords.theUWBSendDropsZytBList, nowTs);
        if (theUWBRecords.theUWBSendDropsZytAList.size() > 0){
            theUWBRecords.theUWBSendDropsZytA = theUWBRecords.theUWBSendDropsZytAList.size();
        } else {
            theUWBRecords.theUWBSendDropsZytA = 0;
            theUWBRecords.theUWBSendDropsZytALastTime = 0L;
            theUWBRecords.theUWBSendDropsZytALastDistance = 9999;
            theUWBRecords.theUWBDistanceSmallCountA = 0;
            theUWBRecords.theUWBDLastTimeLargeCountA = 0;
        }
        if (theUWBRecords.theUWBSendDropsZytBList.size() > 0){
            theUWBRecords.theUWBSendDropsZytB = theUWBRecords.theUWBSendDropsZytBList.size();
        } else {
            theUWBRecords.theUWBSendDropsZytB = 0;
            theUWBRecords.theUWBSendDropsZytBLastTime = 0L;
            theUWBRecords.theUWBSendDropsZytBLastDistance = 9999;
            theUWBRecords.theUWBDistanceSmallCountB = 0;
            theUWBRecords.theUWBDLastTimeLargeCountB = 0;
        }
    }

    private void lastInternalStateNew(long nowTs) {
        cleanOld(theUWBRecords.theUWBSendDropsZytASecondaryList, nowTs);
        cleanOld(theUWBRecords.theUWBSendDropsZytBSecondaryList, nowTs);
        if (theUWBRecords.theUWBSendDropsZytASecondaryList.size() > 0){
            theUWBRecords.theUWBSendDropsZytASecondary = theUWBRecords.theUWBSendDropsZytASecondaryList.size();
        } else {
            theUWBRecords.theUWBSendDropsZytASecondary = 0;
            theUWBRecords.theUWBSendDropsZytASecondaryLastTime = 0L;
            theUWBRecords.theUWBSendDropsZytASecondaryLastDistance = 9999;
            theUWBRecords.theUWBDistanceSmallCountASecondary = 0;
            theUWBRecords.theUWBDLastTimeLargeCountASecondary = 0;
        }
        if (theUWBRecords.theUWBSendDropsZytBSecondaryList.size() > 0){
            theUWBRecords.theUWBSendDropsZytBSecondary = theUWBRecords.theUWBSendDropsZytBSecondaryList.size();
        } else {
            theUWBRecords.theUWBSendDropsZytBSecondary = 0;
            theUWBRecords.theUWBSendDropsZytBSecondaryLastTime = 0L;
            theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance = 9999;
            theUWBRecords.theUWBDistanceSmallCountBSecondary = 0;
            theUWBRecords.theUWBDLastTimeLargeCountBSecondary = 0;
        }
    }

    /**
     * 火车装卸事件检测
     */

    private EventState detectTrainEvent(List<LocationPoint> recordPoints,
                                        List<LocationPoint> historyPoints,
                                        List<LocationPoint> theFirstTenPoints,
                                        LocationPoint currentPoint,
                                        List<LocationPoint> theLastTenPoints,
                                        Integer status) {
        // 判断是否在货运线区域（到达上车区域）
        //        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        thePointAnalysisCount ++;
        boolean inTheTrafficCar = isInTheTrafficCar(theFirstTenPoints, theLastTenPoints);
//        inTheTrafficCar = false;
        boolean inTheAArea = isInTheABeaconArea(theFirstTenPoints, currentPoint, theLastTenPoints, 2.0);
        if (currentPoint.getAcceptTime().equals("2025-12-04 17:07:00")){
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: " + currentPoint);
        }

        if (currentPoint.getAcceptTime().equals("2025-12-05 14:49:03")){
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: " + currentPoint);
        }

        // 统计之前10个点是否在J车附近
        int theFirstPointsInTOACount = tagBeacon.countTagsCloseToBeacons(
                theFirstTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                null,
                false
        );
        // 统计之前10个点是否在J车二层附近
        int theFirstPointsInTOASecondaryCount = tagBeacon.countTagsCloseToBeacons(
                theFirstTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                2,
                false);
        theFirstPointsInTOASecondaryCount = 0;
        // 统计最后10个点是否在J车附近
        int theLastPointsInTOACount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                null,
                false
        );
        // 统计最后10个点是否在J车二层附近
        int theLastPointsInTOASecondaryCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                2,
                false);
        theLastPointsInTOASecondaryCount = 9999;
        // 判断是否靠近J车附近
        boolean isJCKAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                null);

        // 判断是否靠近J车二层附近
        boolean isJCKASecondaryWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                2);
        isJCKASecondaryWithin = false;
        if (isJCKAWithin && (sendOutLastEventState == null || sendInLastEventState == null)) {
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
            double zytALastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "货运线作业台",
                    null,
                    "A",
                    null);
            if (zytALastDistance != -1){
                theUWBRecords.theUWBSendDropsZytALastDistance = zytALastDistance;
            }
            theUWBRecords.theUWBSendDropsZytAList.add(theUWBRecords.theUWBSendDropsZytALastTime);
        }

        // Secondary
        if (isJCKASecondaryWithin && (sendOutLastEventState == null || sendInLastEventState == null)) {
            theUWBRecords.theUWBSendDropsZytASecondary++;
            theUWBRecords.theUWBSendDropsZytASecondaryLastTime = currentPoint.getTimestamp();
            double zytASecondaryLastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "货运线作业台",
                    null,
                    "A",
                    2);
            if (zytASecondaryLastDistance != -1){
                theUWBRecords.theUWBSendDropsZytASecondaryLastDistance = zytASecondaryLastDistance;
            }
            theUWBRecords.theUWBSendDropsZytASecondaryList.add(theUWBRecords.theUWBSendDropsZytASecondaryLastTime);
        }

        // 判断是否靠近作业台fid附近
        boolean isJCKBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "B",
                null);

        // 判断是否靠近作业台fid附近
        boolean isJCKBSecondaryWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "B",
                2);
        isJCKBSecondaryWithin = false;
        if (isJCKBWithin && (sendOutLastEventState == null || sendInLastEventState == null)) {
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
            double zytBLastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "货运线作业台",
                    null,
                    "B",
                    null
            );
            if (zytBLastDistance != -1){
                theUWBRecords.theUWBSendDropsZytBLastDistance = zytBLastDistance;
            }
            theUWBRecords.theUWBSendDropsZytBList.add(theUWBRecords.theUWBSendDropsZytBLastTime);
        }

        // Secondary
        if (isJCKBSecondaryWithin && (sendOutLastEventState == null || sendInLastEventState == null)) {
            theUWBRecords.theUWBSendDropsZytBSecondary++;
            theUWBRecords.theUWBSendDropsZytBSecondaryLastTime = currentPoint.getTimestamp();
            double zytBSecondaryLastDistance = tagBeacon.getTagDistanceToNearestBeacon(
                    currentPoint,
                    baseConfig.getJoysuch().getBuildingId(),
                    "货运线作业台",
                    null,
                    "B",
                    2
            );
            if (zytBSecondaryLastDistance != -1){
                theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance = zytBSecondaryLastDistance;
            }
            theUWBRecords.theUWBSendDropsZytBSecondaryList.add(theUWBRecords.theUWBSendDropsZytBSecondaryLastTime);
        }
//        信标距离优化
//        if ((sendOutLastEventState == null || sendInLastEventState == null) && isJCKAWithin && !isJCKBWithin ){
//            theUWBRecords.theUWBSendDropsZytBLastDistance = 9999;
//        }else if ((sendOutLastEventState == null || sendInLastEventState == null) && !isJCKAWithin && isJCKBWithin){
//            theUWBRecords.theUWBSendDropsZytALastDistance = 9999;
//        }else if ((sendOutLastEventState == null || sendInLastEventState == null) && !isJCKAWithin && !isJCKBWithin){
//            theUWBRecords.theUWBSendDropsZytALastDistance = 9999;
//            theUWBRecords.theUWBSendDropsZytBLastDistance = 9999;
//        }

        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS && status == 0) {
            // 重置状态
            if (sendInLastEventState != null){
                resetInternalState();
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.ARRIVED_DROPPING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                arrivedLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                sendInLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude(),1, 0);
            }
//            curPoint = currentPoint;
//            lastEvent = BoardingDetector.Event.NONE;
//            currentEvent = BoardingDetector.Event.NONE;
//            lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
//            sendOutLastEventState = null;
//            return new EventState(currentEvent, currentPoint.getTimestamp(),1);
        }

        if (DateTimeUtils.dateTimeSSSStrToDateTimeStr(currentPoint.getAcceptTime()).equals("2025-12-17 14:02:10")){
            System.out.println("⚠️ 检测到车辆已进入地跑区域（火车）");
        }

        // 火车发运下车检测
        if ((sendOutLastEventState == null
            && (theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                || theUWBRecords.theUWBSendDropsZytASecondary >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE)
            && (theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE
                || theUWBRecords.theUWBSendDropsZytBSecondary > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE)
            && !inTheTrafficCar && inTheAArea) || (isTrainOutLoading == true && !inTheTrafficCar && inTheAArea)) {
            if (((theUWBRecords.theUWBSendDropsZytBLastTime != 0
                && theUWBRecords.theUWBSendDropsZytALastTime >= theUWBRecords.theUWBSendDropsZytBLastTime
                && theUWBRecords.theUWBSendDropsZytALastDistance < theUWBRecords.theUWBSendDropsZytBLastDistance
                ) || ((theUWBRecords.theUWBSendDropsZytASecondaryLastTime != 0
                    && theUWBRecords.theUWBSendDropsZytASecondaryLastTime >= theUWBRecords.theUWBSendDropsZytBSecondaryLastTime
                    && theUWBRecords.theUWBSendDropsZytASecondaryLastDistance < theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance))
                ) || isTrainOutLoading == true) {
                if ((theUWBRecords.theUWBSendDropsZytBLastTime != 0
                    && theUWBRecords.theUWBSendDropsZytALastTime >= theUWBRecords.theUWBSendDropsZytBLastTime
                    && theUWBRecords.theUWBSendDropsZytALastDistance < theUWBRecords.theUWBSendDropsZytBLastDistance
                    ) || isTrainOutLoading == true){
                    theUWBRecords.theUWBDistanceSmallCountA++;
                    theUWBRecords.theUWBDLastTimeLargeCountA++;
                }
                if ((theUWBRecords.theUWBSendDropsZytASecondaryLastTime != 0
                    && theUWBRecords.theUWBSendDropsZytASecondaryLastTime >= theUWBRecords.theUWBSendDropsZytBSecondaryLastTime
                    && theUWBRecords.theUWBSendDropsZytASecondaryLastDistance < theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance
                    ) || isTrainOutLoading == true){
                    theUWBRecords.theUWBDistanceSmallCountASecondary++;
                    theUWBRecords.theUWBDLastTimeLargeCountASecondary++;
                }
                if ((theUWBRecords.theUWBDistanceSmallCountA >= AB_DISTANCE_THRESHOLD
                    && theUWBRecords.theUWBDLastTimeLargeCountA >= AB_LAST_TIME_THRESHOLD
                    ) || (
                        theUWBRecords.theUWBDistanceSmallCountASecondary >= AB_DISTANCE_THRESHOLD
                        && theUWBRecords.theUWBDLastTimeLargeCountASecondary >= AB_LAST_TIME_THRESHOLD)
                    ) {
                    if (status == 0){
                        isTrainOutLoading = true;
                    }
                    // 在识别到到达上车后又识别到发运下车（识别有冲突 不启用）
//                    if (sendOutLastEventState == null && sendInLastEventState != null && lastEvent == BoardingDetector.Event.ARRIVED_BOARDING){
//                        sendInLastEventState = null;
//                        System.out.println("⚠️ 检测到车辆已进入发运下车区域（火车）已有到达识别流程，结束到达识别流程");
//                        System.out.println("A附近" + theUWBRecords.theUWBSendDropsZytA + " fid附近" + theUWBRecords.theUWBSendDropsZytB);
//                        resetInternalState();
//                        curPoint = currentPoint;
//                        lastEvent = BoardingDetector.Event.SEND_DROPPING;
//                        currentEvent = BoardingDetector.Event.SEND_DROPPING;
//                        sendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
//                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
//                        sendLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
//                        isTrainOutLoading = false;
//                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude(), 2);
//                    }
                    // 在识别到到达上车后又识别到发运下车（识别有冲突 不启用 结束到达卸车流程）
                    if (sendOutLastEventState == null && sendInLastEventState != null && lastEvent == BoardingDetector.Event.ARRIVED_BOARDING){
                        resetInternalState();
                        curPoint = currentPoint;
                        lastEvent = BoardingDetector.Event.NONE;
                        currentEvent = BoardingDetector.Event.ARRIVED_DROPPING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        arrivedLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        sendInLastEventState = null;
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude(),1, 0);
                    }
                    boolean isDuplicateIdentification = false;
                    if (currentPoint.getTimestamp() - sendLastEventState.getTimestamp() <= FilterConfig.SEND_AFTER_DOWN_TIME_THRESHOLD){
                        isDuplicateIdentification = true;
                    }
                    if (lastEvent == BoardingDetector.Event.NONE
                            && !isDuplicateIdentification
                            && (theFirstPointsInTOACount >= AB_IDENTIFY_DISTANCE_STATE_SIZE || theFirstPointsInTOASecondaryCount >= AB_IDENTIFY_DISTANCE_STATE_SIZE)
                            && (theLastPointsInTOACount <= AB_DISTANCE_STATE_SIZE || theLastPointsInTOASecondaryCount <= AB_DISTANCE_STATE_SIZE)) {
                        System.out.println("⚠️ 检测到车辆已进入发运下车区域（火车）");
//                        System.out.println("A附近" + theUWBRecords.theUWBSendDropsZytA + " fid附近" + theUWBRecords.theUWBSendDropsZytB);
                        resetInternalState();
                        curPoint = currentPoint;
                        lastEvent = BoardingDetector.Event.SEND_DROPPING;
                        currentEvent = BoardingDetector.Event.SEND_DROPPING;
                        sendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        sendLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        isTrainOutLoading = false;
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                    }
                }
            }
        }

        // 火车发运上车事件
        if (sendOutLastEventState != null
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && sendOutLastEventState.timestamp - currentPoint.getTimestamp()  > IDENTIFY_IDENTIFY_TIME_INTERVAL_MS
                && isnParkingArea
                && !inTheTrafficCar) {

            System.out.println("⚠️ 检测到车辆已进入发运上车区域（火车）");
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
                System.out.println("⚠️ 检测到发运已上车（火车）");
                resetInternalState();
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                sendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }

        // 火车到达上车车事件
        if (((theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE)
                    || (theUWBRecords.theUWBSendDropsZytASecondary >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE && theUWBRecords.theUWBSendDropsZytBSecondary > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE))
                && !inTheTrafficCar && inTheAArea || (isTrainInLoading == true && !inTheTrafficCar && inTheAArea)) {
            // 到达上车检测
            if (((theUWBRecords.theUWBSendDropsZytALastTime != 0 && theUWBRecords.theUWBSendDropsZytALastTime <= theUWBRecords.theUWBSendDropsZytBLastTime && theUWBRecords.theUWBSendDropsZytALastDistance > theUWBRecords.theUWBSendDropsZytBLastDistance)
                    || (theUWBRecords.theUWBSendDropsZytASecondaryLastTime != 0 && theUWBRecords.theUWBSendDropsZytASecondaryLastTime <= theUWBRecords.theUWBSendDropsZytBSecondaryLastTime && theUWBRecords.theUWBSendDropsZytASecondaryLastDistance > theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance)   )
                || isTrainInLoading == true) {
                if ((theUWBRecords.theUWBSendDropsZytALastTime != 0 && theUWBRecords.theUWBSendDropsZytALastTime <= theUWBRecords.theUWBSendDropsZytBLastTime && theUWBRecords.theUWBSendDropsZytALastDistance > theUWBRecords.theUWBSendDropsZytBLastDistance)
                    || isTrainInLoading == true){
                    theUWBRecords.theUWBDistanceSmallCountB++;
                    theUWBRecords.theUWBDLastTimeLargeCountB++;
                }
                if ((theUWBRecords.theUWBSendDropsZytASecondaryLastTime != 0 && theUWBRecords.theUWBSendDropsZytASecondaryLastTime <= theUWBRecords.theUWBSendDropsZytBSecondaryLastTime && theUWBRecords.theUWBSendDropsZytASecondaryLastDistance > theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance)
                        || isTrainInLoading == true){
                    theUWBRecords.theUWBDistanceSmallCountBSecondary++;
                    theUWBRecords.theUWBDLastTimeLargeCountBSecondary++;
                }
                if ((theUWBRecords.theUWBDistanceSmallCountB >= AB_DISTANCE_THRESHOLD && theUWBRecords.theUWBDLastTimeLargeCountB >= AB_DISTANCE_THRESHOLD)
                    || (theUWBRecords.theUWBDistanceSmallCountBSecondary >= AB_DISTANCE_THRESHOLD && theUWBRecords.theUWBDLastTimeLargeCountBSecondary >= AB_DISTANCE_THRESHOLD)){
                    if (status == 0){
                        isTrainInLoading = true;
                    }
                    boolean isDuplicateIdentification = false;
                    if (currentPoint.getTimestamp() - arrivedLastEventState.getTimestamp() <= FilterConfig.SEND_AFTER_DOWN_TIME_THRESHOLD){
                        isDuplicateIdentification = true;
                    }
                    if (lastEvent == BoardingDetector.Event.NONE
                            && !isDuplicateIdentification
                            && (theFirstPointsInTOACount >= AB_IDENTIFY_DISTANCE_STATE_SIZE || theFirstPointsInTOASecondaryCount >= AB_IDENTIFY_DISTANCE_STATE_SIZE)
                            && (theLastPointsInTOACount <= AB_DISTANCE_STATE_SIZE || theLastPointsInTOASecondaryCount <= AB_DISTANCE_STATE_SIZE)) {
                        System.out.println("⚠️ 检测到到达已上车（火车）");
                        resetInternalState();
                        curPoint = currentPoint;
                        lastEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        currentEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        arrivedLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        if (sendInLastEventState != null
                                && currentPoint.getTimestamp() - sendInLastEventState.timestamp > SAME_STATE_IDENTIFY_TIME_INTERVAL_MS){
                            sendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                            isTrainInLoading = false;
                            return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude(), 1);
                        }
                        sendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        isTrainInLoading = false;
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                    }
                }
            }
        }

        // 火车到达下车事件
        if (sendInLastEventState != null && !inTheTrafficCar) {
            // 检测到达下车事件
            if (lastEvent == BoardingDetector.Event.ARRIVED_BOARDING
                    && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                    && currentPoint.getTimestamp() - sendInLastEventState.timestamp > IDENTIFY_IDENTIFY_TIME_INTERVAL_MS
                    && isnParkingArea) {

                System.out.println("⚠️ 检测到车辆已进入到达下车区域（火车）");
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
                    System.out.println("⚠️ 检测到到达已下车（火车）");
                    resetInternalState();
                    curPoint = currentPoint;
                    lastEvent = BoardingDetector.Event.NONE;
                    currentEvent = BoardingDetector.Event.ARRIVED_DROPPING;
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    sendInLastEventState = null;
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
        }
        lastInternalState(currentPoint.getTimestamp());
        lastInternalStateNew(currentPoint.getTimestamp());
//        if (theUWBRecords.theUWBSendDropsZytALastTime != 0L && theUWBRecords.theUWBSendDropsZytBLastTime != 0L) {
//            if (currentPoint.getTimestamp() - theUWBRecords.theUWBSendDropsZytALastTime > BEACON_DISTANCE_EFFECTIVE_TIME_INTERVAL_MS
//                    && currentPoint.getTimestamp() - theUWBRecords.theUWBSendDropsZytBLastTime > BEACON_DISTANCE_EFFECTIVE_TIME_INTERVAL_MS){
//                resetInternalState();
//            }
//        }
        return new EventState();
    }

    private boolean isInTheABeaconArea(List<LocationPoint> theFirstTenPoints, LocationPoint currentPoint, List<LocationPoint> theLastTenPoints, Double distance) {
        List<LocationPoint> theFirstJTCList = theFirstTenPoints.subList(theFirstTenPoints.size() - 3, theFirstTenPoints.size());
        List<LocationPoint> theLastJTCList = theLastTenPoints.subList(0, 3);
        List<LocationPoint> theList = new ArrayList<>();
        theList.addAll(theFirstJTCList);
        theList.add(currentPoint);
        theList.addAll(theLastJTCList);
        int thePointsInTOACount = tagBeacon.countTagsCloseToBeaconsCustom(
                theList,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                null,
                "A",
                null,
                false,
                distance);
            // 判断是否在交通车上
        if (thePointsInTOACount >= FilterConfig.A_DISTANCE_STATE_SIZE) {
            inTheA = true;
        } else {
            inTheA = false;
        }
        return inTheA;
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
        if (sendOutLastEventState == null || sendInLastEventState != null) {
            // 判断是否在交通车上
            if (theLastTenPointsNotInTOJTCCount >= FilterConfig.TRAFFICCAR_STATE_SIZE) {
                inTheTrafficCar = true;
            } else {
                inTheTrafficCar = false;
            }
        }
        return inTheTrafficCar;
    }

    private static @NonNull List<LocationPoint> getPointList(List<LocationPoint> theFirstTenPoints, List<LocationPoint> theLastTenPoints) {
        List<LocationPoint> theFirstJTCList = theFirstTenPoints.subList(theFirstTenPoints.size() - 5, theFirstTenPoints.size());
        List<LocationPoint> theLastJTCList = theLastTenPoints.subList(0, 10);
        List<LocationPoint> theJTCList = new ArrayList<>();
//        theJTCList.addAll(theFirstJTCList);
        theJTCList.addAll(theLastJTCList);
        return theJTCList;
    }

    /**
     * 火车装卸事件检测（原有逻辑）
     */
    private EventState detectTrainEvent_o(List<LocationPoint> recordPoints,
                                        List<LocationPoint> historyPoints,
                                        List<LocationPoint> theFirstTenPoints,
                                        LocationPoint currentPoint,
                                        List<LocationPoint> theLastTenPoints) {
        
        // 判断是否在货运线区域（到达上车区域）
        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        
//        // 获取交通车数
//        if (sendOutLastEventState != null || sendInLastEventState != null) {
//            if (currentPoint.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                    || currentPoint.getState() == MovementAnalyzer.MovementState.DRIVING) {
//                theTrafficCarCount++;
//            }
//        }

        // 统计最后10个点是否在交通车附近
        int theLastTenPointsNotInZYTCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A",
                null,
                true);
        
        // 判断是否在交通车上
        if (theTrafficCarCount >= FilterConfig.TRAFFICCAR_STATE_SIZE) {
            inTheTrafficCar = true;
        }
        
        // 统计最后10个点是否接近作业台J车附近
        int theLastTenPointsNotInTrafficCarCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A",
                null,
                true);
        
        // 判断是否靠近作业台J车附近
        boolean isZYTAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A",
                0);
        if (isZYTAWithin) {
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
        }
        
        // 判断是否靠近作业台fid附近
        boolean isZYTBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "B",
                0);
        if (isZYTBWithin) {
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
        }
        
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = BoardingDetector.Event.NONE;
            currentEvent = BoardingDetector.Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(), 1);
        }
        
        // 发运下车检测
        if (sendOutLastEventState == null 
                && theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE 
                && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE) {
            
            if (theLastTenPointsNotInZYTCount <= 0 
                    && theUWBRecords.theUWBSendDropsZytALastTime > theUWBRecords.theUWBSendDropsZytBLastTime) {
                
                if (lastEvent == BoardingDetector.Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入发运下车区域（火车）");
                    System.out.println("J车附近" + theUWBRecords.theUWBSendDropsZytA + " fid附近" + theUWBRecords.theUWBSendDropsZytB);
                    lastEvent = BoardingDetector.Event.SEND_DROPPING;
                    currentEvent = BoardingDetector.Event.SEND_DROPPING;
                    sendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                }
            }
            
            // 到达上车检测
            if (theUWBRecords.theUWBSendDropsZytALastTime < theUWBRecords.theUWBSendDropsZytBLastTime) {
                if (lastEvent == BoardingDetector.Event.NONE 
                        && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                        && isTheFreightLineArea) {
                    
                    System.out.println("⚠️ 检测到车辆已进入到达上车区域（火车）");
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    
                    // 判断到达上车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints) {
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    
                    // 判断到达上车点后10个点状态
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
                    
                    // 判断状态标签数量是否满足到达区域上车条件
                    if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_UP_STATE_SIZE
                            && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_UP_STATE_SIZE
                            && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                            && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                        System.out.println("⚠️ 检测到到达已上车（火车）");
                        curPoint = currentPoint;
                        lastEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        currentEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                    }
                }
                
                // 检测到达下车事件
                if (lastEvent == BoardingDetector.Event.ARRIVED_BOARDING 
                        && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                        && isnParkingArea) {
                    
                    System.out.println("⚠️ 检测到车辆已进入到达下车区域（火车）");
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    
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
                    
                    // 判断状态标签数量是否满足到达区域下车条件
                    if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_DOWN_STATE_SIZE
                            && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_DOWN_STATE_SIZE
                            && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                            && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                        System.out.println("⚠️ 检测到到达已下车（火车）");
                        lastEvent = BoardingDetector.Event.NONE;
                        currentEvent = BoardingDetector.Event.ARRIVED_DROPPING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
                    }
                }
            }
        }
        
        // 检测发运上车事件
        if (sendOutLastEventState != null 
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                && isnParkingArea) {
            
            System.out.println("⚠️ 检测到车辆已进入发运上车区域（火车）");
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
                System.out.println("⚠️ 检测到发运已上车（火车）");
                resetInternalState();
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                sendOutLastEventState = null;
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
        sendLastEventState = new EventState();
        arrivedLastEventState = new EventState();
        carSendLastEventState = new EventState();
        carArrivedLastEventState = new EventState();
        sendOutLastEventState = null;
        sendInLastEventState = null;
        theTrafficCarCount = 0;
        inTheTrafficCar = false;
    }
    
    private void resetInternalState() {
        theUWBRecords.theUWBSendDropsZytA = 0;
        theUWBRecords.theUWBSendDropsZytALastTime = 0;
        theUWBRecords.theUWBSendDropsZytALastDistance = 9999;
        theUWBRecords.theUWBDistanceSmallCountA = 0;
        theUWBRecords.theUWBDLastTimeLargeCountA = 0;
        theUWBRecords.theUWBSendDropsZytAList.clear();
        theUWBRecords.theUWBSendDropsZytB = 0;
        theUWBRecords.theUWBSendDropsZytBLastTime = 0;
        theUWBRecords.theUWBSendDropsZytBLastDistance = 9999;
        theUWBRecords.theUWBDistanceSmallCountB = 0;
        theUWBRecords.theUWBDLastTimeLargeCountB = 0;
        theUWBRecords.theUWBSendDropsZytBList.clear();
        // Secondary 主备
        theUWBRecords.theUWBSendDropsZytASecondary = 0;
        theUWBRecords.theUWBSendDropsZytASecondaryLastTime = 0;
        theUWBRecords.theUWBSendDropsZytASecondaryLastDistance = 9999;
        theUWBRecords.theUWBDistanceSmallCountASecondary = 0;
        theUWBRecords.theUWBDLastTimeLargeCountASecondary = 0;
        theUWBRecords.theUWBSendDropsZytASecondaryList.clear();
        theUWBRecords.theUWBSendDropsZytBSecondary = 0;
        theUWBRecords.theUWBSendDropsZytBSecondaryLastTime = 0;
        theUWBRecords.theUWBSendDropsZytBSecondaryLastDistance = 9999;
        theUWBRecords.theUWBDistanceSmallCountBSecondary = 0;
        theUWBRecords.theUWBDLastTimeLargeCountBSecondary = 0;
        theUWBRecords.theUWBSendDropsZytBSecondaryList.clear();
    }

    public void resetSendSessionState(RealTimeDriverTracker.EventKind kind) {
        resetInternalState();
        lastEvent = BoardingDetector.Event.NONE;
        currentEvent = BoardingDetector.Event.SEND_BOARDING;
        if (kind == RealTimeDriverTracker.EventKind.CAR_SEND){
            currentEvent = BoardingDetector.Event.CAR_SEND_BOARDING;
        }
        lastEventState = new EventState(currentEvent, curPoint.getTimestamp(), curPoint.getAcceptTime());
        sendOutLastEventState = null;
        carSendOutLastEventState = null;
        theTrafficCarCount = 0;
        inTheTrafficCar = false;
        isTrainOutLoading = false;
        isTrainInLoading = false;
        isCarOutLoading = false;
        isCarInLoading = false;
    }
    
    @Override
    public String getStrategyName() {
        return "火车和地跑组合装卸策略";
    }
    
    @Override
    public boolean isInParkingArea(LocationPoint currentPoint) {
        return zoneChecker.isInParkingZone(currentPoint);
    }
}

