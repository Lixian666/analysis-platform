package com.jwzt.modules.experiment.domain;

import com.jwzt.modules.experiment.config.FilterConfig;

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
        if (window.size() < 2) return MovementState.STOPPED;

        double totalDist = 0;
        long totalTime = 0;

        for (int i = 1; i < window.size(); i++) {
            LocationPoint p1 = window.get(i - 1);
            LocationPoint p2 = window.get(i);
            totalDist += p1.distanceTo(p2);
            totalTime += (p2.getTimestamp() - p1.getTimestamp());
        }

        double avgSpeed = totalTime > 0 ? (totalDist / (totalTime / 1000.0)) : 0;

        if (FilterConfig.MIN_SPEED_MPS < avgSpeed && avgSpeed <= FilterConfig.MIN_WALKING_SPEED) return MovementState.STOPPED;
        else if (FilterConfig.MIN_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_WALKING_SPEED) return MovementState.WALKING;
        else if (FilterConfig.MAX_WALKING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_RUNING_SPEED) return MovementState.RUNNING;
        else if (FilterConfig.MAX_RUNING_SPEED < avgSpeed && avgSpeed <= FilterConfig.MAX_LOW_DRIVING_SPEED) return MovementState.LOW_DRIVING;
        else return MovementState.DRIVING;
    }
}
