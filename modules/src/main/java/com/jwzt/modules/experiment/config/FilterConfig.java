package com.jwzt.modules.experiment.config;

public class FilterConfig {


    // 数据获取配置（需比RECORD_POINTS_SIZE大）
    public static final int POINTS_SIZE = 60;
    public static final int RECORD_POINTS_SIZE = 51;        // 上下车识别窗口大小


    // 行为识别配置
        // 感应距离配置
        public static final double SENSING_DISTANCE_THRESHOLD = 1.0;  // 靠近感应距离阈值（m）
        public static final int SEND_AFTER_DOWN_UWB_SIZE = 2;

        // 行为识别数配置
        public static final int MIN_POINT_ANALYSIS_COUNT = 60;

        // 速度配置
        public static final double MAX_SPEED_MPS = 22.0; // 最大速度，单位 m/s（比如步行 < 2，驾驶 < 17）
        public static final double MIN_SPEED_MPS = 0.0; // 最小速度，单位 m/s（比如步行 < 2，驾驶 < 17）
        public static final double MIN_WALKING_SPEED = 0.7; // 最小步行速度，单位 m/s
        public static final double MAX_WALKING_SPEED = 2.0; // 最大速度，单位 m/s
        public static final double MAX_RUNING_SPEED = 3.0; // 最大小跑速度，单位 m/s
        public static final double MAX_LOW_DRIVING_SPEED = 7.0; // 最大低速驾驶速度，单位 m/s
        public static final double MAX_JUMP_DISTANCE = 20.0; // 每点最大跳变距离（米）
        public static final int MIN_TIME_INTERVAL_MS = 1; // 最小时间间隔 （毫秒）
        public static final int WINDOW_STATE_SIZE = 2;      // 行为状态识别窗口大小


    // 时间间隔配置
    public static final int ADJACENT_POINTS_TIME_INTERVAL_MS = 600000;   // 识别时间间隔 （毫秒）  0：不进行时间间隔判定
    public static final int IDENTIFY_IDENTIFY_TIME_INTERVAL_MS = 10000;  // 上下车识别标签时间间隔 （毫秒） 0：不进行时间间隔判定
    public static final int SAME_STATE_IDENTIFY_TIME_INTERVAL_MS = 10000;  // 相同状态识别标签时间间隔 （毫秒） 0：不进行时间间隔判定
    public static final int BEACON_DISTANCE_EFFECTIVE_TIME_INTERVAL_MS = 60000; // 信标距离判断有效时间间隔
    public static final int SEND_AFTER_DOWN_TIME_THRESHOLD = 40000;  // 发运下车识别时间间隔 （毫秒）


    // 信标距离判定相关配置
        // 交通车状态标记数设置
        public static final int TRAFFICCAR_STATE_SIZE = 6;

        // A信标距离配置
        public static final int A_DISTANCE_STATE_SIZE = 2;

        // AB信标距离配置
        public static final int AB_DISTANCE_STATE_SIZE = 3;
        public static final int AB_IDENTIFY_DISTANCE_STATE_SIZE = 2;

        // AB信标距离、最后时间配置
        public static final int AB_DISTANCE_THRESHOLD = 2;
        public static final int AB_LAST_TIME_THRESHOLD = 2;


    // 上下车识别配置
    public static final int WINDOW_SIZE = 5; // 数据缓存大小

    public static final int CONTINUED_STOPPED_STATE_SIZE = 3;

    public static final int NUMBER = 3;
    public static final int NUMBER1 = 1;

    public static final int ARRIVED_BEFORE_UP_STATE_SIZE = NUMBER;        // 到达上车识别判定标签数
    public static final int ARRIVED_AFTER_UP_STATE_SIZE = NUMBER;     // 到达下车识别判定标签数
    public static final int ARRIVED_BEFORE_DOWN_STATE_SIZE = NUMBER;        // 到达上车识别判定标签数
    public static final int ARRIVED_AFTER_DOWN_STATE_SIZE = NUMBER;     // 到达下车识别判定标签数

    public static final int SEND_BEFORE_UP_STATE_SIZE = NUMBER;        // 发运上车识别判定标签数
    public static final int SEND_AFTER_UP_STATE_SIZE = NUMBER;     // 发运下车识别判定标签数
    public static final int SEND_BEFORE_DOWN_STATE_SIZE = NUMBER;        // 发运上车识别判定标签数
    public static final int SEND_AFTER_DOWN_STATE_SIZE = NUMBER;     // 发运下车识别判定标签数

    public static final int STOPPED_STATE_SIZE = 3;       // 停车识别判定标签数
    public static final int DRIVING_STATE_SIZE = 2;       // 行驶识别判定标签数


    // 停留检测配置
    public static final int STAY_WINDOW_SIZE = 5;       // 停留检测窗口大小
    public static final double STAY_RADIUS = 10.0;      // 停留区域半径（米）
    public static final long STAY_DURATION_THRESHOLD = 30 * 1000; // 最小停留时长（毫秒）


    // 运动检测配置
    public static final double MAX_SPEED = 120 * 1000 / 3600.0;         // 最大合理速度（m/s），按120km/h计算
    public static final double DISTANCE_THRESHOLD = MAX_SPEED * 1.5;            // 距离阈值（米），超过则视为漂移
    public static final double BASE_DISTANCE_THRESHOLD = 50.0;          // 基础距离阈值（米）
    public static final double MAX_ACCELERATION = 5.0;          // 最大合理加速度（m/s²）
}
