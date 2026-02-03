package com.jwzt.modules.experiment.utils;

import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.vo.VisionLocationMatchResult;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.TagAndBeaconDistanceDeterminer;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 视觉识别与定位数据匹配服务
 * 
 * @author lx
 * @date 2025-01-20
 */
@Component
public class VisionLocationMatcher {
    
    private static final Logger log = LoggerFactory.getLogger(VisionLocationMatcher.class);

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private TagAndBeaconDistanceDeterminer tagBeacon;

    /**
     * 默认时间间隔阈值（毫秒）：30秒
     */
    private static final long DEFAULT_TIME_INTERVAL_MS = 30 * 1000L;

    /**
     * 两数据相邻时间间隔：10秒
     */
    private static final long DEFAULT_TIME_INTERVAL_MS_SHORT = 10 * 1000L;

    /**
     * 两相同卡号数据相邻时间间隔：10秒
     */
    private static final long DEFAULT_TIME_INTERVAL_MS_SAME_CARD = 20 * 1000L;

    
    /**
     * 匹配距离阈值（米）：15米
     */
    private static final double MATCH_DISTANCE_THRESHOLD = 15.0;
    
    /**
     * 历史定位数据存储（线程安全）
     * Key: cardId, Value: 定位点列表（按时间排序）
     */
    private final Map<String, List<LocationPoint>> historyLocationData = new ConcurrentHashMap<>();

    /**
     * 历史视觉识别数据存储（线程安全）
     * 按车辆类型分开存储：train/car/truck
     * Key: 车辆类型（train/car/truck）, Value: 视觉事件列表（按时间排序）
     */
    private final Map<String, List<VisionEvent>> historyVisionDataByType = new ConcurrentHashMap<>();
    
    /**
     * 视觉数据分组结果缓存（用于动态时间间隔计算）
     * 按车辆类型分开存储：train/car/truck
     * Key: 车辆类型（train/car/truck）, Value: 分组列表
     */
    private final Map<String, List<List<VisionEvent>>> visionGroupsByType = new ConcurrentHashMap<>();
    
    /**
     * 追加新的定位数据到历史数据
     * 
     * @param cardId 卡ID
     * @param newLocationPoints 新的定位点列表
     */
    public void appendLocationData(String cardId, List<LocationPoint> newLocationPoints) {
        if (newLocationPoints == null || newLocationPoints.isEmpty()) {
            return;
        }
        
        historyLocationData.compute(cardId, (key, existingList) -> {
            if (existingList == null) {
                existingList = new ArrayList<>();
            }
            existingList.addAll(newLocationPoints);
            // 组合键去重：cardUUID + longitude + latitude + timestamp + tagScanUwbData
            Map<String, LocationPoint> dedup = new LinkedHashMap<>();
            for (LocationPoint lp : existingList) {
                String dedupKey = buildLocationDedupKey(lp);
                dedup.putIfAbsent(dedupKey, lp);
            }
            List<LocationPoint> result = new ArrayList<>(dedup.values());
            // 按时间戳排序
            result.sort(Comparator.comparingLong(LocationPoint::getTimestamp));
            return result;
        });
        
        log.debug("追加定位数据，卡ID: {}, 新增: {} 条, 历史总数: {}", 
                cardId, newLocationPoints.size(), 
                historyLocationData.get(cardId).size());
    }
    
    /**
     * 追加新的视觉识别数据到历史数据
     * 根据event字段自动分类到不同车辆类型
     * 
     * @param newVisionEvents 新的视觉识别事件列表
     */
    public void appendVisionData(List<VisionEvent> newVisionEvents) {
        if (newVisionEvents == null || newVisionEvents.isEmpty()) {
            return;
        }

        // 按车辆类型分组
        Map<String, List<VisionEvent>> groupedByType = new HashMap<>();
        for (VisionEvent event : newVisionEvents) {
            String vehicleType = getVehicleTypeFromEvent(event);
            groupedByType.computeIfAbsent(vehicleType, k -> new ArrayList<>()).add(event);
        }

        // 分别追加到对应类型的历史数据中
        for (Map.Entry<String, List<VisionEvent>> entry : groupedByType.entrySet()) {
            String vehicleType = entry.getKey();
            List<VisionEvent> events = entry.getValue();
            
            historyVisionDataByType.compute(vehicleType, (key, existingList) -> {
                if (existingList == null) {
                    existingList = Collections.synchronizedList(new ArrayList<>());
                }
                existingList.addAll(events);
                
                // 去重（按ID）
                List<VisionEvent> uniqueEvents = existingList.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(VisionEvent::getId))),
                                ArrayList::new
                        ));
                
                // 按时间排序
                uniqueEvents.sort(Comparator.comparingLong(this::getVisionEventTimestamp));
                
                return Collections.synchronizedList(uniqueEvents);
            });
            
            log.debug("追加视觉识别数据，车辆类型: {}, 新增: {} 条, 历史总数: {}", 
                    vehicleType, events.size(), 
                    historyVisionDataByType.get(vehicleType).size());
        }
    }
    
    /**
     * 根据VisionEvent的event字段判断车辆类型
     * 
     * @param event 视觉事件
     * @return 车辆类型：train/car/truck
     */
    private String getVehicleTypeFromEvent(VisionEvent event) {
        if (event == null || event.getEventType() == null) {
            return "train"; // 默认火车
        }
        
        String eventType = event.getEventType();
        if ("load".equals(eventType) || "unload".equals(eventType)) {
            return "train";
        } else if ("gateCommodityVehicleInput".equals(eventType) || "gateCommodityVehicleOutput".equals(eventType)) {
            return "car";
        } else if ("gateBancheInput".equals(eventType) || "gateBancheOutput".equals(eventType)) {
            return "truck";
        }
        
        return "train"; // 默认火车
    }
    
    /**
     * 执行匹配：将视觉识别数据与定位数据进行匹配
     * 按车辆类型分别匹配
     * 
     * @return 匹配结果列表
     */
    public List<VisionLocationMatchResult> matchVisionWithLocation() {
        List<VisionLocationMatchResult> allResults = new ArrayList<>();
        
        // 遍历所有车辆类型，分别进行匹配
        for (Map.Entry<String, List<VisionEvent>> entry : historyVisionDataByType.entrySet()) {
            String vehicleType = entry.getKey();
            List<VisionEvent> visionData = entry.getValue();
            
            if (visionData == null || visionData.isEmpty()) {
                log.debug("车辆类型 {} 的历史视觉识别数据为空，跳过匹配", vehicleType);
                continue;
            }
            
            // 1. 对该类型的视觉识别数据进行分组
            List<List<VisionEvent>> groups = groupVisionEventsByTime(vehicleType, visionData);
            
            // 2. 对每个分组进行匹配
            for (List<VisionEvent> group : groups) {
                VisionLocationMatchResult result = matchGroupWithLocation(group);
                if (result != null && !result.getMatchedLocationPoints().isEmpty()) {
                    allResults.add(result);
                }
            }
            
            log.info("车辆类型 {} 匹配完成，视觉数据组数: {}, 匹配成功组数: {}", 
                    vehicleType, groups.size(), 
                    allResults.stream().filter(r -> getVehicleTypeFromEvent(r.getVisionEventGroup().get(0)).equals(vehicleType)).count());
        }
        
        return allResults;
    }
    
    /**
     * 将视觉识别数据按时间分组（动态时间间隔）
     * 
     * @param vehicleType 车辆类型
     * @param visionData 该类型的视觉数据
     * @return 分组后的视觉事件列表
     */
    private List<List<VisionEvent>> groupVisionEventsByTime(String vehicleType, List<VisionEvent> visionData) {
        if (visionData == null || visionData.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<List<VisionEvent>> groups = new ArrayList<>();
        List<VisionEvent> currentGroup = new ArrayList<>();
        
        for (int i = 0; i < visionData.size(); i++) {
            VisionEvent current = visionData.get(i);
            
            if (currentGroup.isEmpty()) {
                // 第一组，直接添加
                currentGroup.add(current);
            } else {
                // 判断是否与前一个数据时间间隔足够大
                VisionEvent lastInGroup = currentGroup.get(currentGroup.size() - 1);
                long timeDiff = getVisionEventTimestamp(current) - getVisionEventTimestamp(lastInGroup);
                
                // 动态计算时间间隔阈值
                long threshold = calculateDynamicTimeInterval(currentGroup, i, visionData);
                
                if (timeDiff > threshold) {
                    // 时间间隔足够大，开始新的一组
                    groups.add(new ArrayList<>(currentGroup));
                    currentGroup.clear();
                }
                currentGroup.add(current);
            }
        }
        
        // 添加最后一组
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        
        // 更新缓存
        visionGroupsByType.put(vehicleType, Collections.synchronizedList(new ArrayList<>(groups)));
        
        log.debug("车辆类型 {} 视觉数据分组完成，总组数: {}", vehicleType, groups.size());
        return groups;
    }

    /**
     * 构造定位点唯一键，用于去重
     */
    private String buildLocationDedupKey(LocationPoint lp) {
        String card = lp.getCardUUID();
        Double lon = lp.getLongitude();
        Double lat = lp.getLatitude();
        Long ts = lp.getTimestamp();
        Object tag = lp.getTagScanUwbData();
        return String.format("%s|%s|%s|%s|%s",
                card == null ? "null" : card,
                lon == null ? "null" : lon,
                lat == null ? "null" : lat,
                ts == null ? "null" : ts,
                tag == null ? "null" : tag.toString());
    }
    
    /**
     * 动态计算时间间隔阈值
     * 
     * @param currentGroup 当前组的数据
     * @param nextIndex 下一个数据的索引
     * @param visionData 完整的视觉数据列表
     * @return 时间间隔阈值（毫秒）
     */
    private long calculateDynamicTimeInterval(List<VisionEvent> currentGroup, int nextIndex, List<VisionEvent> visionData) {
        // 默认阈值
        long threshold = DEFAULT_TIME_INTERVAL_MS;
        
        // 计算当前组最后1-2个数据的时间间隔
        long groupInternalInterval = 0;
        if (currentGroup.size() >= 2) {
            VisionEvent last = currentGroup.get(currentGroup.size() - 1);
            VisionEvent secondLast = currentGroup.get(currentGroup.size() - 2);
            groupInternalInterval = getVisionEventTimestamp(last) - getVisionEventTimestamp(secondLast);
        } else if (currentGroup.size() == 1 && visionData.size() > nextIndex) {
            // 如果当前组只有一个数据，且还有下一个数据，计算与下一个数据的间隔
            VisionEvent current = currentGroup.get(0);
            VisionEvent next = visionData.get(nextIndex);
            groupInternalInterval = getVisionEventTimestamp(next) - getVisionEventTimestamp(current);
        }
        
        // 计算下一组前1-2个数据的时间间隔
        long nextGroupInternalInterval = 0;
        if (visionData.size() > nextIndex + 1) {
            VisionEvent next = visionData.get(nextIndex);
            VisionEvent nextNext = visionData.get(nextIndex + 1);
            nextGroupInternalInterval = getVisionEventTimestamp(nextNext) - getVisionEventTimestamp(next);
        }
        
        // 取较大值作为阈值，但至少为默认值
        if (groupInternalInterval > 0 || nextGroupInternalInterval > 0) {
            threshold = Math.max(Math.max(groupInternalInterval, nextGroupInternalInterval), DEFAULT_TIME_INTERVAL_MS);
        }
        
        return threshold;
    }
    
    /**
     * 将一组视觉事件与所有定位数据进行匹配
     * 
     * @param visionGroup 视觉事件组
     * @return 匹配结果
     */
    private VisionLocationMatchResult matchGroupWithLocation(List<VisionEvent> visionGroup) {
        VisionLocationMatchResult result = new VisionLocationMatchResult();
        result.setVisionEventGroup(visionGroup);
        List<VisionEvent> notMatches = new ArrayList<>();
        // 将visionGroup中的eventTime取出最大值和最小值，然后输出字符串 最小值-最大值
        long minTime = visionGroup.stream().mapToLong(v -> getVisionEventTimestamp(v)).min().orElse(0L);
        long maxTime = visionGroup.stream().mapToLong(v -> getVisionEventTimestamp(v)).max().orElse(0L);
        String timeRange = DateTimeUtils.timestampToDateTimeStr(minTime) + "-" + DateTimeUtils.timestampToDateTimeStr(maxTime);
        Map<String, List<LocationPoint>> locationData = new HashMap<>(historyLocationData);
        Map<String, List<LocationPoint>> locationDataRetry = new HashMap<>(historyLocationData);
        // 遍历所有视觉事件，每个事件只取最优的一个定位点（距离最近，其次时间差最小，完全相同取时间戳更早的定位点）
        for (VisionEvent visionEvent : visionGroup) {
            if (visionEvent.getId() == 27011L){
                log.info("开始处理视觉数据: {}", visionEvent);
            }
            getBastMatchedResults(visionEvent, locationData, result, timeRange, notMatches, true);
        }

        for (VisionEvent visionEvent : notMatches){
            if (visionEvent.getId() == 27011L){
                log.info("开始处理视觉数据: {}", visionEvent);
            }
            getBastMatchedResults(visionEvent, locationDataRetry, result, timeRange, notMatches, false);
        }
        
        return result;
    }


    /**
     * 获取视觉事件最匹配的定位点
     *
     * @param visionEvent  视觉事件
     * @param locationData 定位数据
     * @param result       匹配结果
     * @param timeRange    时间范围
     * @param unmatched    未匹配的视觉事件
     * @param again        是否再次匹配
     */
    private void getBastMatchedResults(VisionEvent visionEvent, Map<String, List<LocationPoint>> locationData, VisionLocationMatchResult result, String timeRange, List<VisionEvent> unmatched, boolean again) {
        long visionTimestamp = getVisionEventTimestamp(visionEvent);
        if (visionEvent.getLongitude() == null || visionEvent.getLatitude() == null) {
//                log.warn("视觉事件缺少经纬度信息，跳过匹配。事件ID: {}", visionEvent.getId());
            return;
        }
        if (visionEvent.getId() == 27011L){
            System.out.println("aaaa");
        }

        VisionLocationMatchResult.MatchedLocationPoint bestMatch = null;
        List<VisionLocationMatchResult.MatchedLocationPoint> bestMatchedPoints = new ArrayList<>();
        if (visionEvent.getId() == 13740L){
            System.out.println("aaaa");
        }
        // 遍历所有卡的定位数据
        for (Map.Entry<String, List<LocationPoint>> entry : locationData.entrySet()) {
            List<LocationPoint> locationPoints = entry.getValue();
            List<LocationPoint> beforePoints = new LinkedList<>();
            bestMatch = getMatchedLocationPoint(visionEvent, locationPoints, beforePoints, visionTimestamp, bestMatch, result, again);
            bestMatchedPoints.add(bestMatch);
            bestMatch = null;
            beforePoints.clear();
        }

        if (bestMatchedPoints.size() > 1) {
            bestMatch = bestMatchedPoints.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingLong(m -> m.getLocationPoint().getTimestamp()))
                .orElse(null);
        }

        if (bestMatch != null) {
//                log.warn("视觉事件【{}】匹配成功。卡ID: {},事件ID: {}，定位时间：{},事件时间：{}, 距离：{}", timeRange, bestMatch.getCardId(), visionEvent.getId(), bestMatch.getLocationPoint().getAcceptTime(), visionEvent.getEventTime(), bestMatch.getDistance());
            result.getMatchedLocationPoints().add(bestMatch);
            locationData.remove(bestMatch.getCardId());
        }
        if (again && bestMatch == null){
            log.warn("视觉事件【{}】匹配失败。事件ID: {}", timeRange, visionEvent.getId());
            unmatched.add(visionEvent);
        }
    }


    /**
     * 获取视觉事件与一张卡定位数据之间的匹配结果
     *
     * @param visionEvent 视觉事件
     * @param locationPoints 定位数据
     * @param beforePoints 前10个定位数据
     * @param visionTimestamp 视觉事件时间戳
     * @param bestMatch 最佳匹配结果
     * @return 匹配结果
     */
    private VisionLocationMatchResult.MatchedLocationPoint getMatchedLocationPoint(VisionEvent visionEvent, List<LocationPoint> locationPoints, List<LocationPoint> beforePoints, long visionTimestamp, VisionLocationMatchResult.MatchedLocationPoint bestMatch, VisionLocationMatchResult result, boolean again) {
        for (LocationPoint locationPoint : locationPoints) {
            if (locationPoint.getTimestamp() == null ||
                    locationPoint.getLongitude() == null ||
                    locationPoint.getLatitude() == null) {
                continue;
            }
            // 创建一个集合类型是List<LocationPoint>，其中只能有3个元素，进一个新的元素就删除第一个元素
            if (beforePoints.size() >= 5) {
                beforePoints.remove(0);
            }
            beforePoints.add(locationPoint);

            // 时间匹配：允许一定的时间误差（±10秒）
            long timeDiff = Math.abs(visionTimestamp - locationPoint.getTimestamp());
            if (timeDiff > DEFAULT_TIME_INTERVAL_MS_SHORT) {
//                        log.warn("视觉事件【{}】时间匹配失败。事件ID: {}，定位时间：{},事件时间：{}", timeRange, visionEvent.getId(), locationPoint.getAcceptTime(), visionEvent.getEventTime());
                continue;
            }

            // 距离匹配：计算两点间距离
            double distance = calculateDistance(
                    visionEvent.getLongitude(), visionEvent.getLatitude(),
                    locationPoint.getLongitude(), locationPoint.getLatitude()
            );

            if (distance > MATCH_DISTANCE_THRESHOLD) {
//                        log.warn("视觉事件【{}】距离匹配失败。卡ID: {},事件ID: {}，定位时间：{},事件时间：{}, 距离：{}", timeRange, locationPoint.getCardUUID(), visionEvent.getId(), locationPoint.getAcceptTime(), visionEvent.getEventTime(), distance);
                continue;
            }

            boolean inTheTrafficCar = isInTheTrafficCar(beforePoints);
            if (inTheTrafficCar) {
//                        log.warn("视觉事件【{}】交通车匹配失败。卡ID: {},事件ID: {}，定位时间：{},事件时间：{}, 距离：{}", timeRange, locationPoint.getCardUUID(), visionEvent.getId(), locationPoint.getAcceptTime(), visionEvent.getEventTime(), distance);
                continue;
            }

            // 选择最优：先比距离，再比时间差，完全相同取定位时间更早的点
//                    if (bestMatch == null || timeDiff < bestMatch.getTimeDiff()) {
//                        bestMatch = new VisionLocationMatchResult.MatchedLocationPoint(
//                                locationPoint.getCardUUID(), visionEvent, locationPoint, timeDiff, distance
//                        );
//                    }
            if (bestMatch == null || distance < bestMatch.getDistance()) {
                if (again){
                    bestMatch = new VisionLocationMatchResult.MatchedLocationPoint(
                            locationPoint.getCardUUID(), visionEvent, locationPoint, timeDiff, distance
                    );
                }else {
                    for (VisionLocationMatchResult.MatchedLocationPoint matchedLocationPoint : result.getMatchedLocationPoints()){
                        if (matchedLocationPoint.getLocationPoint().getCardUUID().equals(locationPoint.getCardUUID())){
                            long timeDiffRetry = Math.abs(matchedLocationPoint.getLocationPoint().getTimestamp() - locationPoint.getTimestamp());
                            if (timeDiffRetry > DEFAULT_TIME_INTERVAL_MS_SAME_CARD) {
                                bestMatch = new VisionLocationMatchResult.MatchedLocationPoint(
                                        locationPoint.getCardUUID(), visionEvent, locationPoint, timeDiff, distance
                                );
                            }
                        }
                    }
                }
            }
        }
        return bestMatch;
    }

    /**
     * 计算两点间距离（米）
     * 
     * @param lon1 经度1
     * @param lat1 纬度1
     * @param lon2 经度2
     * @param lat2 纬度2
     * @return 距离（米）
     */
    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        Coordinate coord1 = new Coordinate(lon1, lat1);
        Coordinate coord2 = new Coordinate(lon2, lat2);
        return GeoUtils.distanceM(coord1, coord2);
    }

    private boolean isInTheTrafficCar(List<LocationPoint> points) {
        boolean inTheTrafficCar = false;
        int theLastTenPointsNotInTOJTCCount = tagBeacon.countTagsCloseToBeacons(
                points,
                baseConfig.getJoysuch().getBuildingId(),
                "交通车",
                null,
                null,
                null,
                true);
        // 获取交通车数
        // 判断是否在交通车上
        if (theLastTenPointsNotInTOJTCCount >= 2) {
            inTheTrafficCar = true;
        } else {
            inTheTrafficCar = false;
        }
        return inTheTrafficCar;
    }
    
    /**
     * 获取视觉事件的时间戳
     * 
     * @param visionEvent 视觉事件
     * @return 时间戳（毫秒）
     */
    private long getVisionEventTimestamp(VisionEvent visionEvent) {
        if (visionEvent.getEventTime() == null) {
            return 0L;
        }
        try {
            return DateTimeUtils.convertToTimestamp(visionEvent.getEventTime());
        } catch (Exception e) {
            log.warn("解析视觉事件时间失败: {}, 事件ID: {}", visionEvent.getEventTime(), visionEvent.getId());
            return 0L;
        }
    }
    
    /**
     * 清空历史数据（用于测试或重置）
     */
    public void clearHistoryData() {
        historyLocationData.clear();
        historyVisionDataByType.clear();
        visionGroupsByType.clear();
        log.info("历史数据已清空");
    }
    
    /**
     * 获取历史数据统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalVisionEvents = historyVisionDataByType.values().stream()
                .mapToInt(List::size)
                .sum();
        stats.put("visionEventCount", totalVisionEvents);
        stats.put("locationCardCount", historyLocationData.size());
        
        int totalVisionGroups = visionGroupsByType.values().stream()
                .mapToInt(List::size)
                .sum();
        stats.put("visionGroupCount", totalVisionGroups);
        
        int totalLocationPoints = historyLocationData.values().stream()
                .mapToInt(List::size)
                .sum();
        stats.put("totalLocationPoints", totalLocationPoints);
        
        // 按车辆类型统计
        Map<String, Integer> visionEventsByType = new HashMap<>();
        for (Map.Entry<String, List<VisionEvent>> entry : historyVisionDataByType.entrySet()) {
            visionEventsByType.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("visionEventsByType", visionEventsByType);
        
        return stats;
    }
    
    /**
     * 获取指定卡的历史定位数据
     * 返回数据的副本，保证线程安全
     * 
     * @param cardId 卡ID
     * @return 历史定位数据列表（按时间排序），如果卡不存在则返回空列表
     */
    public List<LocationPoint> getHistoryLocationDataForCard(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return new ArrayList<>();
        }
        List<LocationPoint> data = historyLocationData.get(cardId);
        if (data == null) {
            return new ArrayList<>();
        }
        // 返回副本，保证线程安全
        return new ArrayList<>(data);
    }

    /**
     * 初始化历史数据
     */
    public void initHistoryData() {
        historyLocationData.clear();
        historyVisionDataByType.clear();
        visionGroupsByType.clear();
    }
    
    /**
     * 获取指定车辆类型的历史视觉数据
     * 返回数据的副本，保证线程安全
     * 
     * @param vehicleType 车辆类型（train/car/truck）
     * @return 历史视觉事件数据列表（按时间排序），如果类型不存在则返回空列表
     */
    public List<VisionEvent> getHistoryVisionDataByType(String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) {
            return new ArrayList<>();
        }
        List<VisionEvent> data = historyVisionDataByType.get(vehicleType);
        if (data == null) {
            return new ArrayList<>();
        }
        return data;
    }
}

