package com.jwzt.modules.experiment.utils.geo;

public class CoordinateUtils {

    /**
     * 根据比例 alpha 对两个经纬度坐标进行线性插值
     *
     * @param lon1 上一个点的经度
     * @param lat1 上一个点的纬度
     * @param lon2 当前点的经度（异常点）
     * @param lat2 当前点的纬度
     * @param alpha 插值比例（0 ~ 1）
     * @return 插值后的 [经度, 纬度]
     */
    public static double[] interpolate(double lon1, double lat1, double lon2, double lat2, double alpha) {
        alpha = Math.max(0, Math.min(1, alpha)); // 限制范围在[0, 1]
        double lon = lon1 + alpha * (lon2 - lon1);
        double lat = lat1 + alpha * (lat2 - lat1);
        return new double[]{lon, lat};
    }

    /**
     * 根据最大速度和时间差插值比例
     *
     * @param maxSpeed 最大允许速度（单位：m/s）
     * @param timeDiff 时间差（毫秒）
     * @param distance 实际两点之间距离（单位：m）
     * @return 插值比例 alpha（限制在0~1之间）
     */
    public static double computeAlpha(double maxSpeed, long timeDiff, double distance) {
        if (distance == 0) return 0;
        double maxDistance = maxSpeed * (timeDiff / 1000.0);
        return Math.min(1.0, maxDistance / distance);
    }
}
