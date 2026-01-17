package com.jwzt.modules.experiment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "experiment.base")
@Data
public class BaseConfig {
    // 功能开关
    private boolean developEnvironment = true;
    private boolean stayVerify;     // 是否进行停留点验证
    private boolean rangingVerify;  // 行为识别以测距为第一优先级
    private boolean logEnabled;     // 是否输出日志
    private boolean pushData;       // 是否推送数据
    // 调试使用配置
    private boolean outputShp;      // 是否输出定位点shp文件
    private String outputShpPath;      // 输出shp文件路径
    private String deleteDatetime;     // 数据删除时间

    private String yardName;    // 货场名称

    // 定位卡类型
    private String locationCardType;
    // 定位卡类型
    private String cardType;
    // 定位数据来源
    private String locateDataSources;

    private Joysuch joysuch = new Joysuch();
    private swCenter swCenter = new swCenter();
    private DataMatch dataMatch = new DataMatch();
    private CardAnalysis cardAnalysis = new CardAnalysis();

    @Data
    public static class DataMatch {
        private String timeIntervalSeconds;
        private boolean ignoreMatched;
        private boolean updateMatchStatus;
        private boolean saveRfidData;

    }

    @Data
    public class Joysuch {
        private String username;
        private String password;
        private String licence;
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

    @Data
    public class swCenter {
        private long tenantId;
    }

    @Data
    public class CardAnalysis {
        private String pushIp;
        private String vehicxleEntryExit;
        private String assignmentRecord;
        private String vehicleTrack;
        private String beaconPush;
        private String removeVehicle;
        private VisualIdentify visualIdentify = new VisualIdentify();

        @Data
        public class VisualIdentify {
            private String baseUrl;
            private List<String> cameraIds = new ArrayList<>();
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
