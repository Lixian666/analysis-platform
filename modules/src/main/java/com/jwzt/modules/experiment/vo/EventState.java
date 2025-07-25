package com.jwzt.modules.experiment.vo;

import com.jwzt.modules.experiment.domain.BoardingDetector;
import lombok.Data;

@Data
public class EventState {
    public BoardingDetector.Event event = BoardingDetector.Event.NONE;
    public String acceptTime = null;
    public Long timestamp = 0L;
    public Integer state = 0; // 0: 默认 1: 超时 2: 两个识别点时间间隔过短

    public EventState(BoardingDetector.Event currentEvent, Long timestamp) {
        this.event = currentEvent;
        this.timestamp = timestamp;
    }

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, Integer state) {
        this.event = currentEvent;
        this.timestamp = timestamp;
        this.state = state;
    }

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, String acceptTime) {
        this.event = currentEvent;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
    }

    public EventState() {

    }
}
