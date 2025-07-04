package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.map.ZoneChecker;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下车识别器
 */
public class BoardingDetector {

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

    public Event updateState(List<LocationPoint> recordPoints){
        Event result = Event.NONE;;
        if (recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) return result;
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, 10);
        LocationPoint currentPoint = recordPoints.get(FilterConfig.RECORD_POINTS_SIZE / 2);
        List<LocationPoint> theLastTenPoints = recordPoints.subList(recordPoints.size() - 10, recordPoints.size());
        // 检测到达上车
        boolean isTheFreightLineArea = ZoneChecker.isInHuoyunxinZone(currentPoint);
        if (currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED && isTheFreightLineArea){
            System.out.println("⚠️ 检测到车辆已进入上车区域");
            // 判断之前的上下车状态
            if (!lastEvent.equals(Event.NONE)){
                // 到达上车点前后状态标签数量
                int arrivedFirstTag = 0;
                int arrivedLastTag = 0;
                // 判断到达上车点前10个点状态
                for (LocationPoint point : theFirstTenPoints){
                    if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                            || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                        arrivedFirstTag++;
                    }
                }
                // 判断到达上车点后10个点状态
                for (LocationPoint point : theLastTenPoints){
                    if (point.getState() == MovementAnalyzer.MovementState.WALKING
                            || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                            || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
                        arrivedLastTag++;
                    }
                }
                // 判断状态标签数量是否满足到达区域上车条件
                if (arrivedFirstTag >= FilterConfig.ARRIVED_BeforeUp_STATE_SIZE && arrivedLastTag >= FilterConfig.ARRIVED_AfterUp_STATE_SIZE) {
                    System.out.println("⚠️ 检测到已上车");
                    lastEvent = Event.ARRIVED_BOARDING;
                    currentEvent = Event.ARRIVED_BOARDING;
                    result = Event.ARRIVED_BOARDING;
                    return result;
                }
            }
        }
        if (lastEvent == Event.ARRIVED_BOARDING) {

        }

        return result;
    }
    public Event updateState(ArrayList<LocationPoint> window, MovementAnalyzer.MovementState newState) {
        Event result = Event.NONE;;

        if (lastState == MovementAnalyzer.MovementState.WALKING && newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
            result = Event.ARRIVED_BOARDING;
        } else if (lastState == MovementAnalyzer.MovementState.LOW_DRIVING && newState == MovementAnalyzer.MovementState.WALKING) {
            if (ZoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                result = Event.ARRIVED_DROPPING;
            }
        }

        lastState = currentState;
        currentState = newState;
        return result;
    }

    public <E> Event updateState(ArrayList<E> window, ArrayList<E> states) {
        Event result = Event.NONE;
        if (states.size() < 5) return result;
        int lastCountBoarding = 0;
        int currentCountBoarding = 0;
        int lastCountDropping = 0;
        int currentCountDropping = 0;
        if (lastStates.size() == 5) {
            for (int i = 0; i < states.size() - 1; i++) {
                MovementAnalyzer.MovementState newState = (MovementAnalyzer.MovementState) states.get(i);
                if (newState == MovementAnalyzer.MovementState.DRIVING || newState == MovementAnalyzer.MovementState.LOW_DRIVING) {
                    currentCountBoarding += 1;
                }else if (newState == MovementAnalyzer.MovementState.WALKING || newState == MovementAnalyzer.MovementState.STOPPED) {
                    if (ZoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                        currentCountDropping += 1;
                    }
                }
            }
            for (int i = 0; i < lastStates.size() - 1; i++) {
                MovementAnalyzer.MovementState newLastState = (MovementAnalyzer.MovementState) lastStates.get(i);
                if (newLastState == MovementAnalyzer.MovementState.DRIVING || newLastState == MovementAnalyzer.MovementState.LOW_DRIVING) {
                    lastCountDropping += 1;
                } else if (newLastState == MovementAnalyzer.MovementState.WALKING || newLastState == MovementAnalyzer.MovementState.STOPPED) {
                    if (ZoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
                        lastCountBoarding += 1;
                    }
                }
            }
        }
        // 下车
        if (currentCountBoarding>4 && lastCountBoarding>4) {
            result = Event.ARRIVED_BOARDING;
        } else if (currentCountDropping>4 && lastCountDropping>4) {
            if (ZoneChecker.isInParkingZone((LocationPoint) window.get(window.size() - 1))) {
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
