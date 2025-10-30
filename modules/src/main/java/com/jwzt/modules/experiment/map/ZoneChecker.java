package com.jwzt.modules.experiment.map;

import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import org.geotools.data.simple.SimpleFeatureIterator;

/**
 * 区域检查器
 */
@Component
public class ZoneChecker {

    @Autowired
    private FilePathConfig filePathConfig;
    private String yard; // 当前货场，比如 minhang、yuzui
    private final Map<String, List<ZoneFeature>> zoneGeometries = new HashMap<>();

    public ZoneChecker() {
    }

    public ZoneChecker(FilePathConfig filePathConfig) {
        this.filePathConfig = filePathConfig;
        this.yard = filePathConfig.getYardName();
        loadZoneGeometries();
    }

    @PostConstruct
    public void init() {
        System.out.println("ZoneChecker 初始化");
        System.out.println("filePathConfig = " + filePathConfig); // 这里不再是 null
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
        List<String> types = Arrays.asList("driving", "parking2road", "kuqu", "huoyunxin",
                "parking", "road", "huoyunxinzyt", "huoyunxinjc", "banche", "zikuqu");
        for (String type : types) {
            List<String> paths = filePathConfig.getZonePaths(yard, type);
            List<ZoneFeature> featureList = new ArrayList<>();
            for (String path : paths) {
                try {
                    org.geotools.data.shapefile.ShapefileDataStore store = (org.geotools.data.shapefile.ShapefileDataStore) org.geotools.data.FileDataStoreFinder.getDataStore(new java.io.File(path));
                    if (store == null) continue;
                    org.geotools.data.simple.SimpleFeatureSource featureSource = store.getFeatureSource();
                    try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                        while (features.hasNext()) {
                            org.opengis.feature.simple.SimpleFeature feature = features.next();
                            Object geomObj = feature.getDefaultGeometry();
                            if (geomObj instanceof Geometry) {
                                Map<String, Object> attrs = new HashMap<>();
                                for (org.opengis.feature.Property p : feature.getProperties()) {
                                    if (p.getName() != null) attrs.put(p.getName().toString(), p.getValue());
                                }
                                featureList.add(new ZoneFeature((Geometry) geomObj, attrs));
                            }
                        }
                    }
                    store.dispose();
                } catch(Exception e) {
                    System.err.println("读取shp属性失败：" + path);
                    e.printStackTrace();
                }
            }
            zoneGeometries.put(type, featureList);
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
        List<ZoneFeature> features = zoneGeometries.getOrDefault(type, Collections.emptyList());
        org.locationtech.jts.geom.Point point = new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(p.getLongitude(), p.getLatitude()));
        for (ZoneFeature feature : features) {
            if (feature.getGeometry().covers(point)) {
                return true;
            }
        }
        return false;
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

    /**
     * 是否在货场板车作业区域
     */
    public boolean isInBanCheZone(LocationPoint p) {
        return isInZone(p, "banche");
//        return GeoUtils.isInsideGeometry(new Coordinate(p.getLongitude(), p.getLatitude()), zoneGeometries.get("huoyunxinjc"));
    }

    /**
     * 根据点坐标和区域类型，返回命中区域的属性字段值
     * @param p 点
     * @param type 区域类型（如kuqu、zikuqu等）
     * @param fieldName 区块属性字段名（如name/zname等）
     * @return 字段值，未命中返回null
     */
    public String getZoneFieldByPoint(LocationPoint p, String type, String fieldName) {
        List<ZoneFeature> list = zoneGeometries.get(type);
        if (list == null) return null;
        org.locationtech.jts.geom.Point point = new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(p.getLongitude(), p.getLatitude()));
        for (ZoneFeature feature : list) {
            if (feature.getGeometry().covers(point)) {
                Object val = feature.getAttributes().get(fieldName);
                return val == null ? null : val.toString();
            }
        }
        return null;
    }

    /**
     * 区域面及属性
     */
    public static class ZoneFeature {
        private final Geometry geometry;
        private final Map<String, Object> attributes;

        public ZoneFeature(Geometry geometry, Map<String, Object> attributes) {
            this.geometry = geometry;
            this.attributes = attributes;
        }
        public Geometry getGeometry() {
            return geometry;
        }
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
