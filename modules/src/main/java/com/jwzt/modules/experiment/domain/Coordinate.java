package com.jwzt.modules.experiment.domain;

import lombok.Data;

@Data
public class Coordinate {
    private double longitude;
    private double latitude;
    private long timestamp;

    public Coordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Coordinate(double longitude, double latitude, long timestamp) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
    }
}
