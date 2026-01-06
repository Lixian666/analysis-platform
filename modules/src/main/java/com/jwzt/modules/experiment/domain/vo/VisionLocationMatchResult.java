package com.jwzt.modules.experiment.domain.vo;

import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 视觉识别与定位数据匹配结果
 * 
 * @author lx
 * @date 2025-01-20
 */
@Data
public class VisionLocationMatchResult {
    
    /**
     * 视觉识别事件组（时间相邻接续的一组数据）
     */
    private List<VisionEvent> visionEventGroup;
    
    /**
     * 匹配成功的定位点列表（每个视觉事件可能匹配多个定位点）
     */
    private List<MatchedLocationPoint> matchedLocationPoints = new ArrayList<>();
    
    /**
     * 匹配的定位点，包含视觉事件和对应的定位点
     */
    @Data
    public static class MatchedLocationPoint {
        /**
         * 视觉事件
         */
        private VisionEvent visionEvent;
        
        /**
         * 匹配的定位点
         */
        private LocationPoint locationPoint;
        
        /**
         * 时间差（毫秒）
         */
        private long timeDiff;
        
        /**
         * 距离（米）
         */
        private double distance;
        
        public MatchedLocationPoint(VisionEvent visionEvent, LocationPoint locationPoint, long timeDiff, double distance) {
            this.visionEvent = visionEvent;
            this.locationPoint = locationPoint;
            this.timeDiff = timeDiff;
            this.distance = distance;
        }
    }
}

