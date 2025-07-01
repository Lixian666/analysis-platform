package com.jwzt.modules.experiment.filter;

import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.map.ZoneChecker;


/**
 * 坐标点滤波器
 */
public class OutlierFilter {
    private LocationPoint lastPoint = null;

    public int isValid(LocationPoint newPoint) {
        if (lastPoint == null) {
            lastPoint = newPoint;
            return 0; // 第一个点不处理
        }
        Coordinate newCoordinate = new Coordinate(newPoint.getLongitude(), newPoint.getLatitude());
        Coordinate lastCoordinate = new Coordinate(lastPoint.getLongitude(), lastPoint.getLatitude());

        double distance = GeoUtils.distanceM(newCoordinate, lastCoordinate);
        long timeDiff = newPoint.getTimestamp() - lastPoint.getTimestamp();

        // 时间间隔太小
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
}
