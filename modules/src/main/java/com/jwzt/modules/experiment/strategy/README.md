# 装卸业务策略模式重构说明

## 📋 重构概述

本次重构采用**策略模式**对 `RealTimeDriverTracker` 中的装卸业务逻辑进行了解耦和重构，将原本混杂在一个类中的多种装卸业务（火车、板车、地跑等）分离成独立的策略类。

## 🎯 重构目标

1. **职责分离**：每种装卸业务独立成类，职责清晰
2. **易于扩展**：添加新业务（如地跑装卸）只需新增策略类，无需修改现有代码
3. **便于测试**：每种装卸业务可以独立测试
4. **向后兼容**：保持原有 API 不变，不影响现有调用

## 📁 重构后的结构

```
strategy/
├── LoadingUnloadingStrategy.java          # 策略接口
├── TrainLoadingStrategy.java              # 火车装卸策略实现
├── FlatbedLoadingStrategy.java            # 板车装卸策略实现
├── GroundVehicleLoadingStrategy.java      # 地跑装卸策略实现（预留）
├── LoadingStrategyFactory.java            # 策略工厂
└── README.md                               # 本文档
```

## 🔧 核心组件说明

### 1. LoadingUnloadingStrategy（策略接口）

定义了所有装卸策略必须实现的方法：

```java
public interface LoadingUnloadingStrategy {
    // 检测装卸事件
    EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints);
    
    // 重置策略内部状态
    void resetState();
    
    // 获取策略名称
    String getStrategyName();
    
    // 判断是否在停车区域
    boolean isInParkingArea(LocationPoint currentPoint);
}
```

### 2. TrainLoadingStrategy（火车和地跑组合装卸策略）

- **检测算法**：使用原 `BoardingDetector.updateState()` 的逻辑
- **适用场景**：火车装卸 + 地跑装卸（动态识别）
- **核心特性**：
  - 🔍 **动态区域判断**：根据实时位置自动识别是火车区域还是地跑区域
  - 🚫 **无需预设车辆类型**：卡号层面无法区分，完全依赖位置信息
  - 🎯 **智能切换**：同一张卡在不同区域自动切换检测逻辑
- **检测区域**：
  - 火车：货运线区域、货运线作业台区域、停车区域
  - 地跑：地跑作业区域（待配置）
- **UWB 基站**：
  - 火车：货运线作业台 2号线 A/B
  - 地跑：待配置

### 3. FlatbedLoadingStrategy（板车装卸策略）

- **检测算法**：使用原 `BoardingDetector.updateStateTruck()` 的逻辑
- **适用场景**：板车装卸业务
- **检测区域**：板车作业区、停车区域
- **RFID 检测**：板车作业区 RFID

### 4. GroundVehicleLoadingStrategy（地跑装卸策略）

- **状态**：独立策略类已预留，但实际逻辑已整合到 `TrainLoadingStrategy` 中
- **设计原因**：地跑与火车装卸在卡号层面无法区分，必须根据实时位置动态判断
- **实现方式**：`TrainLoadingStrategy` 内部根据区域自动切换检测逻辑
- **用途**：如果将来地跑需要完全独立的检测逻辑，可使用此类

### 5. LoadingStrategyFactory（策略工厂）

根据车辆类型返回对应的策略实现：

```java
public enum VehicleType {
    TRAIN,           // 火车
    FLATBED,         // 板车
    GROUND_VEHICLE   // 地跑
}
```

## 📝 使用方法

### 原有调用方式（保持不变）

```java
// 注入服务
@Autowired
private RealTimeDriverTracker tracker;

// 设置车辆类型
tracker.upsertVehicleType(cardId, RealTimeDriverTracker.VehicleType.CAR);   // 火车和地跑（自动根据区域判断）
tracker.upsertVehicleType(cardId, RealTimeDriverTracker.VehicleType.TRUCK); // 板车

// 实时数据摄入
tracker.ingest(locationPoints);
```

**重要说明**：
- 🔄 **火车和地跑无需预设**：设置为 `VehicleType.CAR` 即可，系统会根据实时位置自动判断是火车还是地跑
- 📍 **位置驱动识别**：当同一张卡经过火车区域时识别为火车装卸，经过地跑区域时识别为地跑装卸
- ⚠️ **板车仍需预设**：板车装卸的检测逻辑独立，需要明确设置为 `VehicleType.TRUCK`

### 配置地跑装卸区域

由于地跑和火车在卡号层面无法区分，需要在 `TrainLoadingStrategy` 中配置地跑作业区域：

1️⃣ **在 ZoneChecker 中添加地跑区域判断方法**

```java
public boolean isInGroundVehicleZone(LocationPoint point) {
    // 根据实际业务配置地跑作业区域的坐标范围或基站
    // 例如：判断是否在特定的地跑作业区域内
    return checkPointInPolygon(point, groundVehicleZonePolygon);
}
```

2️⃣ **在 TrainLoadingStrategy 中启用地跑检测**

修改 `isInGroundVehicleZone` 方法：

```java
private boolean isInGroundVehicleZone(LocationPoint currentPoint) {
    return zoneChecker.isInGroundVehicleZone(currentPoint);
}
```

3️⃣ **实现地跑检测逻辑**

在 `detectGroundVehicleEvent` 方法中实现具体的检测逻辑：

```java
private EventState detectGroundVehicleEvent(...) {
    // 地跑装卸的上下车检测逻辑
    // 可能与火车类似，但使用不同的区域和基站配置
    // ...
}
```

## ✅ 重构优势

| 重构前 | 重构后 |
|--------|--------|
| ❌ 553行代码混杂在一个类中 | ✅ 代码分离到多个策略类，每个类约200-300行 |
| ❌ if-else 判断车辆类型 | ✅ 策略模式自动选择对应实现 |
| ❌ 添加新业务需修改多处代码 | ✅ 新增策略类即可，符合开闭原则 |
| ❌ 难以单独测试某种业务 | ✅ 每个策略可独立测试 |
| ❌ 职责不清晰 | ✅ 每个策略职责单一明确 |
| ❌ 火车和地跑混在一起 | ✅ 动态区域判断，智能切换检测逻辑 |

## 🔍 向后兼容性

✅ **完全兼容**：所有现有的调用代码无需修改

- `RealTimeDriverTracker` 的公共 API 保持不变
- `VehicleType` 枚举保持不变（CAR, TRUCK）
- `upsertVehicleType()`, `ingest()` 等方法签名不变

## 🧪 测试建议

```java
@Test
public void testTrainLoadingStrategy() {
    TrainLoadingStrategy strategy = new TrainLoadingStrategy();
    // 准备测试数据
    List<LocationPoint> recordPoints = ...;
    List<LocationPoint> historyPoints = ...;
    
    // 测试事件检测
    EventState result = strategy.detectEvent(recordPoints, historyPoints);
    
    // 验证结果
    assertEquals(BoardingDetector.Event.ARRIVED_BOARDING, result.getEvent());
}
```

## 📌 注意事项

1. **Spring 管理**：所有策略类都由 Spring 管理（`@Component`），确保依赖注入正常工作
2. **状态管理**：每个策略类维护自己的内部状态，注意多线程环境下的状态隔离
3. **性能**：策略获取通过工厂模式缓存，无额外性能开销

## 🚀 未来扩展方向

1. **地跑装卸区域配置**：在 `ZoneChecker` 中添加地跑作业区域的判断逻辑
2. **地跑检测逻辑实现**：完善 `TrainLoadingStrategy.detectGroundVehicleEvent()` 方法
3. **区域配置可视化**：提供界面配置不同业务的作业区域
4. **事件类型策略化**：考虑将事件类型（EventKind）也纳入策略管理
5. **策略配置化**：通过配置文件动态加载策略

## 🎯 核心设计理念

### 动态识别 vs 预设类型

传统方式（不适用于火车和地跑）：
```
预设车辆类型 → 使用对应策略检测
```

新架构（适用于火车和地跑）：
```
实时位置 → 判断所在区域 → 动态选择检测逻辑
```

### 适用场景对比

| 装卸类型 | 识别方式 | 原因 |
|---------|---------|------|
| 🚂 火车装卸 | 动态区域判断 | 与地跑无法在卡号层面区分 |
| 🏃 地跑装卸 | 动态区域判断 | 与火车无法在卡号层面区分 |
| 🚛 板车装卸 | 预设车辆类型 | 可通过卡号或其他方式预先识别 |

---

**重构完成时间**：2025-10-20  
**重构负责人**：AI Assistant  
**审核建议**：建议进行充分的集成测试，确保各种装卸业务正常工作

