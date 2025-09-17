package com.jwzt.modules.experiment.utils.third.zq.domain;

import lombok.Data;

import java.util.List;

@Data
public class TagScanUwbData {
    private String type;
    private Long time;
    private String dateTime;
    private String bltMac;
    private Integer battery;
    private Integer motionStatus;
    private GnssInfo gnssInfo;
    private List<BltScanUwbBeacon> uwbBeaconList;

    @Data
    public static class GnssInfo {
        private Integer gnssState;
        private Integer noiseCarrier;
        private Integer starNum;
        private Double speedKmh;
    }

    @Data
    public static class BltScanUwbBeacon {
        private String uwbBeaconMac;
        private Double distance;
    }
}
