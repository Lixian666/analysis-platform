package com.jwzt.modules.experiment.strategy;

import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.vo.EventState;

import java.util.List;

/**
 * 装卸业务策略接口
 * 定义不同装卸业务（火车、板车、地跑等）的统一行为规范
 */
public interface LoadingUnloadingStrategy {
    
    /**
     * 检测装卸事件
     * 
     * @param recordPoints 当前窗口的点位记录
     * @param historyPoints 历史点位记录
     * @param status 当前点位状态（0-默认状态，1-回溯）
     * @return 事件状态（上车/下车/无事件）
     */
    EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status);
    
    /**
     * 重置策略内部状态
     * 用于流程超时或完成后的状态清理
     */
    void resetState();
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称（用于日志和调试）
     */
    String getStrategyName();
    
    /**
     * 判断当前点位是否在停车区域
     * 
     * @param currentPoint 当前点位
     * @return true-在停车区域，false-不在
     */
    boolean isInParkingArea(LocationPoint currentPoint);
}

