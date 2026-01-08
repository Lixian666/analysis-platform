package com.jwzt.modules.experiment.utils;

import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.vo.VisionLocationMatchResult;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    /**
     * 默认时间间隔阈值（毫秒）：30秒
     */
    private static final long DEFAULT_TIME_INTERVAL_MS = 30 * 1000L;

    /**
     * 两数据相邻时间间隔：10秒
     */
    private static final long DEFAULT_TIME_INTERVAL_MS_SHORT = 40 * 1000L;
    
    /**
     * 匹配距离阈值（米）：5米
     */
    private static final double MATCH_DISTANCE_THRESHOLD = 8.0;
    
    /**
     * 历史定位数据存储（线程安全）
     * Key: cardId, Value: 定位点列表（按时间排序）
     */
    private final Map<String, List<LocationPoint>> historyLocationData = new ConcurrentHashMap<>();
    
    /**
     * 历史视觉识别数据存储（线程安全）
     * 按时间排序
     */
    private final List<VisionEvent> historyVisionData = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * 视觉数据分组结果缓存（用于动态时间间隔计算）
     */
    private final List<List<VisionEvent>> visionGroups = Collections.synchronizedList(new ArrayList<>());
    
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
     * 
     * @param newVisionEvents 新的视觉识别事件列表
     */
    public void appendVisionData(List<VisionEvent> newVisionEvents) {
        if (newVisionEvents == null || newVisionEvents.isEmpty()) {
            return;
        }

        historyVisionData.addAll(newVisionEvents);
        // 创建去重后的列表
        List<VisionEvent> uniqueVisionEvents = historyVisionData.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(VisionEvent::getId))),
                        ArrayList::new
                ));
        // 清空原列表并添加去重数据
        historyVisionData.clear();
        historyVisionData.addAll(uniqueVisionEvents);

        // 按时间排序
        historyVisionData.sort(Comparator.comparingLong(this::getVisionEventTimestamp));
        
        log.debug("追加视觉识别数据，新增: {} 条, 历史总数: {}", 
                newVisionEvents.size(), historyVisionData.size());
    }
    
    /**
     * 执行匹配：将视觉识别数据与定位数据进行匹配
     * 
     * @return 匹配结果列表
     */
    public List<VisionLocationMatchResult> matchVisionWithLocation() {
        if (historyVisionData.isEmpty()) {
            log.debug("历史视觉识别数据为空，跳过匹配");
            return new ArrayList<>();
        }
        
        // 1. 对视觉识别数据进行分组
        List<List<VisionEvent>> groups = groupVisionEventsByTime();
        
        // 2. 对每个分组进行匹配
        List<VisionLocationMatchResult> results = new ArrayList<>();
        for (List<VisionEvent> group : groups) {
            VisionLocationMatchResult result = matchGroupWithLocation(group);
            if (result != null && !result.getMatchedLocationPoints().isEmpty()) {
                results.add(result);
            }
        }
        
        log.info("匹配完成，视觉数据组数: {}, 匹配成功组数: {}", groups.size(), results.size());
        return results;
    }
    
    /**
     * 将视觉识别数据按时间分组（动态时间间隔）
     * 
     * @return 分组后的视觉事件列表
     */
    private List<List<VisionEvent>> groupVisionEventsByTime() {
        if (historyVisionData.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<List<VisionEvent>> groups = new ArrayList<>();
        List<VisionEvent> currentGroup = new ArrayList<>();
        
        for (int i = 0; i < historyVisionData.size(); i++) {
            VisionEvent current = historyVisionData.get(i);
            
            if (currentGroup.isEmpty()) {
                // 第一组，直接添加
                currentGroup.add(current);
            } else {
                // 判断是否与前一个数据时间间隔足够大
                VisionEvent lastInGroup = currentGroup.get(currentGroup.size() - 1);
                long timeDiff = getVisionEventTimestamp(current) - getVisionEventTimestamp(lastInGroup);
                
                // 动态计算时间间隔阈值
                long threshold = calculateDynamicTimeInterval(currentGroup, i);
                
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
        visionGroups.clear();
        visionGroups.addAll(groups);
        
        log.debug("视觉数据分组完成，总组数: {}", groups.size());
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
     * @return 时间间隔阈值（毫秒）
     */
    private long calculateDynamicTimeInterval(List<VisionEvent> currentGroup, int nextIndex) {
        // 默认阈值
        long threshold = DEFAULT_TIME_INTERVAL_MS;
        
        // 计算当前组最后1-2个数据的时间间隔
        long groupInternalInterval = 0;
        if (currentGroup.size() >= 2) {
            VisionEvent last = currentGroup.get(currentGroup.size() - 1);
            VisionEvent secondLast = currentGroup.get(currentGroup.size() - 2);
            groupInternalInterval = getVisionEventTimestamp(last) - getVisionEventTimestamp(secondLast);
        } else if (currentGroup.size() == 1 && historyVisionData.size() > nextIndex) {
            // 如果当前组只有一个数据，且还有下一个数据，计算与下一个数据的间隔
            VisionEvent current = currentGroup.get(0);
            VisionEvent next = historyVisionData.get(nextIndex);
            groupInternalInterval = getVisionEventTimestamp(next) - getVisionEventTimestamp(current);
        }
        
        // 计算下一组前1-2个数据的时间间隔
        long nextGroupInternalInterval = 0;
        if (historyVisionData.size() > nextIndex + 1) {
            VisionEvent next = historyVisionData.get(nextIndex);
            VisionEvent nextNext = historyVisionData.get(nextIndex + 1);
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
        // 将visionGroup中的eventTime取出最大值和最小值，然后输出字符串 最小值-最大值
        long minTime = visionGroup.stream().mapToLong(v -> getVisionEventTimestamp(v)).min().orElse(0L);
        long maxTime = visionGroup.stream().mapToLong(v -> getVisionEventTimestamp(v)).max().orElse(0L);
        String timeRange = DateTimeUtils.timestampToDateTimeStr(minTime) + "-" + DateTimeUtils.timestampToDateTimeStr(maxTime);
        Map<String, List<LocationPoint>> locationData = new HashMap<>(historyLocationData);
        // 遍历所有视觉事件，每个事件只取最优的一个定位点（距离最近，其次时间差最小，完全相同取时间戳更早的定位点）
        for (VisionEvent visionEvent : visionGroup) {
            long visionTimestamp = getVisionEventTimestamp(visionEvent);
            if (visionEvent.getLongitude() == null || visionEvent.getLatitude() == null) {
                log.warn("视觉事件缺少经纬度信息，跳过匹配。事件ID: {}", visionEvent.getId());
                continue;
            }

            VisionLocationMatchResult.MatchedLocationPoint bestMatch = null;

            // 遍历所有卡的定位数据
            for (Map.Entry<String, List<LocationPoint>> entry : locationData.entrySet()) {
                List<LocationPoint> locationPoints = entry.getValue();
                for (LocationPoint locationPoint : locationPoints) {
                    if (locationPoint.getTimestamp() == null ||
                            locationPoint.getLongitude() == null ||
                            locationPoint.getLatitude() == null) {
                        continue;
                    }

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

                    // 选择最优：先比距离，再比时间差，完全相同取定位时间更早的点
                    if (bestMatch == null
                            || distance < bestMatch.getDistance()
                            || (distance == bestMatch.getDistance() && timeDiff < bestMatch.getTimeDiff())
                            || (distance == bestMatch.getDistance() && timeDiff == bestMatch.getTimeDiff()
                                && locationPoint.getTimestamp() < bestMatch.getLocationPoint().getTimestamp())) {
                        bestMatch = new VisionLocationMatchResult.MatchedLocationPoint(
                                locationPoint.getCardUUID(), visionEvent, locationPoint, timeDiff, distance
                        );
                    }
                }
            }

            if (bestMatch != null) {
                log.warn("视觉事件【{}】匹配成功。卡ID: {},事件ID: {}，定位时间：{},事件时间：{}, 距离：{}", timeRange, bestMatch.getCardId(), visionEvent.getId(), bestMatch.getLocationPoint().getAcceptTime(), visionEvent.getEventTime(), bestMatch.getDistance());
                result.getMatchedLocationPoints().add(bestMatch);
                locationData.remove(bestMatch.getCardId());
            }
        }
        
        return result;
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
        historyVisionData.clear();
        visionGroups.clear();
        log.info("历史数据已清空");
    }
    
    /**
     * 获取历史数据统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("visionEventCount", historyVisionData.size());
        stats.put("locationCardCount", historyLocationData.size());
        stats.put("visionGroupCount", visionGroups.size());
        
        int totalLocationPoints = historyLocationData.values().stream()
                .mapToInt(List::size)
                .sum();
        stats.put("totalLocationPoints", totalLocationPoints);
        
        return stats;
    }
}

