package com.jwzt.modules.experiment.strategy;

import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.vo.EventState;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 地跑装卸策略实现（预留）
 * TODO: 根据实际业务需求实现具体的检测逻辑
 */
@Component
@Scope("prototype")
public class GroundVehicleLoadingStrategy implements LoadingUnloadingStrategy {
    
    @Override
    public EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints) {
        // TODO: 实现地跑装卸的具体检测逻辑
        System.out.println("⚠️ 地跑装卸策略尚未实现，请实现具体业务逻辑");
        return new EventState();
    }
    
    @Override
    public void resetState() {
        // TODO: 实现状态重置逻辑
    }
    
    @Override
    public String getStrategyName() {
        return "地跑装卸策略（预留）";
    }
    
    @Override
    public boolean isInParkingArea(LocationPoint currentPoint) {
        // TODO: 实现停车区域判断逻辑
        return false;
    }
}

