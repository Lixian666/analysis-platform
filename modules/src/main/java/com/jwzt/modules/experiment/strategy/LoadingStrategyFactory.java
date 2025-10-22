package com.jwzt.modules.experiment.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 装卸策略工厂
 * 根据车辆类型返回对应的装卸策略实现
 * 
 * 注意：改为 prototype 作用域，确保每个 RealTimeDriverTracker 实例都有独立的工厂
 * 工厂每次获取策略时都会从 ApplicationContext 获取新的 prototype 实例
 */
@Component
@Scope("prototype")
public class LoadingStrategyFactory {
    
    @Autowired
    private ApplicationContext applicationContext;
    
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
     * 每次调用都会从 Spring 容器获取新的 prototype 策略实例，避免多线程串扰
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
                return applicationContext.getBean(TrainLoadingStrategy.class);
            case FLATBED:
                return applicationContext.getBean(FlatbedLoadingStrategy.class);
            case GROUND_VEHICLE:
                return applicationContext.getBean(GroundVehicleLoadingStrategy.class);
            default:
                throw new IllegalArgumentException("不支持的车辆类型: " + vehicleType);
        }
    }
}

