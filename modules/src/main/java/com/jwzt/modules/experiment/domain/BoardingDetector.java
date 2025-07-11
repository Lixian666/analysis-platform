package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.vo.EventState;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下车识别器
 */
public class BoardingDetector {

    private static final String HUOCHANG = FilePathConfig.YUZUI;

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


    public EventState updateState(List<LocationPoint> recordPoints){
        Event result = Event.NONE;;
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return new EventState();
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, 10);
        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - 10, recordPoints.size());
        ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);
        // 判断是否在货运线区域（发运下车区域）
        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        // 判断上移流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > 300000) {
            // 重置状态
            lastEvent = Event.NONE;
            currentEvent = Event.NONE;
        }
        if (currentPoint.getAcceptTime().equals("2025-07-05 10:19:55.000")){
            System.out.println("触发断点");
        }
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
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
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
                curPoint = currentPoint;
                lastEvent = Event.SEND_BOARDING;
                currentEvent = Event.SEND_BOARDING;
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
        // 检测发运下车事件
        if (lastEvent == Event.SEND_BOARDING && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea) {
            System.out.println("⚠️ 检测到车辆已进入发运下车区域");
            // 发运下车点前后状态标签数量
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            // 判断发运下车点前10个点状态
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
            // 判断发运下车点后10个点状态
            for (LocationPoint point : theLastTenPoints){
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            // 判断状态标签数量是否满足发运区域下车条件
            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_DOWN_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.SEND_AFTER_DOWN_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                System.out.println("⚠️ 检测到发运已下车");
                lastEvent = Event.NONE;
                currentEvent = Event.SEND_DROPPING;
                result = currentEvent;
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
        return new EventState();
    }
    public Event updateState(ArrayList<LocationPoint> window, MovementAnalyzer.MovementState newState) {
        Event result = Event.NONE;;

        ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);

        if (lastState == MovementAnalyzer.MovementState.WALKING && newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
            result = Event.ARRIVED_BOARDING;
        } else if (lastState == MovementAnalyzer.MovementState.LOW_DRIVING && newState == MovementAnalyzer.MovementState.WALKING) {
            if (zoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                result = Event.ARRIVED_DROPPING;
            }
        }

        lastState = currentState;
        currentState = newState;
        return result;
    }

//    public Event updateState(List<LocationPoint> recordPoints){
//        Event result = Event.NONE;;
//        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return result;
//        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, 10);
//        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
//        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - 10, recordPoints.size());
//        ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);
//        // 判断是否在货运线区域（发运下车区域）
//        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
//        // 判断是否在停车区域（发运上车区域）
//        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
//        // 判断上移流程是否超时
//        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > 300000) {
//            // 重置状态
//            lastEvent = Event.NONE;
//            currentEvent = Event.NONE;
//        }
//        if (currentPoint.getAcceptTime().equals("2025-07-05 10:19:55.000")){
//            System.out.println("触发断点");
//        }
//        // 检测到达上车
//        if (lastEvent == Event.NONE && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea){
//            System.out.println("⚠️ 检测到车辆已进入到达上车区域");
//            // 到达上车点前后状态标签数量
//            int arrivedStoppedTag = 0;
//            int arrivedDrivingTag = 0;
//            int arrivedFirstTag = 0;
//            int arrivedLastTag = 0;
//
//            // 判断到达上车点前10个点状态
//            for (LocationPoint point : theFirstTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedFirstTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
//                    arrivedStoppedTag++;
//                }
//            }
//            // 判断到达上车点后10个点状态
//            for (LocationPoint point : theLastTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.WALKING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
//                    arrivedLastTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
//                    arrivedDrivingTag++;
//                }
//            }
//            // 判断状态标签数量是否满足到达区域上车条件
//            if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_UP_STATE_SIZE
//                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_UP_STATE_SIZE
//                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
//                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
//                System.out.println("⚠️ 检测到到达已上车");
//                curPoint = currentPoint;
//                lastEvent = Event.ARRIVED_BOARDING;
//                currentEvent = Event.ARRIVED_BOARDING;
//                result = currentEvent;
//                return result;
//            }
//        }
//        // 检测到达下车事件
//        if (lastEvent == Event.ARRIVED_BOARDING && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
//            System.out.println("⚠️ 检测到车辆已进入到达下车区域");
//            // 到达下车点前后状态标签数量
//            int arrivedStoppedTag = 0;
//            int arrivedDrivingTag = 0;
//            int arrivedFirstTag = 0;
//            int arrivedLastTag = 0;
//            // 判断到达下车点前10个点状态
//            for (LocationPoint point : theFirstTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedFirstTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedDrivingTag++;
//                }
//            }
//            // 判断到达下车点后10个点状态
//            for (LocationPoint point : theLastTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedLastTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
//                    arrivedStoppedTag++;
//                }
//
//            }
//            // 判断状态标签数量是否满足到达区域下车条件
//            if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_DOWN_STATE_SIZE
//                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_DOWN_STATE_SIZE
//                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
//                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
//                System.out.println("⚠️ 检测到到达已下车");
//                lastEvent = Event.NONE;
//                currentEvent = Event.ARRIVED_DROPPING;
//                result = currentEvent;
//                return result;
//            }
//        }
//        // 检测发运上车事件
//        if (lastEvent == Event.NONE && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isnParkingArea) {
//            System.out.println("⚠️ 检测到车辆已进入发运上车区域");
//            // 发运上车点前后状态标签数量
//            int arrivedStoppedTag = 0;
//            int arrivedDrivingTag = 0;
//            int arrivedFirstTag = 0;
//            int arrivedLastTag = 0;
//            // 判断发运上车点前10个点状态
//            for (LocationPoint point : theFirstTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedFirstTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
//                    arrivedStoppedTag++;
//                }
//            }
//            // 判断发运上车点后10个点状态
//            for (LocationPoint point : theLastTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.WALKING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
//                    arrivedLastTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
//                    arrivedDrivingTag++;
//                }
//            }
//            // 判断状态标签数量是否满足发运区域上车条件
//            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_UP_STATE_SIZE
//                    && arrivedLastTag >= FilterConfig.SEND_AFTER_UP_STATE_SIZE
//                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
//                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
//                System.out.println("⚠️ 检测到发运已上车");
//                curPoint = currentPoint;
//                lastEvent = Event.SEND_BOARDING;
//                currentEvent = Event.SEND_BOARDING;
//                result = currentEvent;
//                return result;
//            }
//        }
//        // 检测发运下车事件
//        if (lastEvent == Event.SEND_BOARDING && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea) {
//            System.out.println("⚠️ 检测到车辆已进入发运下车区域");
//            // 发运下车点前后状态标签数量
//            int arrivedStoppedTag = 0;
//            int arrivedDrivingTag = 0;
//            int arrivedFirstTag = 0;
//            int arrivedLastTag = 0;
//            // 判断发运下车点前10个点状态
//            for (LocationPoint point : theFirstTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedFirstTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
//                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedDrivingTag++;
//                }
//            }
//            // 判断发运下车点后10个点状态
//            for (LocationPoint point : theLastTenPoints){
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
//                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
//                    arrivedLastTag++;
//                }
//                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
//                    arrivedStoppedTag++;
//                }
//            }
//            // 判断状态标签数量是否满足发运区域下车条件
//            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_DOWN_STATE_SIZE
//                    && arrivedLastTag >= FilterConfig.SEND_AFTER_DOWN_STATE_SIZE
//                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
//                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
//                System.out.println("⚠️ 检测到发运已下车");
//                lastEvent = Event.NONE;
//                currentEvent = Event.SEND_DROPPING;
//                result = currentEvent;
//                return result;
//            }
//        }
//        return result;
//    }
//    public Event updateState(ArrayList<LocationPoint> window, MovementAnalyzer.MovementState newState) {
//        Event result = Event.NONE;;
//
//        ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);
//
//        if (lastState == MovementAnalyzer.MovementState.WALKING && newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
//            result = Event.ARRIVED_BOARDING;
//        } else if (lastState == MovementAnalyzer.MovementState.LOW_DRIVING && newState == MovementAnalyzer.MovementState.WALKING) {
//            if (zoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
//                result = Event.ARRIVED_DROPPING;
//            }
//        }
//
//        lastState = currentState;
//        currentState = newState;
//        return result;
//    }

    public <E> Event updateState(ArrayList<E> window, ArrayList<E> states) {
        Event result = Event.NONE;
        if (states.size() < 5) return result;
        int lastCountBoarding = 0;
        int currentCountBoarding = 0;
        int lastCountDropping = 0;
        int currentCountDropping = 0;

        ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);

        if (lastStates.size() == 5) {
            for (int i = 0; i < states.size() - 1; i++) {
                MovementAnalyzer.MovementState newState = (MovementAnalyzer.MovementState) states.get(i);
                if (newState == MovementAnalyzer.MovementState.DRIVING || newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
                    currentCountBoarding += 1;
                }else if (newState == MovementAnalyzer.MovementState.WALKING || newState == MovementAnalyzer.MovementState.STOPPED) {
                    if (zoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                        currentCountDropping += 1;
                    }
                }
            }
            for (int i = 0; i < lastStates.size() - 1; i++) {
                MovementAnalyzer.MovementState newLastState = (MovementAnalyzer.MovementState) lastStates.get(i);
                if (newLastState == MovementAnalyzer.MovementState.DRIVING || newLastState == MovementAnalyzer.MovementState.LOW_DRIVING) {
                    lastCountDropping += 1;
                } else if (newLastState == MovementAnalyzer.MovementState.WALKING || newLastState == MovementAnalyzer.MovementState.STOPPED) {
                    if (zoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                        lastCountBoarding += 1;
                    }
                }
            }
        }
        // 下车
        if (currentCountBoarding>4 && lastCountBoarding>4) {
            result = Event.ARRIVED_BOARDING;
        } else if (currentCountDropping>4 && lastCountDropping>4) {
            if (zoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                result = Event.ARRIVED_DROPPING;
            }
        }
        lastStates = (List<MovementAnalyzer.MovementState>) states;
        return result;
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
