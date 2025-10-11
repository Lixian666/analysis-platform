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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jwzt.modules.experiment.config.FilterConfig.SENSING_DISTANCE_THRESHOLD;

@Service
public class TagAndBeaconDistanceDeterminer {

    @Autowired
    private ITakBeaconInfoService takBeaconInfoService;

    @Autowired
    private RedisCache redisCache;

    private static final String BEACON_CACHE_KEY_PREFIX = "beacons:";

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
                for (TakBeaconInfo b : matchedBeacons) {
                    System.out.println(
                            MessageFormat.format("⚠️ 检测到标签【{0}】与基站id【{1}】距离为【{2}】米",
                                    p.getCardUUID(), beacon.getUwbBeaconMac(), beacon.getDistance() / 10000));
                    if (beacon.getUwbBeaconMac().equals(b.getBeaconId())) {
                        if (beacon.getDistance() < SENSING_DISTANCE_THRESHOLD) {
                            System.out.println(
                                    MessageFormat.format("⚠️ 检测到标签【{0}】与基站【{1}】距离小于【{2}】米，距离为【{3}米】",
                                            p.getCardUUID(), b.getName(), SENSING_DISTANCE_THRESHOLD / 10000, beacon.getDistance() / 10000));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
