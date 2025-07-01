package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.map.ZoneChecker;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下车识别器
 */
public class BoardingDetector {
    private List<MovementAnalyzer.MovementState> lastStates = new ArrayList<>();
    private MovementAnalyzer.MovementState lastState = MovementAnalyzer.MovementState.STOPPED;
    private MovementAnalyzer.MovementState currentState = MovementAnalyzer.MovementState.STOPPED;

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

    public enum Event {
        NONE,
        ARRIVED_BOARDING,           // 到达上车
        ARRIVED_DROPPING,           // 到达下车
        SEND_BOARDING,           // 发运上车
        SEND_DROPPING,           // 发运下车
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
