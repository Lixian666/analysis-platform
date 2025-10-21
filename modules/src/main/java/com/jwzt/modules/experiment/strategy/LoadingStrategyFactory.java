package com.jwzt.modules.experiment.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 装卸策略工厂
 * 根据车辆类型返回对应的装卸策略实现
 */
@Component
public class LoadingStrategyFactory {
    
    @Autowired
    private TrainLoadingStrategy trainLoadingStrategy;
    
    @Autowired
    private FlatbedLoadingStrategy flatbedLoadingStrategy;
    
    @Autowired
    private GroundVehicleLoadingStrategy groundVehicleLoadingStrategy;
    
    /**
     * 车辆类型枚举
     */
    public enum VehicleType {
        /** 火车 */
        TRAIN,
        /** 板车 */
        FLATBED,
        /** 地跑 */
        GROUND_VEHICLE
    }
    
    /**
     * 根据车辆类型获取对应的装卸策略
     * 
     * @param vehicleType 车辆类型
     * @return 对应的装卸策略实现
     */
    public LoadingUnloadingStrategy getStrategy(VehicleType vehicleType) {
        if (vehicleType == null) {
            // 默认使用火车装卸策略
            vehicleType = VehicleType.TRAIN;
        }
        
        switch (vehicleType) {
            case TRAIN:
                return trainLoadingStrategy;
            case FLATBED:
                return flatbedLoadingStrategy;
            case GROUND_VEHICLE:
                return groundVehicleLoadingStrategy;
            default:
                throw new IllegalArgumentException("不支持的车辆类型: " + vehicleType);
        }
    }
}

