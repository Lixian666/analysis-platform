package com.jwzt.modules.experiment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "experiment.base")
@Data
public class BaseConfig {
    // 功能开关
    private boolean stayVerify;     // 是否进行停留点验证
    // 调试使用配置
    private boolean outputShp;      // 是否输出定位点shp文件
    private String outputShpPath;      // 输出shp文件路径
    private String deleteDatetime;     // 数据删除时间

    // 定位卡类型
    private String locationCardType;
    // 定位卡类型
    private String cardType;

    private Joysuch joysuch = new Joysuch();

    @Data
    public class Joysuch {
        private String username;
        private String password;
        private String baseUrl;
        private Api api = new Api();
        private String buildingName;
        private String buildingId;

        @Data
        public class Api {
            private String buildList;
            private String cards;
            private String beacons;
            private String points;
            private String subscribe;
            private String unsubscribe;
            private String tagScanUwbHistory;
        }
    }

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





    // 定位卡配置
    public static final String RTK = "rtk";
    public static final String ZHENQU = "zhenqu";
    public static final String OTHER = "other";
    public static final String LOCATION_CARD_TYPE = OTHER;
    public static final String BAN_CAR = "bancar";
    public static final String CAR = "car";
    public static final String CARD_TYPE = CAR;

}
