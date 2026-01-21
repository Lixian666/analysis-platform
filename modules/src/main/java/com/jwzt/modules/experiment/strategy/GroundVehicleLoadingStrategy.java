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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 地跑装卸策略实现（预留）
 * TODO: 根据实际业务需求实现具体的检测逻辑
 */
@Component
@Scope("prototype")
public class GroundVehicleLoadingStrategy implements LoadingUnloadingStrategy {

    private static final Logger log = LoggerFactory.getLogger(GroundVehicleLoadingStrategy.class);

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ZoneChecker zoneChecker;

    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;

    private BoardingDetector.Event lastEvent = BoardingDetector.Event.NONE;
    private BoardingDetector.Event currentEvent = BoardingDetector.Event.NONE;
    private LocationPoint curPoint = null;
    private EventState lastEventState = new EventState();
    private EventState carSendOutLastEventState = null;
    private EventState carSendInLastEventState = null;
    private Boolean inTheTrafficCar = false;
    
    @Override
    public EventState detectEventAlready(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status) {
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
        // 判断是否在货运线区域（到达上车区域）
        //        boolean isTheFreightLineArea = zoneChecker.isInHuoyunxinZone(currentPoint);
        // 判断是否在停车区域（发运上车区域）
        boolean isnParkingArea = zoneChecker.isInParkingZone(currentPoint);
        boolean inTheTrafficCar = isInTheTrafficCar(theFirstTenPoints, theLastTenPoints);
        // 地跑发运上车事件
        if (status == 0
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && isnParkingArea
                && !inTheTrafficCar) {

            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            int parkTags = 0;
            boolean isParking = false;

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

            // 判断到达下车点前5个点状态
            for (int i = 0; i < 5; i++) {
                LocationPoint point = theFirstTenPoints.get(i);
                if (point.getSpeed() < FilterConfig.MIN_WALKING_SPEED){
                    parkTags++;
                }else {
                    parkTags = 0;
                }
                if (!isParking && parkTags >= 2){
                    isParking = true;
                }
            }

            // 判断状态标签数量是否满足发运区域上车条件
            if (arrivedFirstTag >= FilterConfig.SEND_BEFORE_UP_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.SEND_AFTER_UP_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE
                    && isParking) {
                log.warn("⚠️ 检测到发运已上车（地跑）,卡ID:【{}】,时间:【{}】", currentPoint.getCardId(), currentPoint.getAcceptTime());
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.CAR_SEND_BOARDING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                carSendOutLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }

        // 地跑到达下车事件
        // 检测到达下车事件
        if (status == 1
                && currentPoint.getState() == MovementAnalyzer.MovementState.STOPPED
                && isnParkingArea
                && !inTheTrafficCar) {

            int arrivedStoppedTag = 0;
            int arrivedDrivingTag = 0;
            int arrivedFirstTag = 0;
            int arrivedLastTag = 0;
            int parkTags = 0;
            boolean isParking = false;

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
            // 判断到达下车点前5个点状态
            for (int i = 0; i < 5; i++) {
                LocationPoint point = theLastTenPoints.get(i);
                if (point.getSpeed() < FilterConfig.MIN_WALKING_SPEED){
                    parkTags++;
                }else {
                    parkTags = 0;
                }
                if (!isParking && parkTags >= 2){
                    isParking = true;
                }
            }

            // 判断状态标签数量是否满足到达区域下车条件
            if (arrivedFirstTag >= FilterConfig.ARRIVED_BEFORE_DOWN_STATE_SIZE
                    && arrivedLastTag >= FilterConfig.ARRIVED_AFTER_DOWN_STATE_SIZE
                    && arrivedStoppedTag >= FilterConfig.STOPPED_STATE_SIZE
                    && arrivedDrivingTag >= FilterConfig.DRIVING_STATE_SIZE
                    && isParking) {
                log.warn("⚠️ 检测到到达已下车（地跑）,卡ID:【{}】,时间:【{}】", currentPoint.getCardId(), currentPoint.getAcceptTime());
                curPoint = currentPoint;
                lastEvent = BoardingDetector.Event.NONE;
                currentEvent = BoardingDetector.Event.CAR_ARRIVED_DROPPING;
                lastEventState = new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime());
                carSendInLastEventState = null;
                return new EventState(currentEvent, currentPoint.getTimestamp(), currentPoint.getAcceptTime(), currentPoint.getLongitude(), currentPoint.getLatitude());
            }
        }
        return new EventState();
    }

    @Override
    public EventState detectEvent(List<LocationPoint> recordPoints, List<LocationPoint> historyPoints, Integer status) {
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
        return zoneChecker.isInParkingZone(currentPoint);
    }

    private boolean isInTheTrafficCar(List<LocationPoint> theFirstTenPoints, List<LocationPoint> theLastTenPoints) {
        List<LocationPoint> theFirstJTCList = theFirstTenPoints.subList(theFirstTenPoints.size() - 5, theFirstTenPoints.size());
        List<LocationPoint> theLastJTCList = theLastTenPoints.subList(0, 10);
        List<LocationPoint> theJTCList = new ArrayList<>();
//        theJTCList.addAll(theFirstJTCList);
        theJTCList.addAll(theLastJTCList);
        int theLastTenPointsNotInTOJTCCount = tagBeacon.countTagsCloseToBeacons(
                theJTCList,
                baseConfig.getJoysuch().getBuildingId(),
                "交通车",
                null,
                null,
                null,
                true);
        // 获取交通车数
        if (carSendOutLastEventState == null || carSendInLastEventState != null) {
            // 判断是否在交通车上
            if (theLastTenPointsNotInTOJTCCount >= FilterConfig.TRAFFICCAR_STATE_SIZE) {
                inTheTrafficCar = true;
            } else {
                inTheTrafficCar = false;
            }
        }
        return inTheTrafficCar;
    }
}

