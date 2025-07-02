package com.jwzt.modules.experiment.filter;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.geo.CoordinateUtils;

import java.util.*;


/**
 * 坐标点滤波器
 */
public class OutlierFilter {
    private final int windowSize = 5;    // 滑动窗口大小必须为奇数
    private LocationPoint lastPoint = null;
    private final Deque<LocationPoint> history = new ArrayDeque<>();
    private static final double MAX_DEVIATION_SPEEDUP = 7.0;  //允许的偏差
    private static final double ANGLE_THRESHOLD = 150.0; //角度阈值

    public int isValid(LocationPoint newPoint) {
        if (lastPoint == null) {
            lastPoint = newPoint;
            return 0; // 第一个点不处理
        }
        Coordinate newCoordinate = new Coordinate(newPoint.getLongitude(), newPoint.getLatitude());
        Coordinate lastCoordinate = new Coordinate(lastPoint.getLongitude(), lastPoint.getLatitude());

        double distance = GeoUtils.distanceM(newCoordinate, lastCoordinate);
        long timeDiff = newPoint.getTimestamp() - lastPoint.getTimestamp();

//         时间间隔太小
        if (timeDiff < FilterConfig.MIN_TIME_INTERVAL_MS) {
            lastPoint = newPoint;
            return 1;
        }

        if (!ZoneChecker.isInDrivingZone(newPoint)){
            lastPoint = newPoint;
            return 3;
        }
        double speed = distance / (timeDiff / 1000.0); // m/s
        newPoint.setSpeed(speed);
        // 速度过大 or 跳跃距离过远
        if (speed > FilterConfig.MAX_SPEED_MPS || distance > FilterConfig.MAX_JUMP_DISTANCE) {
            lastPoint = newPoint;
            return 2;
        }
        lastPoint = newPoint;
        return 0;
    }

    public List<LocationPoint> fixTheData(List<LocationPoint> newPoints) {
        List<LocationPoint> newLocationPoints = new ArrayList<>();
        for (LocationPoint newPoint : newPoints){
            if (lastPoint == null) {
                lastPoint = newPoint;
                history.addLast(newPoint);
                continue;
            }
            if (history.size() < ((windowSize / 2) + 1)){
                newLocationPoints.add(newPoint);
            }
            long timeDiff = (newPoint.getTimestamp() - lastPoint.getTimestamp());
            if (!ZoneChecker.isInDrivingZone(newPoint)){
//                lastPoint = newPoint;
                System.out.println("⚠️  区域异常定位点已剔除：" + newPoint);
                continue;
            }
            // 时间间隔太小
            if (timeDiff < FilterConfig.MIN_TIME_INTERVAL_MS || Objects.equals(newPoint.getTimestamp(), lastPoint.getTimestamp())) {
                lastPoint = newPoint;
                history.removeLast();
                System.out.println("⚠️  时间间隔异常定位点已更新最新点位：" + newPoint);
            }
            history.addLast(newPoint);
            if (history.size() < windowSize) {
                continue;
            }
            if (history.size() > windowSize) {
                history.removeFirst();
            }
            // 计算中间索引
            int middleIndex = history.size() / 2;
            List<LocationPoint> window = new ArrayList<>(history);
            LocationPoint firstPoint = window.get(middleIndex - 2);
            LocationPoint middleBeforePoint = window.get(middleIndex - 1);
            LocationPoint middlePoint = window.get(middleIndex);
            LocationPoint middleAfterPoint = window.get(middleIndex + 1);
            LocationPoint finalPoint = window.get(middleIndex + 2);
            // 速度跳变判断
            double v01 = speed(firstPoint, middleBeforePoint);
            double v12 = speed(middleBeforePoint, middlePoint);
            double v23 = speed(middlePoint, middleAfterPoint);
            double v34 = speed(middleAfterPoint, finalPoint);
            if (Math.abs(v01 - v12) > FilterConfig.MAX_SPEED_MPS) {
                double medianSpeed = FilterConfig.MAX_SPEED_MPS;
                if (v01 > (v12 + v23 + v34)){
                    List<Double> speeds = new ArrayList<>();
                    speeds.add(v12);
                    speeds.add(v23);
                    speeds.add(v34);
                    Collections.sort(speeds);
                    medianSpeed = speeds.get(1);
                }
                double distance = GeoUtils.distanceM(new Coordinate(middleBeforePoint.getLongitude(), middleBeforePoint.getLatitude()), new Coordinate(middleAfterPoint.getLongitude(), middleAfterPoint.getLatitude()));
                long time = middleAfterPoint.getTimestamp() - middleBeforePoint.getTimestamp();
                double alpha = CoordinateUtils.computeAlpha(medianSpeed, time, distance);
                double[] corrected = CoordinateUtils.interpolate(
                        middleBeforePoint.getLongitude(), middleBeforePoint.getLatitude(),
                        middleAfterPoint.getLongitude(), middleAfterPoint.getLatitude(),
                        alpha
                );
                LocationPoint newMiddlePoint = middlePoint;
                newMiddlePoint.setLongitude(corrected[0]);
                newMiddlePoint.setLatitude(corrected[1]);
                newMiddlePoint.setSpeed(speed(middleBeforePoint,middlePoint));
                System.out.println("⚠️  速度异常定位点已纠正："+ "\n" + "旧点位" + middlePoint + "\n" + "新点位" + newMiddlePoint);
            }
            lastPoint = newPoint;
            newLocationPoints.add(middlePoint);
        }

        return newLocationPoints;
    }

    private static double speed(LocationPoint a, LocationPoint b) {
        Coordinate ca = new Coordinate(a.getLongitude(), a.getLatitude());
        Coordinate cb = new Coordinate(b.getLongitude(), b.getLatitude());
        double d = GeoUtils.distanceM(ca, cb);
        long dt = b.getTimestamp() - a.getTimestamp();
        return (dt > 0) ? d / (dt / 1000.0) : 0;
    }
}
