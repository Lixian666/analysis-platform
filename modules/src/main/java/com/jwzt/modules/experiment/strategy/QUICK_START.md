# 装卸业务策略快速使用指南

## 🎯 核心概念

### 三种装卸业务类型

| 装卸类型 | 识别方式 | 策略类 | VehicleType |
|---------|---------|--------|------------|
| 🚂 火车装卸 | 动态区域判断 | TrainLoadingStrategy | CAR |
| 🏃 地跑装卸 | 动态区域判断 | TrainLoadingStrategy | CAR |
| 🚛 板车装卸 | 预设车辆类型 | FlatbedLoadingStrategy | TRUCK |

## 📋 重要说明

### ⚠️ 火车和地跑的特殊性

**问题**：火车装卸和地跑装卸在卡号层面**无法区分**

**解决方案**：使用**动态区域判断**
- 同一张卡设置为 `VehicleType.CAR`
- 系统根据实时位置自动判断：
  - 在火车区域 → 执行火车装卸检测逻辑
  - 在地跑区域 → 执行地跑装卸检测逻辑

```java
// 同一张卡在不同区域自动切换检测逻辑
tracker.upsertVehicleType(cardId, VehicleType.CAR); // 火车和地跑共用

// 系统内部流程：
// 点位在火车区域 → 识别为火车装卸
// 点位在地跑区域 → 识别为地跑装卸
```

## 🚀 使用示例

### 示例 1：火车和地跑装卸（自动识别）

```java
@Autowired
private RealTimeDriverTracker tracker;

// 设置为 CAR 类型（火车和地跑共用）
tracker.upsertVehicleType("card-001", RealTimeDriverTracker.VehicleType.CAR);

// 喂入数据
List<LocationPoint> points = getLocationPoints();
tracker.ingest(points);

// 系统会根据点位所在区域自动判断：
// - 如果点位在货运线区域 → 识别为火车装卸
// - 如果点位在地跑区域 → 识别为地跑装卸
```

### 示例 2：板车装卸（预设类型）

```java
// 设置为 TRUCK 类型
tracker.upsertVehicleType("card-002", RealTimeDriverTracker.VehicleType.TRUCK);

// 喂入数据
tracker.ingest(points);

// 系统始终使用板车装卸检测逻辑
```

### 示例 3：批量处理（多种类型）

```java
// 火车和地跑的卡
tracker.upsertVehicleType("card-001", VehicleType.CAR);
tracker.upsertVehicleType("card-002", VehicleType.CAR);

// 板车的卡
tracker.upsertVehicleType("card-003", VehicleType.TRUCK);
tracker.upsertVehicleType("card-004", VehicleType.TRUCK);

// 批量喂入数据（支持多卡混合）
List<LocationPoint> allPoints = getAllLocationPoints();
tracker.ingest(allPoints);
```

## 🔧 配置地跑装卸

### 步骤 1：在 ZoneChecker 中添加地跑区域判断

```java
@Component
public class ZoneChecker {
    
    /**
     * 判断是否在地跑作业区域
     */
    public boolean isInGroundVehicleZone(LocationPoint point) {
        // 方式1：通过坐标范围判断
        return checkPointInPolygon(point, groundVehicleZonePolygon);
        
        // 方式2：通过特定基站判断
        // return isNearGroundVehicleBeacon(point);
    }
}
```

### 步骤 2：在 TrainLoadingStrategy 中启用地跑检测

修改 `isInGroundVehicleZone` 方法：

```java
private boolean isInGroundVehicleZone(LocationPoint currentPoint) {
    return zoneChecker.isInGroundVehicleZone(currentPoint); // 修改此行
}
```

### 步骤 3：实现地跑检测逻辑

在 `detectGroundVehicleEvent` 方法中实现：

```java
private EventState detectGroundVehicleEvent(List<LocationPoint> recordPoints,
                                            List<LocationPoint> historyPoints,
                                            List<LocationPoint> theFirstTenPoints,
                                            LocationPoint currentPoint,
                                            List<LocationPoint> theLastTenPoints) {
    // 判断是否在地跑上车区域
    boolean isInGroundVehicleBoardingZone = zoneChecker.isInGroundVehicleBoardingZone(currentPoint);
    
    // 判断是否在地跑下车区域
    boolean isInGroundVehicleDropZone = zoneChecker.isInGroundVehicleDropZone(currentPoint);
    
    // 检测上车事件
    if (lastEvent == BoardingDetector.Event.NONE && isInGroundVehicleBoardingZone) {
        // ... 上车检测逻辑
        return new EventState(BoardingDetector.Event.ARRIVED_BOARDING, currentPoint.getTimestamp());
    }
    
    // 检测下车事件
    if (lastEvent == BoardingDetector.Event.ARRIVED_BOARDING && isInGroundVehicleDropZone) {
        // ... 下车检测逻辑
        return new EventState(BoardingDetector.Event.ARRIVED_DROPPING, currentPoint.getTimestamp());
    }
    
    return new EventState();
}
```

## 📊 数据流程图

```
┌─────────────────┐
│  LocationPoint  │
│  (实时位置点)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  VehicleType?   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
  CAR       TRUCK
    │         │
    │         └──────────────────┐
    │                            │
    ▼                            ▼
┌─────────────────┐    ┌──────────────────┐
│  区域判断       │    │ FlatbedLoading   │
│  (动态识别)     │    │ Strategy         │
└────────┬────────┘    └──────────────────┘
         │
    ┌────┴────┐
    │         │
 火车区域   地跑区域
    │         │
    ▼         ▼
┌─────┐   ┌─────┐
│火车 │   │地跑 │
│检测 │   │检测 │
└─────┘   └─────┘
```

## ⚡ 常见问题

### Q1: 如何判断当前使用的是哪种检测逻辑？

**A**: 查看控制台日志：
- `开始处理（火车装卸）` → 火车检测逻辑
- `开始处理（地跑装卸）` → 地跑检测逻辑
- `开始处理（板车装卸）` → 板车检测逻辑

### Q2: 地跑区域还没配置，会影响火车装卸吗？

**A**: 不会。`isInGroundVehicleZone` 默认返回 `false`，所有 CAR 类型的点位都会走火车检测逻辑。

### Q3: 同一张卡可以在火车和地跑区域来回切换吗？

**A**: 可以。系统会根据每个点位的实时位置动态选择检测逻辑。

### Q4: 如何测试地跑装卸功能？

**A**: 
1. 配置地跑作业区域（`ZoneChecker.isInGroundVehicleZone`）
2. 实现地跑检测逻辑（`TrainLoadingStrategy.detectGroundVehicleEvent`）
3. 准备测试数据，确保点位在地跑区域内
4. 观察日志输出 `开始处理（地跑装卸）`

## 🔍 调试技巧

### 开启详细日志

在 `TrainLoadingStrategy` 中添加日志：

```java
private boolean isInGroundVehicleZone(LocationPoint currentPoint) {
    boolean result = zoneChecker.isInGroundVehicleZone(currentPoint);
    System.out.println("🔍 区域判断：当前点位 " + currentPoint.getAcceptTime() + 
                       " 是否在地跑区域：" + result);
    return result;
}
```

### 查看策略选择

在 `RealTimeDriverTracker` 中添加日志：

```java
private LoadingUnloadingStrategy getStrategyForVehicleType(VehicleType vehicleType) {
    LoadingStrategyFactory.VehicleType strategyType;
    if (vehicleType == VehicleType.TRUCK) {
        strategyType = LoadingStrategyFactory.VehicleType.FLATBED;
        System.out.println("📌 使用板车装卸策略");
    } else {
        strategyType = LoadingStrategyFactory.VehicleType.TRAIN;
        System.out.println("📌 使用火车和地跑组合策略");
    }
    return loadingStrategyFactory.getStrategy(strategyType);
}
```

## 📞 技术支持

如有问题，请参考完整文档：`README.md`

---

**最后更新**：2025-10-20

