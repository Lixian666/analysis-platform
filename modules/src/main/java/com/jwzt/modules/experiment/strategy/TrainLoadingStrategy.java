package com.jwzt.modules.experiment.strategy;

import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.MovementAnalyzer;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.map.ZoneChecker;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import com.jwzt.modules.experiment.vo.EventState;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jwzt.modules.experiment.config.FilterConfig.ADJACENT_POINTS_TIME_INTERVAL_MS;

/**
 * 火车和地跑组合装卸策略实现
 * 说明：火车装卸和地跑装卸在卡号层面无法区分，需根据实时位置的区域动态判断
 * 使用 updateState 方法进行事件检测
 */
@Component
public class TrainLoadingStrategy implements LoadingUnloadingStrategy {
    
    @Autowired
    private BaseConfig baseConfig;
    
    @Autowired
    private ZoneChecker zoneChecker;
    
    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;
    
    @Autowired
    private OutlierFilter outlierFilter;
    
    private TheUWBRecords theUWBRecords = new TheUWBRecords();
    
    private BoardingDetector.Event lastEvent = BoardingDetector.Event.NONE;
    private BoardingDetector.Event currentEvent = BoardingDetector.Event.NONE;
    private LocationPoint curPoint = null;
    private EventState lastEventState = new EventState();
    private EventState sendOutLastEventState = null;
    private EventState sendInLastEventState = null;
    
    private int theTrafficCarCount = 0;
    private Boolean inTheTrafficCar = false;
    
    @Data
    private static class TheUWBRecords {
        int theUWBSendDropsZytA = 0;
        long theUWBSendDropsZytALastTime = 0;
        int theUWBSendDropsZytB = 0;
        long theUWBSendDropsZytBLastTime = 0;
    }
    
    @Override
    public EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints) {
        // 第一层检查：必须至少有 RECORD_POINTS_SIZE 个点
        if (recordPoints == null || recordPoints.size() < FilterConfig.RECORD_POINTS_SIZE) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: recordPoints 为 null 或大小不足: " +
                (recordPoints == null ? "null" : recordPoints.size()) + 
                ", 需要至少 " + FilterConfig.RECORD_POINTS_SIZE);
            return new EventState();
        }
        
        // 第二层检查：确保可以安全访问 halfSize 索引
        int halfSize = FilterConfig.RECORD_POINTS_SIZE / 2;
        if (recordPoints.size() <= halfSize) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: recordPoints 大小不足以访问中间点: " +
                recordPoints.size() + ", halfSize=" + halfSize);
            return new EventState();
        }
        
        // 安全地获取子列表和中间点，使用边界保护
        List<LocationPoint> theFirstTenPoints = recordPoints.subList(0, Math.min(halfSize, recordPoints.size()));
        int currentIndex = Math.min(halfSize, recordPoints.size() - 1);
        LocationPoint currentPoint = recordPoints.get(currentIndex);
        
        // 确保计算 theLastTenPoints 时不越界
        int lastStart = Math.max(0, recordPoints.size() - halfSize);
        int lastEnd = recordPoints.size();
        if (lastStart >= lastEnd) {
            System.out.println("异常日志 ⚠️ TrainLoadingStrategy: lastStart >= lastEnd: " + lastStart + " >= " + lastEnd);
            return new EventState();
        }
        List<LocationPoint> theLastTenPoints = recordPoints.subList(lastStart, lastEnd);
        
        // 根据当前点位所在区域判断是火车装卸还是地跑装卸
        if (isInGroundVehicleZone(currentPoint)) {
            System.out.println("开始处理（地跑装卸）：" + currentPoint);
            return detectGroundVehicleEvent(recordPoints, historyPoints, theFirstTenPoints, currentPoint, theLastTenPoints);
        } else {
            System.out.println("开始处理（火车装卸）：" + currentPoint);
            return detectTrainEvent(recordPoints, historyPoints, theFirstTenPoints, currentPoint, theLastTenPoints);
        }
    }
    
    /**
     * 判断是否在地跑作业区域
     * TODO: 根据实际业务配置地跑作业区域的判断逻辑
     */
    private boolean isInGroundVehicleZone(LocationPoint currentPoint) {
        // 示例：可以通过区域配置或者特定的基站来判断
        // return zoneChecker.isInGroundVehicleZone(currentPoint);
        // 暂时返回 false，待实际业务需求时实现
        return false;
    }
    
    /**
     * 地跑装卸事件检测
     * TODO: 根据实际业务需求实现地跑装卸的具体检测逻辑
     */
    private EventState detectGroundVehicleEvent(List<LocationPoint> recordPoints,
                                                List<LocationPoint> historyPoints,
                                                List<LocationPoint> theFirstTenPoints,
                                                LocationPoint currentPoint,
                                                List<LocationPoint> theLastTenPoints) {
        // TODO: 实现地跑装卸的检测逻辑
        // 可能与火车装卸类似，但使用不同的区域和基站配置
        System.out.println("⚠️ 地跑装卸检测逻辑待实现");
        return new EventState();
    }
    
    /**
     * 火车装卸事件检测（原有逻辑）
     */
    private EventState detectTrainEvent(List<LocationPoint> recordPoints,
                                        List<LocationPoint> historyPoints,
                                        List<LocationPoint> theFirstTenPoints,
                                        LocationPoint currentPoint,
                                        List<LocationPoint> theLastTenPoints) {
        
        // 判断是否在货运线区域（到达上车区域）
        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        
        // 获取交通车数
        if (sendOutLastEventState != null || sendInLastEventState != null) {
            if (currentPoint.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                    || currentPoint.getState() == MovementAnalyzer.MovementState.DRIVING) {
                theTrafficCarCount++;
            }
        }
        
        // 判断是否在交通车上
        if (theTrafficCarCount >= FilterConfig.TRAFFICCAR_STATE_SIZE) {
            inTheTrafficCar = true;
        }
        
        // 统计最后10个点是否接近作业台J车附近
        int theLastTenPointsNotInZYTCount = tagBeacon.countTagsCloseToBeacons(
                theLastTenPoints,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A"
        );
        
        // 判断是否靠近作业台J车附近
        boolean isZYTAWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "A"
        );
        if (isZYTAWithin) {
            theUWBRecords.theUWBSendDropsZytA++;
            theUWBRecords.theUWBSendDropsZytALastTime = currentPoint.getTimestamp();
        }
        
        // 判断是否靠近作业台fid附近
        boolean isZYTBWithin = tagBeacon.theTagIsCloseToTheBeacon(
                currentPoint,
                baseConfig.getJoysuch().getBuildingId(),
                "货运线作业台",
                "2号线",
                "B"
        );
        if (isZYTBWithin) {
            theUWBRecords.theUWBSendDropsZytB++;
            theUWBRecords.theUWBSendDropsZytBLastTime = currentPoint.getTimestamp();
        }
        
        // 判断上次流程是否超时
        if (curPoint != null && currentPoint.getTimestamp() - curPoint.getTimestamp() > ADJACENT_POINTS_TIME_INTERVAL_MS) {
            // 重置状态
            lastEvent = BoardingDetector.Event.NONE;
            currentEvent = BoardingDetector.Event.NONE;
            curPoint = null;
            return new EventState(currentEvent, currentPoint.getTimestamp(), 1);
        }
        
        // 发运下车检测
        if (sendOutLastEventState == null 
                && theUWBRecords.theUWBSendDropsZytA >= FilterConfig.SEND_AFTER_DOWN_UWB_SIZE 
                && theUWBRecords.theUWBSendDropsZytB > FilterConfig.SEND_AFTER_DOWN_UWB_SIZE) {
            
            if (theLastTenPointsNotInZYTCount <= 0 
                    && theUWBRecords.theUWBSendDropsZytALastTime > theUWBRecords.theUWBSendDropsZytBLastTime) {
                
                if (lastEvent == BoardingDetector.Event.NONE) {
                    System.out.println("⚠️ 检测到车辆已进入发运下车区域（火车）");
                    System.out.println("J车附近" + theUWBRecords.theUWBSendDropsZytA + " fid附近" + theUWBRecords.theUWBSendDropsZytB);
                    lastEvent = BoardingDetector.Event.SEND_DROPPING;
                    currentEvent = BoardingDetector.Event.SEND_DROPPING;
                    sendOutLastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                    return new EventState(currentEvent, currentPoint.getTimestamp());
                }
            }
            
            // 到达上车检测
            if (theUWBRecords.theUWBSendDropsZytALastTime < theUWBRecords.theUWBSendDropsZytBLastTime) {
                if (lastEvent == BoardingDetector.Event.NONE 
                        && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                        && isTheFreightLineArea) {
                    
                    System.out.println("⚠️ 检测到车辆已进入到达上车区域（火车）");
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    
                    // 判断到达上车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints) {
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    
                    // 判断到达上车点后10个点状态
                    for (LocationPoint point : theLastTenPoints) {
                        if (point.getState() == MovementAnalyzer.MovementState.WALKING
                                || point.getState() == MovementAnalyzer.MovementState.RUNNING
                                || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
                            arrivedLastTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
                            arrivedDrivingTag++;
                        }
                    }
                    
                    // 判断状态标签数量是否满足到达区域上车条件
                    if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_UP_STATE_SIZE
                            && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_UP_STATE_SIZE
                            && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                            && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                        System.out.println("⚠️ 检测到到达已上车（火车）");
                        curPoint = currentPoint;
                        lastEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        currentEvent = BoardingDetector.Event.ARRIVED_BOARDING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
                
                // 检测到达下车事件
                if (lastEvent == BoardingDetector.Event.ARRIVED_BOARDING 
                        && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                        && isnParkingArea) {
                    
                    System.out.println("⚠️ 检测到车辆已进入到达下车区域（火车）");
                    int arrivedStoppedTag = 0;
                    int arrivedDrivingTag = 0;
                    int arrivedFirstTag = 0;
                    int arrivedLastTag = 0;
                    
                    // 判断到达下车点前10个点状态
                    for (LocationPoint point : theFirstTenPoints) {
                        if (point.getState() == MovementAnalyzer.MovementState.DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.RUNNING
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedFirstTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.DRIVING
                                || point.getState() == MovementAnalyzer.MovementState.RUNNING
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedDrivingTag++;
                        }
                    }
                    
                    // 判断到达下车点后10个点状态
                    for (LocationPoint point : theLastTenPoints) {
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                                || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                            arrivedLastTag++;
                        }
                        if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                            arrivedStoppedTag++;
                        }
                    }
                    
                    // 判断状态标签数量是否满足到达区域下车条件
                    if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_DOWN_STATE_SIZE
                            && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_DOWN_STATE_SIZE
                            && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                            && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                        System.out.println("⚠️ 检测到到达已下车（火车）");
                        lastEvent = BoardingDetector.Event.NONE;
                        currentEvent = BoardingDetector.Event.ARRIVED_DROPPING;
                        lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                        return new EventState(currentEvent, currentPoint.getTimestamp());
                    }
                }
            }
        }
        
        // 检测发运上车事件
        if (sendOutLastEventState != null 
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED 
                && isnParkingArea) {
            
            System.out.println("⚠️ 检测到车辆已进入发运上车区域（火车）");
            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            
            // 判断发运上车点前10个点状态
            for (LocationPoint point : theFirstTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED
                        || point.getState() == MovementAnalyzer.MovementState.WALKING) {
                    arrivedFirstTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.STOPPED) {
                    arrivedStoppedTag++;
                }
            }
            
            // 判断发运上车点后10个点状态
            for (LocationPoint point : theLastTenPoints) {
                if (point.getState() == MovementAnalyzer.MovementState.WALKING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING
                        || point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING) {
                    arrivedLastTag++;
                }
                if (point.getState() == MovementAnalyzer.MovementState.LOW_DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.DRIVING
                        || point.getState() == MovementAnalyzer.MovementState.RUNNING) {
                    arrivedDrivingTag++;
                }
            }
            
            // 判断状态标签数量是否满足发运区域上车条件
            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_UP_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.SEND_AFTER_UP_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE) {
                System.out.println("⚠️ 检测到发运已上车（火车）");
                resetInternalState();
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                sendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp());
            }
        }
        
        return new EventState();
    }
    
    @Override
    public void resetState() {
        resetInternalState();
        lastEvent = BoardingDetector.Event.NONE;
        currentEvent = BoardingDetector.Event.NONE;
        curPoint = null;
        lastEventState = new EventState();
        sendOutLastEventState = null;
        sendInLastEventState = null;
        theTrafficCarCount = 0;
        inTheTrafficCar = false;
    }
    
    private void resetInternalState() {
        theUWBRecords.theUWBSendDropsZytA = 0;
        theUWBRecords.theUWBSendDropsZytALastTime = 0;
        theUWBRecords.theUWBSendDropsZytB = 0;
        theUWBRecords.theUWBSendDropsZytBLastTime = 0;
    }
    
    @Override
    public String getStrategyName() {
        return "火车和地跑组合装卸策略";
    }
    
    @Override
    public boolean isInParkingArea(LocationPoint currentPoint) {
        return zoneChecker.isInParkingZone(currentPoint);
    }
}

