package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import lombok.Data;

@Data
public class LocationPoint {
    private Integer cardId;
    private String cardUUID;
    private Double longitude;
    private Double latitude;
    private String acceptTime;
    private Long timestamp;
    private Double speed;
    private Double thirdSpeed;
    private MovementAnalyzer.MovementState state;
    private MovementAnalyzer.MovementState distanceState;
    private String event;

    private TagScanUwbData tagScanUwbData;
//    private visualIdentityData visualIdentityData;

    private Boolean isStay = false;

    public LocationPoint(TagScanUwbData tagScanUwbData) {
        this.tagScanUwbData = tagScanUwbData;
    }

    public LocationPoint(Integer cardId, Double longitude, Double latitude, String acceptTime, Long timestamp) {
        this.cardId = cardId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
    }

    public LocationPoint(String cardId, Double longitude, Double latitude, String acceptTime, Long timestamp) {
        this.cardUUID = cardId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
    }

    public LocationPoint(String cardId, Double longitude, Double latitude, String acceptTime, Long timestamp, TagScanUwbData tagScanUwbData) {
        this.cardUUID = cardId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.tagScanUwbData = tagScanUwbData;
    }

    public LocationPoint(double avgX, double avgY, String acceptTime, Long timestamp, Double speed) {
        this.longitude = avgX;
        this.latitude = avgY;
        this.acceptTime = acceptTime;
        this.timestamp = timestamp;
        this.speed = speed;
    }

    public LocationPoint(Integer cardId, double longitude, double latitude) {
        this.cardId = cardId;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public LocationPoint() {

    }

    // 可扩展字段：速度、方向等
    public double distanceTo(LocationPoint other) {
        Coordinate coordinate1 = new Coordinate(this.longitude, this.latitude);
        Coordinate coordinate2 = new Coordinate(other.longitude, other.latitude);
        double distance =  GeoUtils.distanceM(coordinate1, coordinate2);
        return distance;
    }

    // 计算两点间速度（米/秒）
    public double speedTo(LocationPoint other) {
        double distance = this.distanceTo(other);
        double timeDiff = Math.abs(this.timestamp - other.timestamp) / 1000.0;
        return timeDiff > 0 ? distance / timeDiff : 0;
    }
}
