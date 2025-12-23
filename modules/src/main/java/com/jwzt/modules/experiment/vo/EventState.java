package com.jwzt.modules.experiment.vo;

import com.jwzt.modules.experiment.domain.BoardingDetector;
import lombok.Data;

@Data
public class EventState {
    public BoardingDetector.Event event = BoardingDetector.Event.NONE;
    public String acceptTime = null;
    public Long timestamp = 0L;
    public Double lang = null;
    public Double lat = null;
    public Integer newEventState = 0; // 0: 默认 1: 新的上车点 2: 到达变发运生成新的上下车点 3: 发运变到达生成新的上下车点
    public Integer state = 0; // 0: 默认 1: 超时 2: 两个识别点时间间隔过短
    public String zoneName = null;
    public String zoneNameRfid = null;
    public String zone = null;

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

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, String acceptTime, Double lang, Double lat) {
        this.event = currentEvent;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.lang = lang;
        this.lat = lat;
    }

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, String acceptTime, Double lang, Double lat, Integer newEventState) {
        this.event = currentEvent;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.lang = lang;
        this.lat = lat;
        this.newEventState = newEventState;
    }

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, String acceptTime, Double lang, Double lat, Integer state, Integer newEventState) {
        this.event = currentEvent;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.lang = lang;
        this.lat = lat;
        this.state = state;
        this.newEventState = newEventState;
    }

    public EventState(BoardingDetector.Event currentEvent, Long timestamp, String acceptTime, Double lang, Double lat, String zoneName, String zoneNameRfid, String zone) {
        this.event = currentEvent;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.lang = lang;
        this.lat = lat;
        this.zoneName = zoneName;
        this.zoneNameRfid = zoneNameRfid;
        this.zone = zone;
    }

    public EventState() {

    }
}
