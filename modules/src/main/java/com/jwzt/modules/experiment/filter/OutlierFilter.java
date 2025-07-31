package com.jwzt.modules.experiment.filter;

import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.MovementAnalyzer;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.geo.CoordinateUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.jwzt.modules.experiment.utils.GeoUtils.calculateCenter;


/**
 * 坐标点滤波器
 */
public class OutlierFilter {
    private final int windowSize = 5;    // 滑动窗口大小必须为奇数
    private LocationPoint lastPoint = null;
    private final Deque<LocationPoint> history = new ArrayDeque<>();

    private Deque<LocationPoint> window = new ArrayDeque<>();
    private static final double MAX_DEVIATION_SPEEDUP = 7.0;  //允许的偏差
    private static final double ANGLE_THRESHOLD = 150.0; //角度阈值
    private static final String HUOCHANG = FilePathConfig.HUOCHANG;
    ZoneChecker zoneChecker = new ZoneChecker(HUOCHANG);



    public List<LocationPoint> stateAnalysis(List<LocationPoint> points) {
        List<LocationPoint> result = new ArrayList<>();
        for (LocationPoint point : points){
            window.addLast(point);
            if (window.size() > 2) {
                window.removeFirst();
            }
            // 通过windowSize个点判断当前运动状态
            MovementAnalyzer.MovementState state = MovementAnalyzer.analyzeState(new ArrayList<>(window));
            if (state == MovementAnalyzer.MovementState.DRIVING) {
                System.out.println("🚗 当前正在驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
            } else if (state == MovementAnalyzer.MovementState.LOW_DRIVING) {
                System.out.println("🚗🐢 当前正在低速驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
            } else if (state == MovementAnalyzer.MovementState.WALKING) {
                System.out.println("🚶 当前在步行，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
            } else if (state == MovementAnalyzer.MovementState.RUNNING) {
                System.out.println("🏃 当前在小跑，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
            } else {
                System.out.println("⛔ 当前静止，时间为：" + point.getAcceptTime());
            }
            point.setState(state);
            result.add(point);
        }
        return result;
    }




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

        if (!zoneChecker.isInDrivingZone(newPoint)){
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

    public List<LocationPoint> fixTheData(List<LocationPoint> sortPoints) {
        sortPoints = sortPoints.stream()
                .filter(OutlierFilter::isValidCoordinate)
                .collect(Collectors.toList());
        System.out.println("开始处理一秒内多个点的情况");
        // 处理一秒内多个点的情况（使用中位数）
        List<LocationPoint> newPoints = GeoUtils.processMultiplePointsPerSecond(sortPoints);
        System.out.println("完成处理一秒内多个点的情况");
        if (FilterConfig.IS_STAY_VERIFY){
            // 检测停留点
            detectStayPoints(newPoints);
            // 修正停留区
            newPoints = correctStayPoints(newPoints);
        }
        System.out.println("开始修正运动点（带速度自适应阈值）");
        // 修正运动点（带速度自适应阈值）
        List<LocationPoint> newLocationPoints = correctMovingPoints(newPoints);
        System.out.println("完成修正运动点（带速度自适应阈值）");
//        List<LocationPoint> newLocationPoints = new ArrayList<>();
//        for (LocationPoint newPoint : newPoints){
//            if (lastPoint == null) {
//                lastPoint = newPoint;
//                history.addLast(newPoint);
//                continue;
//            }
//            if (history.size() < ((windowSize / 2) + 1)){
//                newLocationPoints.add(newPoint);
//            }
//            long timeDiff = (newPoint.getTimestamp() - lastPoint.getTimestamp());
//            if (!zoneChecker.isInDrivingZone(newPoint)){
////                lastPoint = newPoint;
//                System.out.println("⚠️  区域异常定位点已剔除：" + newPoint);
//                continue;
//            }
////            // 时间间隔太小
////            if (timeDiff < FilterConfig.MIN_TIME_INTERVAL_MS || Objects.equals(newPoint.getTimestamp(), lastPoint.getTimestamp())) {
////                lastPoint = newPoint;
////                history.removeLast();
////                System.out.println("⚠️  时间间隔异常定位点已更新最新点位：" + newPoint);
////            }
//            history.addLast(newPoint);
//            if (history.size() < windowSize) {
//                continue;
//            }
//            if (history.size() > windowSize) {
//                history.removeFirst();
//            }
//            // 计算中间索引
//            int middleIndex = history.size() / 2;
//            List<LocationPoint> window = new ArrayList<>(history);
//            LocationPoint firstPoint = window.get(middleIndex - 2);
//            LocationPoint middleBeforePoint = window.get(middleIndex - 1);
//            LocationPoint middlePoint = window.get(middleIndex);
//            LocationPoint middleAfterPoint = window.get(middleIndex + 1);
//            LocationPoint finalPoint = window.get(middleIndex + 2);
//            // 速度跳变判断
//            double v01 = speed(firstPoint, middleBeforePoint);
//            double v12 = speed(middleBeforePoint, middlePoint);
//            double v23 = speed(middlePoint, middleAfterPoint);
//            double v34 = speed(middleAfterPoint, finalPoint);
//            if (Math.abs(v01 - v12) > FilterConfig.MAX_SPEED_MPS) {
//                double medianSpeed = FilterConfig.MAX_SPEED_MPS;
//                if (v01 > (v12 + v23 + v34)){
//                    List<Double> speeds = new ArrayList<>();
//                    speeds.add(v12);
//                    speeds.add(v23);
//                    speeds.add(v34);
//                    Collections.sort(speeds);
//                    medianSpeed = speeds.get(1);
//                }
//                double distance = GeoUtils.distanceM(new Coordinate(middleBeforePoint.getLongitude(), middleBeforePoint.getLatitude()), new Coordinate(middleAfterPoint.getLongitude(), middleAfterPoint.getLatitude()));
//                long time = middleAfterPoint.getTimestamp() - middleBeforePoint.getTimestamp();
//                double alpha = CoordinateUtils.computeAlpha(medianSpeed, time, distance);
//                double[] corrected = CoordinateUtils.interpolate(
//                        middleBeforePoint.getLongitude(), middleBeforePoint.getLatitude(),
//                        middleAfterPoint.getLongitude(), middleAfterPoint.getLatitude(),
//                        alpha
//                );
//                LocationPoint newMiddlePoint = middlePoint;
//                newMiddlePoint.setLongitude(corrected[0]);
//                newMiddlePoint.setLatitude(corrected[1]);
//                newMiddlePoint.setSpeed(speed(middleBeforePoint,middlePoint));
//                System.out.println("⚠️  速度异常定位点已纠正："+ "\n" + "旧点位" + middlePoint + "\n" + "新点位" + newMiddlePoint);
//            }
//            lastPoint = newPoint;
//            newLocationPoints.add(middlePoint);
//        }
        return newLocationPoints;
    }

    private static double distance(LocationPoint a, LocationPoint b) {
        Coordinate ca = new Coordinate(a.getLongitude(), a.getLatitude());
        Coordinate cb = new Coordinate(b.getLongitude(), b.getLatitude());
        return GeoUtils.distanceM(ca, cb);
    }

    private static double speed(LocationPoint a, LocationPoint b) {
        Coordinate ca = new Coordinate(a.getLongitude(), a.getLatitude());
        Coordinate cb = new Coordinate(b.getLongitude(), b.getLatitude());
        double d = GeoUtils.distanceM(ca, cb);
        long dt = b.getTimestamp() - a.getTimestamp();
        return (dt > 0) ? d / (dt / 1000.0) : 0;
    }

    private static double directionChange(LocationPoint p0, LocationPoint p1, LocationPoint p2, LocationPoint p3, LocationPoint p4) {
        Coordinate c0 = new Coordinate(p0.getLongitude(), p0.getLatitude());
        Coordinate c1 = new Coordinate(p1.getLongitude(), p1.getLatitude());
        Coordinate c2 = new Coordinate(p2.getLongitude(), p2.getLatitude());
        Coordinate c3 = new Coordinate(p3.getLongitude(), p3.getLatitude());
        Coordinate c4 = new Coordinate(p4.getLongitude(), p4.getLatitude());
        double directionChange = GeoUtils.calculateDirectionChange(c0, c1, c2, c3, c4);
        return directionChange;
    }

    /**
     * 修正运动点（带速度自适应阈值）
     */
    private static List<LocationPoint> correctMovingPoints(List<LocationPoint> points) {
        if (points.size() < 5) return points;

        List<LocationPoint> corrected = new ArrayList<>(points);
        boolean hasDrift;

        do {
            hasDrift = false;
            for (int i = 2; i < corrected.size() - 2; i++) {
                LocationPoint p1 = corrected.get(i - 2);
                LocationPoint p2 = corrected.get(i - 1);
                LocationPoint curr = corrected.get(i);
                LocationPoint p4 = corrected.get(i + 1);
                LocationPoint p5 = corrected.get(i + 2);
                if (FilterConfig.IS_STAY_VERIFY && curr.getIsStay()) continue;

                double dist1 = distance(p2, curr);
                double dist2 = distance(curr, p4);
                double time1 = (curr.getTimestamp() - p2.getTimestamp()) / 1000.0;
                double time2 = (p4.getTimestamp() - curr.getTimestamp()) / 1000.0;

                double speed1 = (time1 > 0) ? dist1 / time1 : 0.0;
                double speed2 = (time2 > 0) ? dist2 / time2 : 0.0;

                double dynamicThreshold = Math.max(FilterConfig.BASE_DISTANCE_THRESHOLD, Math.max(speed1, speed2) * 2);

                // 异常检测条件
                boolean isDrift = false;
                if (dist1 > dynamicThreshold && dist2 > dynamicThreshold) {
                    isDrift = true;
                }

                // 方向突变检测
                if (!isDrift && speed1 > FilterConfig.MAX_RUNING_SPEED) {
                    double angle = directionChange(p1, p2, curr, p4, p5);
                    if (angle > 120.0) {
                        isDrift = true;
                    }
                }

                if (isDrift) {
                    if (!isValidCoordinate(p2) || !isValidCoordinate(p4)) continue;
                    LocationPoint fixed = interpolate(p2, p4);
                    corrected.set(i, fixed);
                    hasDrift = true;
                }

                if (i == corrected.size() - 3){
                    hasDrift = false;
                }
            }
        } while (hasDrift);

        // 后处理：移动平均平滑
        corrected = smoothWithMovingAverage(corrected, 3);
        return corrected;
    }

//    private static List<LocationPoint> correctMovingPoints(List<LocationPoint> points) {
//        if (points.size() < 3) return points;
//
//        // 计算每个点的瞬时速度
//        double[] speeds = new double[points.size()];
//        speeds[0] = 0.0;
//        for (int i = 1; i < points.size(); i++) {
//            LocationPoint prev = points.get(i - 1);
//            LocationPoint curr = points.get(i);
//            double dist = distance(prev, curr);
//            double time = (curr.getTimestamp() - prev.getTimestamp()) / 1000.0;
//            speeds[i] = (time > 0) ? dist / time : 0.0;
//        }
//
//        List<LocationPoint> corrected = new ArrayList<>(points);
//        boolean hasDrift;
//
//        do {
//            hasDrift = false;
//            for (int i = 1; i < corrected.size() - 1; i++) {
//                System.out.println(i);
//                if (FilterConfig.IS_STAY_VERIFY){
//                    if (corrected.get(i).getIsStay()) continue; // 跳过停留点
//                }
//                LocationPoint prev = corrected.get(i - 1);
//                LocationPoint curr = corrected.get(i);
//                LocationPoint next = corrected.get(i + 1);
//
//                // 计算动态阈值（基于前后速度）
//                double dynamicThreshold = Math.max(FilterConfig.BASE_DISTANCE_THRESHOLD,
//                        Math.max(speeds[i], speeds[i + 1]) * 2.0);
//
//                //
//                double speed = speeds[i];
//                // 计算加速度
//                double distPrev = distance(prev, curr);
//                double distNext = distance(curr, next);
//                double timePrev = (curr.getTimestamp() - prev.getTimestamp()) / 1000.0;
//                double timeNext = (next.getTimestamp() - curr.getTimestamp()) / 1000.0;
//
//                double accelPrev = (timePrev > 0) ? (2 * distPrev / (timePrev * timePrev)) : 0;
//                double accelNext = (timeNext > 0) ? (2 * distNext / (timeNext * timeNext)) : 0;
//
//                // 漂移检测条件
//                boolean isDrift = false;
//
//                // 条件1：距离异常（当前点与前后点距离过大）
//                if (distPrev > dynamicThreshold && distNext > dynamicThreshold) {
//                    isDrift = true;
//                }
//                // 条件2：加速度异常
//                else if (accelPrev > FilterConfig.MAX_ACCELERATION || accelNext > FilterConfig.MAX_ACCELERATION) {
//                    isDrift = true;
//                }
//                // 条件3：速度达到驾驶速度判断方向突变
//                if (speed > FilterConfig.MAX_RUNING_SPEED){
//                    // 条件3：方向突变（前后向量夹角大于120度）
//                    if (i > 1 && i < corrected.size() - 2) {
//                        LocationPoint prev2 = corrected.get(i - 2);
//                        LocationPoint next2 = corrected.get(i + 2);
////                        System.out.println("prev2: " + prev2.getAcceptTime() + " timestamp:" + prev2.getTimestamp());
////                        System.out.println("prev: " + prev.getAcceptTime() + " timestamp:" + prev.getTimestamp());
////                        System.out.println("curr: " + curr.getAcceptTime() + " timestamp:" + curr.getTimestamp());
////                        System.out.println("next: " + next.getAcceptTime() + " timestamp:" + next.getTimestamp());
////                        System.out.println("next2: " + next2.getAcceptTime() + " timestamp:" + next2.getTimestamp());
//                        double angle = directionChange(prev2, prev, curr, next, next2);
//                        if (angle > 120.0) {
//                            isDrift = true;
//                        }
//                    }
//                }
//
//                // 修正漂移点
//                if (isDrift) {
//                    if (!isValidCoordinate(prev) || !isValidCoordinate(next)) {
//                        continue; // 不修正，让它保留原始值
//                    }
//                    LocationPoint fixedPoint = interpolate(prev, next);
//                    corrected.set(i, fixedPoint);
//                    hasDrift = true;
////                    System.out.println("修正前速度：" + speeds[i]);
//                    // 更新速度
//                    speeds[i] = distance(prev, fixedPoint) /
//                            ((fixedPoint.getTimestamp() - prev.getTimestamp()) / 1000.0);
////                    System.out.println("修正后速度：" + speeds[i]);
//                }
//                if (i == (corrected.size() - 2)){
//                    hasDrift = false;
//                }
//            }
//        } while (hasDrift);
//
//        return corrected;
//    }

    /**
     * 停留点检测（使用滑动窗口）
     */
    private static void detectStayPoints(List<LocationPoint> points) {
        if (points.size() < FilterConfig.STAY_WINDOW_SIZE) return;

        // 存储停留段信息: <开始索引, 结束索引, 中心点>
        List<Object[]> staySegments = new ArrayList<>();

        int start = 0;
        while (start <= points.size() - FilterConfig.STAY_WINDOW_SIZE) {
            // 获取当前窗口
            List<LocationPoint> window = points.subList(start, start + FilterConfig.STAY_WINDOW_SIZE);

            // 计算窗口中心
            LocationPoint center = calculateCenter(window);

            // 检查窗口内所有点是否都在停留半径内
            boolean allInRadius = true;
            for (LocationPoint p : window) {
                if (distance(center, p) > FilterConfig.STAY_RADIUS) {
                    allInRadius = false;
                    break;
                }
            }

            // 发现停留窗口
            if (allInRadius) {
                int end = start + FilterConfig.STAY_WINDOW_SIZE - 1;

                // 尝试扩展停留段
                while (end < points.size() - 1) {
                    LocationPoint next = points.get(end + 1);
                    if (distance(center, next) <= FilterConfig.STAY_RADIUS) {
                        end++;
                        // 更新中心点（动态计算）
                        center = calculateCenter(points.subList(start, end + 1));
                    } else {
                        break;
                    }
                }

                // 检查停留时长是否满足阈值
                long duration = points.get(end).getTimestamp() - points.get(start).getTimestamp();
                if (duration >= FilterConfig.STAY_DURATION_THRESHOLD) {
                    staySegments.add(new Object[]{start, end, center});
                }

                start = end; // 跳过已处理的停留段
            }
            start++;
        }

        // 标记停留点
        for (Object[] seg : staySegments) {
            int segStart = (int) seg[0];
            int segEnd = (int) seg[1];
            for (int i = segStart; i <= segEnd; i++) {
                points.get(i).setIsStay(true);
            }
        }
    }

    /**
     * 修正停留区域的点
     */
    private static List<LocationPoint> correctStayPoints(List<LocationPoint> points) {
        // 获取所有停留段
        List<int[]> staySegments = new ArrayList<>();
        int i = 0;
        while (i < points.size()) {
            if (points.get(i).getIsStay()) {
                int start = i;
                while (i < points.size() && points.get(i).getIsStay()) i++;
                staySegments.add(new int[]{start, i - 1});
            } else {
                i++;
            }
        }

        // 对每个停留段进行修正
        for (int[] seg : staySegments) {
            int start = seg[0];
            int end = seg[1];
            LocationPoint center = calculateCenter(points.subList(start, end + 1));

            // 替换停留段内所有点为停留中心
            for (int j = start; j <= end; j++) {
                points.set(j, new LocationPoint(
                        points.get(j).getCardId(),
                        center.getLongitude(),
                        center.getLatitude(),
                        points.get(j).getAcceptTime(),
                        points.get(j).getTimestamp()));
                points.get(j).setIsStay(true);
            }
        }

        return points;
    }

    /**
     * 线性插值（中点）
     */
    private static LocationPoint interpolate(LocationPoint p1, LocationPoint p2) {
        long timestamp = (p1.getTimestamp() + p2.getTimestamp()) / 2;
        return new LocationPoint(
                p1.getCardId(),
                (p1.getLongitude() + p2.getLongitude()) / 2,
                (p1.getLatitude() + p2.getLatitude()) / 2,
                DateTimeUtils.timestampToDateTimeStr(timestamp),
                timestamp
        );
    }

    private static boolean isValidCoordinate(LocationPoint p) {
        return p != null &&
                p.getLatitude() != 0.0 &&
                p.getLongitude() != 0.0 &&
                Math.abs(p.getLatitude()) <= 90 &&
                Math.abs(p.getLongitude()) <= 180;
    }

    private static List<LocationPoint> smoothWithMovingAverage(List<LocationPoint> points, int windowSize) {
        List<LocationPoint> smoothed = new ArrayList<>(points);
        int half = windowSize / 2;

        for (int i = half; i < points.size() - half; i++) {
            double sumLon = 0.0, sumLat = 0.0;
            int validCount = 0;
            LocationPoint center = points.get(i);

            for (int j = i - half; j <= i + half; j++) {
                LocationPoint p = points.get(j);
                // 判断是否有效坐标
                if (!isValidCoordinate(p)) continue;
                // 判断是否漂移值（比如距离中心点超过100米）
                if (distance(center, p) > 100.0) continue;

                sumLon += p.getLongitude();
                sumLat += p.getLatitude();
                validCount++;
            }

            // 若有效点数足够才更新
            if (validCount > 0) {
                double avgLon = sumLon / validCount;
                double avgLat = sumLat / validCount;
                LocationPoint curr = smoothed.get(i);
                curr.setLongitude(avgLon);
                curr.setLatitude(avgLat);
            }
        }

        return smoothed;
    }


//    private static List<LocationPoint> smoothWithMovingAverage(List<LocationPoint> points, int windowSize) {
//        List<LocationPoint> smoothed = new ArrayList<>(points);
//        int half = windowSize / 2;
//        for (int i = half; i < points.size() - half; i++) {
//            double sumLon = 0.0, sumLat = 0.0;
//            for (int j = i - half; j <= i + half; j++) {
//                if (!isValidCoordinate(points.get(j))) {
//                         break; // 不修正，让它保留原始值
//                    }
//                sumLon += points.get(j).getLongitude();
//                sumLat += points.get(j).getLatitude();
//            }
//            double avgLon = sumLon / windowSize;
//            double avgLat = sumLat / windowSize;
//            LocationPoint curr = smoothed.get(i);
//            curr.setLongitude(avgLon);
//            curr.setLatitude(avgLat);
//        }
//        return smoothed;
//    }


}
