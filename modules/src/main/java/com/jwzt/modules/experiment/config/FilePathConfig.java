package com.jwzt.modules.experiment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "experiment.file-path")
@Data
public class FilePathConfig {

    private String yardName;
    private String basePath;

    /**
     * yard -> zoneType -> pathList
     * 例如：minhang -> driving -> [xxx.shp, yyy.shp]
     */
    private Map<String, Map<String, List<String>>> yards = new HashMap<>();

    public List<String> getZonePaths(String yard, String zoneType) {
        return yards.getOrDefault(yard, Collections.emptyMap())
                .getOrDefault(zoneType, Collections.emptyList());
    }

// 旧配置兼容之前的算法
    public static final String MINHANG = "minhang";
    public static final String YUZUI = "yuzui";
    public static final String HUOCHANG = YUZUI;
//    public static final String PATH = "";
    public static final String BASE_PATH = "D:/PlatformData/shp";

    // 货场区域
    private static final Map<String, List<String>> DRIVING_ZONE_PATHS = new HashMap<>();
    // 停车区域-道路区域
    private static final Map<String, List<String>> PARKING_2_ROAD_ZONE_PATHS = new HashMap<>();
    // 停车区域
    private static final Map<String, List<String>> PARKING_ZONE_PATHS = new HashMap<>();
    // 道路区域
    private static final Map<String, List<String>> ROAD_ZONE_PATHS = new HashMap<>();
    // 货运线区域
    private static final Map<String, List<String>> HUOYUNXIN_ZONE_PATHS = new HashMap<>();
    // 货运线区域-作业台
    private static final Map<String, List<String>> HUOYUNXINZYT_ZONE_PATHS = new HashMap<>();
    // 货运线区域-J车
    private static final Map<String, List<String>> HUOYUNXINJC_ZONE_PATHS = new HashMap<>();

    static {
        // 闵行配置
        DRIVING_ZONE_PATHS.put(MINHANG, Arrays.asList(
                BASE_PATH + "/minhang/行车道路/xingcdaolu.shp",
                BASE_PATH + "/minhang/20250624库区/kuqu.shp",
                BASE_PATH + "/minhang/货运线/huoyunxin.shp"
        ));
        PARKING_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                BASE_PATH + "/minhang/20250624库区/kuqu.shp"
        ));
        ROAD_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                BASE_PATH + "/minhang/行车道路/xingcdaolu.shp"
        ));
        HUOYUNXIN_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                BASE_PATH + "/minhang/货运线/huoyunxin.shp"
        ));

        // 鱼嘴配置（根据你具体路径再补充）
        DRIVING_ZONE_PATHS.put(YUZUI, Arrays.asList(
                BASE_PATH + "/yuzui/20250630鱼嘴道路/daolu.shp",
                BASE_PATH + "/yuzui/20250630鱼嘴库区/kuqu.shp",
                BASE_PATH + "/yuzui/20250630鱼嘴货运线/huoyunxian.shp"
        ));

        PARKING_2_ROAD_ZONE_PATHS.put(YUZUI, Arrays.asList(
                BASE_PATH + "/yuzui/20250630鱼嘴道路/daolu.shp",
                BASE_PATH + "/yuzui/20250630鱼嘴库区/kuqu.shp"
        ));
        PARKING_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                BASE_PATH + "/yuzui/20250630鱼嘴库区/kuqu.shp"
        ));
        ROAD_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                BASE_PATH + "/yuzui/20250630鱼嘴道路/daolu.shp"
        ));
        HUOYUNXIN_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                BASE_PATH + "/yuzui/20250630鱼嘴货运线/huoyunxian.shp"
        ));
        HUOYUNXINZYT_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                BASE_PATH + "/yuzui/20250630鱼嘴货运线作业台/huoyunxianzyt.shp"
        ));
        HUOYUNXINJC_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                BASE_PATH + "/yuzui/20250630鱼嘴货运线J车/huoyunxianjc.shp"
        ));
    }

    public static List<String> getDrivingZonePaths(String yard) {
        return DRIVING_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getParking2RoadZonePaths(String yard) {
        return PARKING_2_ROAD_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getParkingZonePaths(String yard) {
        return PARKING_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getRoadZonePaths(String yard) {
        return ROAD_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getHuoyunxinZonePaths(String yard) {
        return HUOYUNXIN_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getHuoyunxinZytZonePaths(String yard) {
        return HUOYUNXINZYT_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }

    public static List<String> getHuoyunxinJcZonePaths(String yard) {
        return HUOYUNXINJC_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
    }
}
