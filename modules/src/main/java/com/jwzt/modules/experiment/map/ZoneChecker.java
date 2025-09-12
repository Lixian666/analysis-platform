package com.jwzt.modules.experiment.map;

import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 区域检查器
 */
@Component
public class ZoneChecker {

    @Autowired
    private FilePathConfig filePathConfig;
    private String yard; // 当前货场，比如 minhang、yuzui
    private final Map<String, List<Geometry>> zoneGeometries = new HashMap<>();

    public ZoneChecker(FilePathConfig filePathConfig) {
        this.filePathConfig = filePathConfig;
        this.yard = filePathConfig.getYardName();
        loadZoneGeometries();
    }

//    public ZoneChecker(FilePathConfig filePathConfig, @Value("${experiment.default-yard:minhang}") String yard) {
//        this.filePathConfig = filePathConfig;
//        this.yard = yard;
//        loadZoneGeometries();
//    }

    public void switchYard(String yard) {
        this.yard = yard;
        reloadZoneGeometries();
    }

    public void reloadZoneGeometries() {
        zoneGeometries.clear();
        loadZoneGeometries();
    }

    /**
     * 加载区域
     */
    private void loadZoneGeometries() {
        List<String> types = Arrays.asList("driving", "parking2road", "huoyunxin",
                "parking", "road", "huoyunxinzyt", "huoyunxinjc");

        for (String type : types) {
            List<String> paths = filePathConfig.getZonePaths(yard, type);
            zoneGeometries.put(type, GeoUtils.loadGeometries(paths));
        }
    }
//    private void loadZoneGeometries() {
//        zoneGeometries.put("driving", GeoUtils.loadGeometries(FilePathConfig.getDrivingZonePaths(yard)));
//        zoneGeometries.put("parking2road", GeoUtils.loadGeometries(FilePathConfig.getParking2RoadZonePaths(yard)));
//        zoneGeometries.put("huoyunxin", GeoUtils.loadGeometries(FilePathConfig.getHuoyunxinZonePaths(yard)));
//        zoneGeometries.put("parking", GeoUtils.loadGeometries(FilePathConfig.getParkingZonePaths(yard)));
//        zoneGeometries.put("road", GeoUtils.loadGeometries(FilePathConfig.getRoadZonePaths(yard)));
//        zoneGeometries.put("huoyunxinzyt", GeoUtils.loadGeometries(FilePathConfig.getHuoyunxinZytZonePaths(yard)));
//        zoneGeometries.put("huoyunxinjc", GeoUtils.loadGeometries(FilePathConfig.getHuoyunxinJcZonePaths(yard)));
//    }

    public boolean isInZone(LocationPoint p, String type) {
        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()),
                zoneGeometries.getOrDefault(type, Collections.emptyList()));
    }
    /**
     * 是否在货场驾驶区域
     */
    public boolean isInDrivingZone(LocationPoint p) {
        return isInZone(p, "driving");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("driving"));
    }

    /**
     * 是否在货场停车区域和道路区域
     */
    public boolean isInParking2RoadZone(LocationPoint p) {
        return isInZone(p, "parking2road");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("parking2road"));
    }

    /**
     * 是否在货场货运线区域
     */
    public boolean isInHuoyunxinZone(LocationPoint p) {
        return isInZone(p, "huoyunxin");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("huoyunxin"));
    }

    /**
     * 是否在货场停车区域
     */
    public boolean isInParkingZone(LocationPoint p) {
        return isInZone(p, "parking");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("parking"));
    }

    /**
     * 是否在货场道路区域
     */
    public boolean isInRoadZone(LocationPoint p) {
        return isInZone(p, "road");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("road"));
    }

    /**
     * 是否在货场货运线-作业台区域
     */
    public boolean isInHuoyunxinZytZone(LocationPoint p) {
        return isInZone(p, "huoyunxinzyt");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("huoyunxinzyt"));
    }

    /**
     * 是否在货场货运线-J车区域
     */
    public boolean isInHuoyunxinJcZone(LocationPoint p) {
        return isInZone(p, "huoyunxinjc");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("huoyunxinjc"));
    }

//    public ZoneChecker(String yard) {
//        this.yard = yard;
//    }
//
//    /**
//     * 是否在货场驾驶区域
//     */
//    public boolean isInDrivingZone(LocationPoint p) {
//        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
//        List<String> shpPaths = FilePathConfig.getDrivingZonePaths(yard);
//        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
//    }
//
//    /**
//     * 是否在货场停车区域
//     */
//    public boolean isInParking2RoadZone(LocationPoint p) {
//        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
//        List<String> shpPaths = FilePathConfig.getParking2RoadZonePaths(yard);
//        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
//    }
//
//    /**
//     * 是否在货场货运线区域
//     */
//    public boolean isInHuoyunxinZone(LocationPoint p) {
//        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
//        List<String> shpPaths = FilePathConfig.getHuoyunxinZonePaths(yard);
//        return GeoUtils.isInsideShp(newCoordinate, shpPaths, 0.0);
//    }
//
//    /**
//     * 是否在货场停车区域
//     */
//    public boolean isInParkingZone(LocationPoint p) {
//        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
//        List<String> shpPaths = FilePathConfig.getParkingZonePaths(yard);
//        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
//    }
//
//    /**
//     * 是否在货场道路区域
//     */
//    public boolean isInRoadZone(LocationPoint p) {
//        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
//        List<String> shpPaths = FilePathConfig.getRoadZonePaths(yard);
//        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
//    }
}
