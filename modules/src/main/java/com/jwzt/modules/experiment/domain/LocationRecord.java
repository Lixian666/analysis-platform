package com.jwzt.modules.experiment.domain;

import lombok.Data;

@Data
public class LocationRecord {
    private Long driverId;
    private long timestamp;
    private double x;
    private double y;
    private String state;
    private String event;
}
