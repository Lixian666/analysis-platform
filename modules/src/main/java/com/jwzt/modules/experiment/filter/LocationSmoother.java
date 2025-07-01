package com.jwzt.modules.experiment.filter;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * 定位数据平滑（去噪处理）滤波器
 */
public class LocationSmoother {
    private final int windowSize = FilterConfig.WINDOW_SIZE;
    private final Deque<LocationPoint> history = new ArrayDeque<>();

    public LocationPoint smooth(LocationPoint newPoint) {
        history.addLast(newPoint);
        if (history.size() > windowSize) {
            history.removeFirst();
        }

        double sumX = 0;
        double sumY = 0;
        double weightSum = 0;

        int size = history.size();
        int index = 1;

        // 计算加权平均
        for (LocationPoint point : history) {
            // 权重随着index递增，表示越靠近当前点，权重越大
            double weight = index++;
            sumX += point.getLongitude() * weight;
            sumY += point.getLatitude() * weight;
            weightSum += weight;
        }

        // 计算平滑后的经纬度
        double avgX = sumX / weightSum;
        double avgY = sumY / weightSum;

        return new LocationPoint(avgX, avgY, newPoint.getAcceptTime(), newPoint.getTimestamp(), newPoint.getSpeed());
    }
//        history.addLast(newPoint);
//        if (history.size() > windowSize) {
//            history.removeFirst();
//        }
//
//        double avgX = history.stream().mapToDouble(LocationPoint::getLongitude).average().orElse(newPoint.getLongitude());
//        double avgY = history.stream().mapToDouble(LocationPoint::getLatitude).average().orElse(newPoint.getLatitude());
//
//        return new LocationPoint(avgX, avgY, newPoint.getAcceptTime(), newPoint.getTimestamp(), newPoint.getSpeed());
//    }
}
