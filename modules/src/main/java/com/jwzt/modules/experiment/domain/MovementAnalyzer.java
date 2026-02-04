package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.utils.GeoUtils;

import java.util.List;

/**
 * 移动状态分析器
 */
public class MovementAnalyzer {

    public enum MovementState {
        WALKING,      // 步行        速度：0.3 ~ 2.0 m/s
        RUNNING,      // 小跑、快走   速度：2.0 ~ 3.0 m/s
        LOW_DRIVING,  // 低速行驶     速度：3.0 ~ 7.0 m/s
        DRIVING,      // 正常行驶     速度：7.0 ~ 16+ m/s
        STOPPED       // 停止        速度：0 ~ 0.2 m/s
    }
    // 判断状态（传入最近N个点）
    public static MovementState analyzeState(List<LocationPoint> window) {
        if (window.size() < 2){
            if (window.size() == 1){Double avgSpeed =window.get(0).getSpeed();
                if (FilterConfig.MIN_SPEED_MPS <= avgSpeed && avgSpeed <= FilterConfig.MIN_WALKING_SPEED) return MovementState.STOPPED;
                else if (FilterConfig.MIN_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_WALKING_SPEED) return MovementState.WALKING;
                else if (FilterConfig.MAX_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_RUNING_SPEED) return MovementState.RUNNING;
                else if (FilterConfig.MAX_RUNING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_LOW_DRIVING_SPEED) return MovementState.LOW_DRIVING;
                else return MovementState.DRIVING;
            }else return MovementState.STOPPED;
        }

        double totalDist = 0;
        long totalTime = 0;

        for (int i = 1; i < window.size(); i++) {
            LocationPoint p1 = window.get(i - 1);
            LocationPoint p2 = window.get(i);
            totalDist += p1.distanceTo(p2);
            totalTime += (p2.getTimestamp() - p1.getTimestamp());
        }

        double avgSpeed = totalTime > 0 ? (totalDist / (totalTime / 1000.0)) : 0;

        if (FilterConfig.MIN_SPEED_MPS <= avgSpeed && avgSpeed <= FilterConfig.MIN_WALKING_SPEED) return MovementState.STOPPED;
        else if (FilterConfig.MIN_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_WALKING_SPEED) return MovementState.WALKING;
        else if (FilterConfig.MAX_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_RUNING_SPEED) return MovementState.RUNNING;
        else if (FilterConfig.MAX_RUNING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_LOW_DRIVING_SPEED) return MovementState.LOW_DRIVING;
        else return MovementState.DRIVING;
    }

    public static MovementState observeState(
            List<LocationPoint> window,
            List<LocationPoint> areaWindow
    ) {

        if (window == null || window.isEmpty()) return MovementState.STOPPED;

        if (window.size() == 1) {
            return speedToState(window.get(0).getSpeed());
        }

        double totalDist = 0;
        long totalTime = 0;

        for (int i = 1; i < window.size(); i++) {
            LocationPoint p1 = window.get(i - 1);
            LocationPoint p2 = window.get(i);
            totalDist += p1.distanceTo(p2);
            totalTime += (p2.getTimestamp() - p1.getTimestamp());
        }

        double avgSpeed = totalTime > 0 ? totalDist / (totalTime / 1000.0) : 0;

        // ===== 改进后的几何增强判停 =====
        if (areaWindow != null && areaWindow.size() >= 3) {
            // 散列圆半径
            double radius = calcSpreadRadius(areaWindow);
            // 最大距离
            double maxDist = maxPairDistance(areaWindow);

            boolean veryLowSpeed = avgSpeed < 0.5;
            boolean lowSpeed = avgSpeed < 1.0;

            if (veryLowSpeed && radius < 1.0 && maxDist < 2.0) {
                return MovementState.STOPPED;
            }

            if (lowSpeed && radius < 2.0 && maxDist < 3.0) {
                return MovementState.STOPPED;
            }
        }

        // 最终还是速度主导
        return speedToState(avgSpeed);
    }


    private static MovementState speedToState(double speed) {

        if (FilterConfig.MIN_SPEED_MPS <= speed && speed <= FilterConfig.MIN_WALKING_SPEED)
            return MovementState.STOPPED;
        else if (FilterConfig.MIN_WALKING_SPEED < speed && speed <= FilterConfig.MAX_WALKING_SPEED)
            return MovementState.WALKING;
        else if (FilterConfig.MAX_WALKING_SPEED < speed && speed <= FilterConfig.MAX_RUNING_SPEED)
            return MovementState.RUNNING;
        else if (FilterConfig.MAX_RUNING_SPEED < speed && speed <= FilterConfig.MAX_LOW_DRIVING_SPEED)
            return MovementState.LOW_DRIVING;
        else
            return MovementState.DRIVING;
    }

    // 几何计算
    private static double calcSpreadRadius(List<LocationPoint> pts) {

        if (pts == null || pts.isEmpty()) return 0;

        // ===== 1. 计算轨迹质心（几何中心）=====
        double centerLon = 0;
        double centerLat = 0;

        for (LocationPoint p : pts) {
            centerLon += p.getLongitude();
            centerLat += p.getLatitude();
        }

        centerLon /= pts.size();
        centerLat /= pts.size();

        // ===== 2. 计算所有点到中心的最远距离 =====
        double maxDist = 0;

        Coordinate center = new Coordinate(centerLon, centerLat);

        for (LocationPoint p : pts) {
            Coordinate c = new Coordinate(p.getLongitude(), p.getLatitude());
            double dist = GeoUtils.distanceM(center, c);
            maxDist = Math.max(maxDist, dist);
        }

        return maxDist;  // 单位：米
    }


    private static double maxPairDistance(List<LocationPoint> pts) {

        if (pts == null || pts.size() < 2) return 0;

        double max = 0;

        for (int i = 0; i < pts.size(); i++) {
            Coordinate c1 = new Coordinate(pts.get(i).getLongitude(), pts.get(i).getLatitude());

            for (int j = i + 1; j < pts.size(); j++) {
                Coordinate c2 = new Coordinate(pts.get(j).getLongitude(), pts.get(j).getLatitude());
                double dist = GeoUtils.distanceM(c1, c2);
                max = Math.max(max, dist);
            }
        }
        return max;
    }



}
