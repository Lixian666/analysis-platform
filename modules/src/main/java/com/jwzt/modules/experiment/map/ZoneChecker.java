package com.jwzt.modules.experiment.map;

import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;

import java.util.List;

/**
 * 区域检查器
 */
public class ZoneChecker {

    private final String yard; // 当前货场，比如 minhang、yuzui

    public ZoneChecker(String yard) {
        this.yard = yard;
    }

    /**
     * 是否在货场驾驶区域
     */
    public boolean isInDrivingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = FilePathConfig.getDrivingZonePaths(yard);
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    /**
     * 是否在货场停车区域
     */
    public boolean isInParking2RoadZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = FilePathConfig.getParking2RoadZonePaths(yard);
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    /**
     * 是否在货场货运线区域
     */
    public boolean isInHuoyunxinZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = FilePathConfig.getHuoyunxinZonePaths(yard);
        return GeoUtils.isInsideShp(newCoordinate, shpPaths, 1.0);
    }

    /**
     * 是否在货场停车区域
     */
    public boolean isInParkingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = FilePathConfig.getParkingZonePaths(yard);
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    /**
     * 是否在货场道路区域
     */
    public boolean isInRoadZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = FilePathConfig.getRoadZonePaths(yard);
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }
}
