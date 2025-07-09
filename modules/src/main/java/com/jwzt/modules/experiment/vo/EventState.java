package com.jwzt.modules.experiment.vo;

import com.jwzt.modules.experiment.domain.BoardingDetector;
import lombok.Data;

@Data
public class EventState {
    public BoardingDetector.Event event = BoardingDetector.Event.NONE;
    public String acceptTime = null;
    public Long timestamp = 0L;

    public EventState(BoardingDetector.Event currentEvent, Long timestamp) {
        this.event = currentEvent;
        this.timestamp = timestamp;
    }

    public EventState() {

    }
}
