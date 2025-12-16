package com.jwzt.modules.experiment.utils.third.zq;

import com.alibaba.fastjson2.JSONArray;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;
import com.jwzt.modules.experiment.service.ITakBeaconInfoService;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jwzt.modules.experiment.config.FilterConfig.SENSING_DISTANCE_THRESHOLD;

@Service
public class TagAndBeaconDistanceDeterminer {

    @Autowired
    private ITakBeaconInfoService takBeaconInfoService;

    @Autowired
    private RedisCache redisCache;

    private static final String BEACON_CACHE_KEY_PREFIX = "beacons:";

    /**
     * 获取信标的距离阈值
     * 如果信标的 distance 字段不为 null，使用该值
     * 如果为 null，回退到默认阈值 SENSING_DISTANCE_THRESHOLD
     * 
     * @param beacon 信标信息
     * @return 距离阈值
     */
    private double getDistanceThreshold(TakBeaconInfo beacon) {
        if (beacon.getDistance() != null) {
            // TakBeaconInfo.distance 单位是米
            return beacon.getDistance();
        }
        // 回退到默认阈值
        return SENSING_DISTANCE_THRESHOLD;
    }

    /**
     * 获取信标的距离阈值（单位：米），用于日志显示
     * 
     * @param beacon 信标信息
     * @return 距离阈值（米）
     */
    private double getDistanceThresholdInMeters(TakBeaconInfo beacon) {
        if (beacon.getDistance() != null) {
            return beacon.getDistance();
        }
        return SENSING_DISTANCE_THRESHOLD;
    }

    /**
     * 获取指定 buildId 的所有基站（优先从 Redis 获取，缓存10分钟）
     */
    private List<TakBeaconInfo> getBeaconsByBuildId(String buildId) {
        String redisKey = BEACON_CACHE_KEY_PREFIX + buildId;

        // 1. 尝试从缓存获取
        JSONArray jsonBeacons = redisCache.getCacheObject(redisKey);
        List<TakBeaconInfo> beacons = null;
        if (jsonBeacons != null){
            beacons = jsonBeacons.toJavaList(TakBeaconInfo.class);
        }

        // 2. 如果缓存没有，查询数据库并写入缓存
        if (beacons == null || beacons.isEmpty()) {
            TakBeaconInfo query = new TakBeaconInfo();
            query.setBuildId(buildId);
            query.setStatus(0);
            beacons = takBeaconInfoService.selectTakBeaconInfoList(query);

            if (beacons != null && !beacons.isEmpty()) {
                redisCache.setCacheObject(redisKey, beacons, 10, TimeUnit.MINUTES);
            }
        }
        return beacons;
    }

    /**
     * 手动刷新某个 buildId 的缓存（比如基站表有更新时调用）
     */
    public void refreshBeaconsCache(String buildId) {
        String redisKey = BEACON_CACHE_KEY_PREFIX + buildId;
        redisCache.deleteObject(redisKey);

        TakBeaconInfo query = new TakBeaconInfo();
        query.setBuildId(buildId);
        List<TakBeaconInfo> beacons = takBeaconInfoService.selectTakBeaconInfoList(query);

        if (beacons != null && !beacons.isEmpty()) {
            redisCache.setCacheObject(redisKey, beacons, 10, TimeUnit.MINUTES);
        }
    }

    /**
     * 统计靠近基站的标签数量
     */
    public int countTagsCloseToBeacons(List<LocationPoint> points, String buildId, String type, String location, String area) {
        // 1. 获取某个 buildId 下的所有 beacons
        List<TakBeaconInfo> allBeacons = getBeaconsByBuildId(buildId);
        if (allBeacons == null || allBeacons.isEmpty()) {
            return 0;
        }

        // 2. 在内存中过滤符合条件的 beacons
        List<TakBeaconInfo> matchedBeacons = allBeacons.stream()
                .filter(b -> (type == null || type.equals(b.getType())) &&
                        (location == null || location.equals(b.getLocation())) &&
                        (area == null || area.equals(b.getArea())))
                .collect(Collectors.toList());

        if (matchedBeacons.isEmpty()) {
            return 0;
        }

        // 3. 遍历所有点位，判断是否靠近任意一个匹配的基站
        int closeCount = 0;
        for (LocationPoint p : points) {
            if (p.getTagScanUwbData() == null || p.getTagScanUwbData().getUwbBeaconList().isEmpty()) {
                continue;
            }

            boolean isClose = false;
            for (TagScanUwbData.BltScanUwbBeacon beacon : p.getTagScanUwbData().getUwbBeaconList()) {
                for (TakBeaconInfo b : matchedBeacons) {
                    if (beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                        double threshold = getDistanceThreshold(b);
                        if (beacon.getDistance() < threshold) {
                            System.out.println(
                                    MessageFormat.format("⚠️ 标签【{0}】靠近基站【{1}】，距离【{2}米】，阈值【{3}米】",
                                            p.getCardUUID(), b.getName(), beacon.getDistance(), getDistanceThresholdInMeters(b)));

                            isClose = true;
                            break; // 只要靠近任意一个基站就计数
                        }
                    }
                }
                if (isClose) break;
            }

            if (isClose) {
                closeCount++;
            }
        }

        return closeCount;
    }

    /**
     * 判断标签是否靠近基站
     */
    public Boolean theTagIsCloseToTheBeacon(LocationPoint p, String buildId, String type, String location, String area) {
        // 1. 获取某个 buildId 下的所有 beacons
        List<TakBeaconInfo> allBeacons = getBeaconsByBuildId(buildId);

        if (allBeacons == null || allBeacons.isEmpty()) {
            return false;
        }

        // 2. 在内存中过滤符合条件的 beacons
        List<TakBeaconInfo> matchedBeacons = allBeacons.stream()
                .filter(b -> (type == null || type.equals(b.getType())) &&
                        (location == null || location.equals(b.getLocation())) &&
                        (area == null || area.equals(b.getArea())))
                .collect(Collectors.toList());

        // 3. 判断标签是否靠近基站
        if (p.getTagScanUwbData() != null && p.getTagScanUwbData().getUwbBeaconList().size() > 0) {
            for (TagScanUwbData.BltScanUwbBeacon beacon : p.getTagScanUwbData().getUwbBeaconList()) {
                if (beacon.getUwbBeaconMac().equals("1918FD01385B")){
                    System.out.println(
                            MessageFormat.format("⚠️ 检测到标签【{0}】与基站id【{1}】距离为【{2}】米",
                                    p.getCardUUID(), beacon.getUwbBeaconMac(), beacon.getDistance()));
                }
                for (TakBeaconInfo b : matchedBeacons) {
                    System.out.println(
                            MessageFormat.format("⚠️ 检测到标签【{0}】与基站id【{1}】距离为【{2}】米",
                                    p.getCardUUID(), beacon.getUwbBeaconMac(), beacon.getDistance()));
                    if (beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                        double threshold = getDistanceThreshold(b);
                        if (beacon.getDistance() < threshold) {
                            System.out.println(
                                    MessageFormat.format("⚠️ 检测到标签【{0}】与基站【{1}】距离小于【{2}】米，距离为【{3}米】",
                                            p.getCardUUID(), b.getName(), getDistanceThresholdInMeters(b), beacon.getDistance()));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断标签靠近最近基站的距离
     */
    public double getTagDistanceToNearestBeacon(LocationPoint p, String buildId, String type, String location, String area) {
        // 1. 获取某个 buildId 下的所有 beacons
        List<TakBeaconInfo> allBeacons = getBeaconsByBuildId(buildId);
        if (allBeacons == null || allBeacons.isEmpty()) {
            return -1;
        }

        // 2. 筛选符合条件的 beacons
        List<TakBeaconInfo> matchedBeacons = allBeacons.stream()
                .filter(b -> (type == null || type.equals(b.getType())) &&
                        (location == null || location.equals(b.getLocation())) &&
                        (area == null || area.equals(b.getArea())))
                .collect(Collectors.toList());
        if (matchedBeacons.isEmpty()) {
            return -1;
        }

        // 3. 遍历标签扫描到的基站，找出符合阈值的最近距离
        double minDistance = Double.MAX_VALUE;
        if (p.getTagScanUwbData() != null && p.getTagScanUwbData().getUwbBeaconList() != null) {
            for (TagScanUwbData.BltScanUwbBeacon beacon : p.getTagScanUwbData().getUwbBeaconList()) {
                for (TakBeaconInfo b : matchedBeacons) {
                    if (!beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                        continue;
                    }
                    double threshold = getDistanceThreshold(b);
                    double dist = beacon.getDistance();
                    if (dist < threshold && dist < minDistance) {
                        minDistance = dist;
                    }
                }
            }
        }

        // 4. 未命中阈值内基站返回 -1
        return minDistance == Double.MAX_VALUE ? -1 : minDistance;
    }

    /**
     * 判断标签是否按顺序逐渐远离基站（支持短暂波动 + 并行分析多个基站）
     *
     * @param points                    连续点集合（按时间或顺序排列）
     * @param buildId                   场所ID
     * @param type                      基站类型（可选）
     * @param location                  基站位置（可选）
     * @param area                      区域（可选）
     * @param maxAllowDecreaseCount     最大允许距离下降次数（容忍波动）
     * @param enableParallelBeaconAnalysis 是否开启并行分析多个基站
     * @return true 表示总体趋势为逐渐远离；false 表示中途明显靠近
     */
    public Boolean isTagGraduallyFarFromBeacon(List<LocationPoint> points,
                                               String buildId,
                                               String type,
                                               String location,
                                               String area,
                                               int maxAllowDecreaseCount,
                                               boolean enableParallelBeaconAnalysis) {
        if (points == null || points.size() < 2) {
            return false;
        }

        // 1. 获取某个 buildId 下的所有 beacons
        List<TakBeaconInfo> allBeacons = getBeaconsByBuildId(buildId);
        if (allBeacons == null || allBeacons.isEmpty()) {
            return false;
        }

        // 2. 筛选符合条件的 beacons
        List<TakBeaconInfo> matchedBeacons = allBeacons.stream()
                .filter(b -> (type == null || type.equals(b.getType())) &&
                        (location == null || location.equals(b.getLocation())) &&
                        (area == null || area.equals(b.getArea())))
                .collect(Collectors.toList());

        if (matchedBeacons.isEmpty()) {
            return false;
        }

        // 3. 计算每个点到最近基站的距离
        List<Double> minDistances = new ArrayList<>();

        // 使用并行流（可控）
        Stream<LocationPoint> pointStream = enableParallelBeaconAnalysis ? points.parallelStream() : points.stream();

        pointStream.forEach(p -> {
            double minDist = Double.MAX_VALUE;
            if (p.getTagScanUwbData() != null && !p.getTagScanUwbData().getUwbBeaconList().isEmpty()) {
                for (TagScanUwbData.BltScanUwbBeacon beacon : p.getTagScanUwbData().getUwbBeaconList()) {
                    for (TakBeaconInfo b : matchedBeacons) {
                        if (beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                            double dist = beacon.getDistance();
                            if (dist < minDist) {
                                minDist = dist;
                            }
                        }
                    }
                }
            }

            synchronized (minDistances) { // 防止并行流下多线程竞争
                if (minDist < Double.MAX_VALUE) {
                    minDistances.add(minDist);
                }
            }
        });

        // 4. 如果数据不足则直接返回
        if (minDistances.size() < 2) {
            return false;
        }

        // 5. 按原顺序比较距离变化趋势
        int decreaseCount = 0;
        for (int i = 1; i < minDistances.size(); i++) {
            double prev = minDistances.get(i - 1);
            double curr = minDistances.get(i);

            System.out.println(MessageFormat.format("点 {0}->{1} 距离变化：{2} → {3} 米", i - 1, i, prev / 10000.0, curr / 10000.0));

            if (curr < prev) {
                decreaseCount++;
                System.out.println(MessageFormat.format("⚠️ 距离减小：{0}→{1}（第 {2} 次波动）", prev / 10000.0, curr / 10000.0, decreaseCount));
                if (decreaseCount > maxAllowDecreaseCount) {
                    System.out.println("❌ 超过最大允许波动次数，判定为未持续远离");
                    return false;
                }
            }
        }

        if (decreaseCount == 0) {
            System.out.println("✅ 距离持续增大，标签逐渐远离基站");
        } else {
            System.out.println(MessageFormat.format("✅ 存在 {0} 次轻微波动，但总体趋势为远离", decreaseCount));
        }

        return true;
    }

    /**
     * 获取所有距离判定成功的基站详细信息列表（可含重复，用于后续统计）
     * @param points 打点列表
     * @param buildId 建筑ID
     * @param type 类型
     * @param location 位置
     * @param area 区域
     * @return 被判定距离合格的beacon列表（可含重复，便于统计出现频率）
     */
    public List<TakBeaconInfo> getBeaconsInRangeForPoints(List<LocationPoint> points, String buildId, String type, String location, String area) {
        List<TakBeaconInfo> result = new ArrayList<>();
        List<TakBeaconInfo> allBeacons = getBeaconsByBuildId(buildId);
        if (allBeacons == null || allBeacons.isEmpty()) {
            return result;
        }
        List<TakBeaconInfo> matchedBeacons = allBeacons.stream()
                .filter(b -> (type == null || type.equals(b.getType())) &&
                        (location == null || location.equals(b.getLocation())) &&
                        (area == null || area.equals(b.getArea())))
                .collect(Collectors.toList());
        if (matchedBeacons.isEmpty()) {
            return result;
        }
        for (LocationPoint p : points) {
            if (p.getTagScanUwbData() == null || p.getTagScanUwbData().getUwbBeaconList().isEmpty()) {
                continue;
            }
            for (com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData.BltScanUwbBeacon beacon : p.getTagScanUwbData().getUwbBeaconList()) {
                for (TakBeaconInfo b : matchedBeacons) {
                    if (beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                        double threshold = getDistanceThreshold(b);
                        if (beacon.getDistance() < threshold) {
                            result.add(b);
                        }
                    }
                }
            }
        }
        return result;
    }
}
