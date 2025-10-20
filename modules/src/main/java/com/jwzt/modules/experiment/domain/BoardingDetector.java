package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.vo.EventState;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.jwzt.modules.experiment.config.FilterConfig.ADJACENT_POINTS_TIME_INTERVAL_MS;
import static com.jwzt.modules.experiment.config.FilterConfig.IDENTIFY_IDENTIFY_TIME_INTERVAL_MS;

/**
 * 上下车识别器
 */
@Component
@Scope("prototype")
public class BoardingDetector {

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ZoneChecker zoneChecker;

    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;

    private TheUWBRecords theUWBRecords = new TheUWBRecords();

    private TheUWBRecordsTruck theUWBRecordsTruck = new TheUWBRecordsTruck();

    /** 每张卡的运行态 */
    @Data
    private static class TheUWBRecords {
        int theUWBSendDropsZytA = 0;
        long theUWBSendDropsZytALastTime = 0;
        int theUWBSendDropsZytB = 0;
        long theUWBSendDropsZytBLastTime = 0;
    }

    @Data
    private static class TheUWBRecordsTruck {
        int theUWBSendDropsRFID = 0;
    }

    public boolean detect(LocationPoint currentPoint) {
        return zoneChecker.isInHuoyunxinZone(currentPoint);
    }

    public enum Event {
        NONE,
        ARRIVED_BOARDING,           // 到达上车
        ARRIVED_DROPPING,           // 到达下车
        SEND_BOARDING,           // 发运上车
        SEND_DROPPING,           // 发运下车
    }

    private List<MovementAnalyzer.MovementState> lastStates = new ArrayList<>();
    private MovementAnalyzer.MovementState lastState = MovementAnalyzer.MovementState.STOPPED;
    private MovementAnalyzer.MovementState currentState = MovementAnalyzer.MovementState.STOPPED;
    private Event lastEvent = Event.NONE;
    private Event currentEvent = Event.NONE;
    private LocationPoint curPoint = null;

    private Boolean theLastTenPointsNotInFreightLine = false;

    private int theTrafficCarCount = 0;
    private Boolean inTheTrafficCar = false;

    private EventState  lastEventState = new EventState();
    // 发运/到达状态记录
    private EventState  sendOutLastEventState = null;       // 发运
    private EventState  sendInLastEventState = null;        // 到达

    public Boolean isnParkingArea(LocationPoint currentPoint) {
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        return isnParkingArea;
    }

    private void speedJudgment(LocationPoint point) {
        if (point.getState() == MovementAnalyzer.MovementState.DRIVING) {
            System.out.println("🚗 当前正在驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING) {
            System.out.println("🚗🐢 当前正在低速驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.WALKING) {
            System.out.println("🚶 当前在步行，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (point.getState() == MovementAnalyzer.MovementState.RUNNING) {
            System.out.println("🏃 当前在小跑，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else {
            System.out.println("⛔ 当前静止，时间为：" + point.getAcceptTime());
        }
    }
    public EventState updateState(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints){
        Event result = Event.NONE;
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return new EventState();
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, FilterConfig.RECORD_POINTS_SIZE / 2);
        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
        System.out.println("开始处理：" + currentPoint);
        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - (FilterConfig.RECORD_POINTS_SIZE / 2), recordPoints.size());
        // 判断是否在货运线区域（到达上车区域）
        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在货运线作业台区域（发运下车区域）
        boolean isTheZYTArea = zoneChecker.isInHuoyunxinZytZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        // 获取交通车数
        if (sendOutLastEventState != null || sendInLastEventState != null){
            if (currentPoint.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                    || currentPoint.getState() == MovementAnalyzer.MovementState.DRIVING) {
                theTrafficCarCount++;
            }
        }
        // 判断是否在交通车上
        if (theTrafficCarCount >= FilterConfig.TRAFFICCAR_STATE_SIZE){
            inTheTrafficCar = true;
        }
        // 判断点位是否逐渐远离J车附近基站
//        boolean isFarAway = tagBeacon.isTagGraduallyFarFromBeacon(
//                theLastTenPoints,
//                baseConfig.getJoysuch().getBuildingId(),
//                "货运线",
//                "2号线",
//                "A",
//                2,
//                true
//        );
        // 统计最后10个点是否接近作业台J车附近
        int theLastTenPointsNotInZYTCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A"
        );

        // 判断是否靠近作业台J车附近
        boolean isZYTAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A"
                );
        if (isZYTAWithin){
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
        }
        // 判断是否靠近作业台fid附近
        boolean isZYTBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "B"
        );
        if (isZYTBWithin){
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
//            for (LocationPoint point : theLastTenPoints){
//                boolean isWithin = tagBeacon.theTagIsCloseToTheBeacon(point, baseConfig.getJoysuch().getBuildingId(), "货运线作业台", "2号线", "B");
//                if (isWithin){
//                    theUWBRecords.theUWBSendDrops++;
//                }
//            }
        }
        if (currentPoint.getAcceptTime().equals("2025-09-28 17:22:20")){
            System.out.println("触发断点");
        }
        if (currentPoint.getAcceptTime().equals("2025-09-28 17:41:54")){
            System.out.println("触发断点");
        }
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = Event.NONE;
            currentEvent = Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(),1);
        }
        if (sendOutLastEventState == null && theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE){
            // 检测发运装车事件
            if (theLastTenPointsNotInZYTCount <= 0 && theUWBRecords.theUWBSendDropsZytALastTime > theUWBRecords.theUWBSendDropsZytBLastTime){
                // 检测发运下车事件（在货运线内就算下车）
                if (lastEvent == Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入发运下车区域");
                    // 发运下车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    // 判断状态标签数量是否满足发运区域下车条件
                    System.out.println("⚠️ 检测到发运已下车");
                    System.out.println("J车附近" + theUWBRecords.theUWBSendDropsZytA + "fid附近" + theUWBRecords.theUWBSendDropsZytB);
                    lastEvent = Event.SEND_DROPPING;
                    currentEvent = Event.SEND_DROPPING;
                    sendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp());
                }
            }
            //        // 两个状态之间的时间间隔
//        if (IDENTIFY_IDENTIFY_TIME_INTERVAL_MS > 0
//                && lastEvent == Event.NONE
//                && lastEventState.getTimestamp() > 0
//                && (currentPoint.getTimestamp() - lastEventState.getTimestamp()) < IDENTIFY_IDENTIFY_TIME_INTERVAL_MS){
//            curPoint = null;
//            return new EventState(Event.NONE, currentPoint.getTimestamp(),2);
//        }
            if (theUWBRecords.theUWBSendDropsZytALastTime < theUWBRecords.theUWBSendDropsZytBLastTime){
                // 检测到达上车
                if (lastEvent == Event.NONE && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea){
                    System.out.println("⚠️ 检测到车辆已进入到达上车区域");
                    // 到达上车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;

                    // 判断到达上车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints){
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    // 判断到达上车点后10个点状态
                    for (LocationPoint point : theLastTenPoints){
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
                        System.out.println("⚠️ 检测到到达已上车");
                        curPoint = currentPoint;
                        lastEvent = Event.ARRIVED_BOARDING;
                        currentEvent = Event.ARRIVED_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
                // 检测到达下车事件
                if (lastEvent == Event.ARRIVED_BOARDING && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
                    System.out.println("⚠️ 检测到车辆已进入到达下车区域");
                    // 到达下车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    // 判断到达下车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints){
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
                    for (LocationPoint point : theLastTenPoints){
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
                        System.out.println("⚠️ 检测到到达已下车");
                        lastEvent = Event.NONE;
                        currentEvent = Event.ARRIVED_DROPPING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
            }
        }
        // 检测发运上车事件
        if (sendOutLastEventState != null && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
            System.out.println("⚠️ 检测到车辆已进入发运上车区域");
            // 发运上车点前后状态标签数量
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            // 判断发运上车点前10个点状态
            for (LocationPoint point : theFirstTenPoints){
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedFirstTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            // 判断发运上车点后10个点状态
            for (LocationPoint point : theLastTenPoints){
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
                System.out.println("⚠️ 检测到发运已上车");
                init();
                curPoint = currentPoint;
                lastEvent = Event.NONE;
                currentEvent = Event.SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                sendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
        return new EventState();
    }

    public EventState updateStateTruck(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints){
        Event result = Event.NONE;
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return new EventState();
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
                null
        );

        // 判断是否靠近作业台J车附近
        boolean isRFIDWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "板车作业区",
                null,
                null
        );
        if (isRFIDWithin){
            theUWBRecordsTruck.theUWBSendDropsRFID++;
        }
        System.out.println("rfid数" + theUWBRecordsTruck.theUWBSendDropsRFID);
        if (currentPoint.getAcceptTime().equals("2025-10-16 18:30:10")){
            System.out.println("触发断点");
        }
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = Event.NONE;
            currentEvent = Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(),1);
        }
        // 监测板车上车事件
        if (sendInLastEventState == null && theUWBRecordsTruck.theUWBSendDropsRFID >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE){
            // 检测板车卸车事件
            if (theLastTenPointsNotInRFIDCount <= 0){
                // 检测板车上车事件（离开RFID范围就算上车）
                if (lastEvent == Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入板车卸车上车区域");
                    lastEvent = Event.ARRIVED_BOARDING;
                    currentEvent = Event.ARRIVED_BOARDING;
                    sendInLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp());
                }
            }
        }
        // 检测板车卸车下车事件
        if (sendInLastEventState != null && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
            System.out.println("⚠️ 检测到车辆已进入板车卸车下车区域");
            // 发运上车点前后状态标签数量
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            // 判断板车卸车下车点前10个点状态
            for (LocationPoint point : theFirstTenPoints){
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
            for (LocationPoint point : theLastTenPoints){
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            // 判断状态标签数量是否满足发运区域上车条件
            if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_UP_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_UP_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                System.out.println("⚠️ 检测到板车卸车下车");
                initTruck();
                curPoint = currentPoint;
                lastEvent = Event.NONE;
                currentEvent = Event.ARRIVED_DROPPING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                sendInLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
        return new EventState();
    }

    public EventState updateState(List<LocationPoint> recordPoints){
        Event result = Event.NONE;
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return new EventState();
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, FilterConfig.RECORD_POINTS_SIZE / 2);
        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
        System.out.println("开始处理：" + currentPoint);
        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - (FilterConfig.RECORD_POINTS_SIZE / 2), recordPoints.size());
        // 判断是否在货运线区域（到达上车区域）
        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在货运线作业台区域（发运下车区域）
        boolean isTheZYTArea = zoneChecker.isInHuoyunxinZytZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        // 判断是否靠近作业台J车附近
        boolean isZYTAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A"
        );
        if (isZYTAWithin){
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
        }
        // 判断是否靠近作业台fid附近
        boolean isZYTBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "B"
        );
        if (isZYTBWithin){
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
//            for (LocationPoint point : theLastTenPoints){
//                boolean isWithin = tagBeacon.theTagIsCloseToTheBeacon(point, baseConfig.getJoysuch().getBuildingId(), "货运线作业台", "2号线", "B");
//                if (isWithin){
//                    theUWBRecords.theUWBSendDrops++;
//                }
//            }
        }
        if (currentPoint.getAcceptTime().equals("2025-09-28 17:41:08")){
            System.out.println("触发断点");
        }
        if (currentPoint.getAcceptTime().equals("2025-09-28 17:41:54")){
            System.out.println("触发断点");
        }
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = Event.NONE;
            currentEvent = Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(),1);
        }
        if (theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE && theUWBRecords.theUWBSendDropsZytA > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE){
            // 检测发运装车事件
            if (theUWBRecords.theUWBSendDropsZytALastTime > theUWBRecords.theUWBSendDropsZytBLastTime){
                // 检测发运上车事件
                if (lastEvent == Event.NONE && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
                    System.out.println("⚠️ 检测到车辆已进入发运上车区域");
                    // 发运上车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    // 判断发运上车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints){
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    // 判断发运上车点后10个点状态
                    for (LocationPoint point : theLastTenPoints){
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
                        System.out.println("⚠️ 检测到发运已上车");
                        init();
                        curPoint = currentPoint;
                        lastEvent = Event.SEND_BOARDING;
                        currentEvent = Event.SEND_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
                // 检测发运下车事件（在货运线内就算下车）
                if (lastEvent == Event.SEND_BOARDING && isTheZYTArea) {
                    System.out.println("⚠️ 检测到车辆已进入发运下车区域");
                    // 发运下车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    // 判断状态标签数量是否满足发运区域下车条件
                    System.out.println("⚠️ 检测到发运已下车");
                    System.out.println("J车附近" + theUWBRecords.theUWBSendDropsZytA + "fid附近" + theUWBRecords.theUWBSendDropsZytB);
                    init();
                    lastEvent = Event.NONE;
                    currentEvent = Event.SEND_DROPPING;
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp());
                }
            }
            //        // 两个状态之间的时间间隔
//        if (IDENTIFY_IDENTIFY_TIME_INTERVAL_MS > 0
//                && lastEvent == Event.NONE
//                && lastEventState.getTimestamp() > 0
//                && (currentPoint.getTimestamp() - lastEventState.getTimestamp()) < IDENTIFY_IDENTIFY_TIME_INTERVAL_MS){
//            curPoint = null;
//            return new EventState(Event.NONE, currentPoint.getTimestamp(),2);
//        }
            if (theUWBRecords.theUWBSendDropsZytALastTime < theUWBRecords.theUWBSendDropsZytBLastTime){
                // 检测到达上车
                if (lastEvent == Event.NONE && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea){
                    System.out.println("⚠️ 检测到车辆已进入到达上车区域");
                    // 到达上车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;

                    // 判断到达上车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints){
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    // 判断到达上车点后10个点状态
                    for (LocationPoint point : theLastTenPoints){
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
                        System.out.println("⚠️ 检测到到达已上车");
                        curPoint = currentPoint;
                        lastEvent = Event.ARRIVED_BOARDING;
                        currentEvent = Event.ARRIVED_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
                // 检测到达下车事件
                if (lastEvent == Event.ARRIVED_BOARDING && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
                    System.out.println("⚠️ 检测到车辆已进入到达下车区域");
                    // 到达下车点前后状态标签数量
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    // 判断到达下车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints){
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
                    for (LocationPoint point : theLastTenPoints){
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
                        System.out.println("⚠️ 检测到到达已下车");
                        lastEvent = Event.NONE;
                        currentEvent = Event.ARRIVED_DROPPING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(),currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
            }
        }
        return new EventState();
    }

    private void init() {
        theUWBRecords.theUWBSendDropsZytA = 0;
        theUWBRecords.theUWBSendDropsZytALastTime = 0;
        theUWBRecords.theUWBSendDropsZytB = 0;
        theUWBRecords.theUWBSendDropsZytBLastTime = 0;
    }

    private void initTruck() {
        theUWBRecordsTruck.theUWBSendDropsRFID = 0;
    }

    public Event updateState(MovementAnalyzer.MovementState newState) {
        Event result = Event.NONE;

        if (lastState == MovementAnalyzer.MovementState.WALKING && newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
            result = Event.ARRIVED_BOARDING;
        } else if (lastState == MovementAnalyzer.MovementState.LOW_DRIVING && newState == MovementAnalyzer.MovementState.WALKING) {
            result = Event.ARRIVED_DROPPING;
        }

        lastState = currentState;
        currentState = newState;
        return result;
    }
}
