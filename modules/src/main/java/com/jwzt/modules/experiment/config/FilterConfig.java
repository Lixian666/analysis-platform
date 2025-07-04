package com.jwzt.modules.experiment.config;

public class FilterConfig {
    public static final double MAX_SPEED_MPS = 22.0; // 最大速度，单位 m/s（比如步行 < 2，驾驶 < 17）
    public static final double MIN_SPEED_MPS = 0.0; // 最小速度，单位 m/s（比如步行 < 2，驾驶 < 17）
    public static final double MIN_WALKING_SPEED = 0.3; // 最小步行速度，单位 m/s
    public static final double MAX_WALKING_SPEED = 2.0; // 最大速度，单位 m/s
    public static final double MAX_RUNING_SPEED = 3.0; // 最大小跑速度，单位 m/s
    public static final double MAX_LOW_DRIVING_SPEED = 7.0; // 最大低速驾驶速度，单位 m/s
    public static final double MAX_JUMP_DISTANCE = 20.0; // 每点最大跳变距离（米）
    public static final int MIN_TIME_INTERVAL_MS = 1000; // 最小时间间隔 （毫秒）

    public static final int WINDOW_SIZE = 5; // 数据缓存大小（判断5组连续点位平均数）
    public static final int ARRIVED_BeforeUp_STATE_SIZE = 7;
    public static final int ARRIVED_AfterUp_STATE_SIZE = 7;
    public static final int WINDOW_STATE_SIZE = 2;
    public static final int RECORD_POINTS_SIZE = 21;

    // 最大合理速度（m/s），按120km/h计算
    public static final double MAX_SPEED = 120 * 1000 / 3600.0;
    // 距离阈值（米），超过则视为漂移
    public static final double DISTANCE_THRESHOLD = MAX_SPEED * 1.5;
    // 基础距离阈值（米）
    public static final double BASE_DISTANCE_THRESHOLD = 50.0;
    // 最大合理加速度（m/s²）
    public static final double MAX_ACCELERATION = 5.0;
}
