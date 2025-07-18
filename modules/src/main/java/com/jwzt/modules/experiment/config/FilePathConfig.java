package com.jwzt.modules.experiment.config;

import java.util.*;

public class FilePathConfig {

    public static final String MINHANG = "minhang";
    public static final String YUZUI = "yuzui";

    public static final String RTK = "rtk";
    public static final String OTHER = "other";

    // 货场区域
    private static final Map<String, List<String>> DRIVING_ZONE_PATHS = new HashMap<>();
    // 停车区域
    private static final Map<String, List<String>> PARKING_ZONE_PATHS = new HashMap<>();
    // 道路区域
    private static final Map<String, List<String>> ROAD_ZONE_PATHS = new HashMap<>();
    // 货运线区域
    private static final Map<String, List<String>> HUOYUNXIN_ZONE_PATHS = new HashMap<>();

    static {
        // 闵行配置
        DRIVING_ZONE_PATHS.put(MINHANG, Arrays.asList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp",
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp",
                "D:\\work\\data\\shp\\minhang\\货运线\\huoyunxin.shp"
        ));
        PARKING_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp"
        ));
        ROAD_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp"
        ));
        HUOYUNXIN_ZONE_PATHS.put(MINHANG, Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\货运线\\huoyunxin.shp"
        ));

        // 鱼嘴配置（根据你具体路径再补充）
        DRIVING_ZONE_PATHS.put(YUZUI, Arrays.asList(
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴道路\\daolu.shp",
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴库区\\kuqu.shp",
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴货运线\\huoyunxian.shp"
        ));
        PARKING_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴库区\\kuqu.shp"
        ));
        ROAD_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴道路\\daolu.shp"
        ));
        HUOYUNXIN_ZONE_PATHS.put(YUZUI, Collections.singletonList(
                "D:\\work\\data\\shp\\yuzui\\20250630鱼嘴货运线\\huoyunxian.shp"
        ));
    }

    public static List<String> getDrivingZonePaths(String yard) {
        return DRIVING_ZONE_PATHS.getOrDefault(yard, Collections.emptyList());
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
}
