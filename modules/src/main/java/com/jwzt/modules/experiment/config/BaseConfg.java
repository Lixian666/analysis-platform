package com.jwzt.modules.experiment.config;

public class BaseConfg {
    // 功能开关
    public static final boolean IS_STAY_VERIFY = false;         // 是否进行停留点验证

    // 调试使用配置
    public static final boolean IS_OUTPUT_SHP = false;       // 是否输出定位点shp文件
    public static final String OUTPUT_SHP_PATH = "D:/PlatformData/output";        // 输出shp文件路径
    public static final String DELETE_DATETIME = "2025-08-07 14:00:00";     // 数据删除时间

    // 定位卡配置
    public static final String RTK = "rtk";
    public static final String ZHENQU = "zhenqu";
    public static final String OTHER = "other";
    public static final String LOCATION_CARD_TYPE = OTHER;
    public static final String BAN_CAR = "bancar";
    public static final String CAR = "car";
    public static final String CARD_TYPE = CAR;

    // 真趣定位服务
//    public static final String USER_NAME = "18911091136";
//    public static final String PASSWORD = "3W.163.com";
//    public static final String BASE_URL = "https://api.joysuch.com:46000";
    public static final String USER_NAME = "client";
    public static final String PASSWORD = "JoySuch@client10";
    public static final String BASE_URL = "http://61.50.136.180:46000";
    public static final String GET_BUILDLIST_URL = BASE_URL + "/api/v1/buildList";
    public static final String GET_CARDS_URL = BASE_URL + "/api/v4/device/blts";
    public static final String GET_POINTS_URL = BASE_URL + "/api/v1/datacenter/historyPath";
    public static final String SUBSCRIBE_URL = BASE_URL + "/api/v2/subscribe/http/subscribe";
    public static final String UNSUBSCRIBE_URL = BASE_URL + "/api/v2/subscribe/http/unsubscribe";

}
