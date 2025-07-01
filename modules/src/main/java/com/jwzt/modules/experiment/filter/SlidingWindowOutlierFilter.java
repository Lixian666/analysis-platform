package com.jwzt.modules.experiment.filter;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * 滑动窗口 + 中值位置滤波器
 */
public class SlidingWindowOutlierFilter {
    private final int windowSize = FilterConfig.WINDOW_SIZE;
    private final Deque<LocationPoint> window = new ArrayDeque<>();

    public boolean isValid(LocationPoint newPoint) {
        if (window.size() < windowSize) {
            window.addLast(newPoint);
            return true;
        }

        // 计算窗口中心点
        double avgX = window.stream().mapToDouble(LocationPoint::getLongitude).average().orElse(newPoint.getLongitude());
        double avgY = window.stream().mapToDouble(LocationPoint::getLatitude).average().orElse(newPoint.getLatitude());

        double dx = newPoint.getLongitude() - avgX;
        double dy = newPoint.getLatitude() - avgY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 10.0) { // 距离中心太远，判定为异常点
            return false;
        }

        window.removeFirst();
        window.addLast(newPoint);
        return true;
    }
}
