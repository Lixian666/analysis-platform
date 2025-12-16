package com.jwzt.modules.experiment.utils.third.zq;

import com.alibaba.fastjson2.JSONArray;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FusionData {
    // 时间差阈值（毫秒）
    private static final long TIME_WINDOW = 1000;

    /**
     * 融合定位数据与标签数据
     *
     * @param locationDataList 定位数据列表
     * @param tagDataList      标签数据的 JSON 数组
     * @return 融合后的定位点列表（每个点可能带有对应的 tag 数据）
     */
    public static List<LocationPoint> processesFusionLocationDataAndTagData(
            List<LocationPoint> locationDataList, JSONArray tagDataList) {

        List<TagScanUwbData> tagList = tagDataList.toJavaList(TagScanUwbData.class);

        // 对 tagList 中所有 BltScanUwbBeacon 的 distance 除以 10000
        for (TagScanUwbData tag : tagList) {
            if (tag.getUwbBeaconList() != null) {
                for (TagScanUwbData.BltScanUwbBeacon beacon : tag.getUwbBeaconList()) {
                    if (beacon.getDistance() != null) {
                        beacon.setDistance(beacon.getDistance() / 1000.0);
                    }
                }
            }
        }

        // 用于快速索引：按 uwbBeaconMac 分组（或用 bltMac，如果你的业务以此为主键）
        Map<String, List<TagScanUwbData>> tagBuffer = new ConcurrentHashMap<>();
        for (TagScanUwbData tag : tagList) {
            tagBuffer.computeIfAbsent(tag.getBltMac(), k -> new ArrayList<>()).add(tag);
        }

        // 结果集
        List<LocationPoint> fusedResults = new ArrayList<>();

        for (LocationPoint loc : locationDataList) {
            // 这里假设 cardUUID 与 uwbBeaconMac 能对应上
            String key = loc.getCardUUID();
            if (key == null) {
                fusedResults.add(loc);
                continue;
            }

            List<TagScanUwbData> relatedTags = tagBuffer.getOrDefault(key, Collections.emptyList());

            TagScanUwbData bestMatch = null;
            long minTimeDiff = Long.MAX_VALUE;

            for (TagScanUwbData tag : relatedTags) {
                long diff = Math.abs(tag.getTime() - loc.getTimestamp());
                if (diff <= TIME_WINDOW && diff < minTimeDiff) {
                    minTimeDiff = diff;
                    bestMatch = tag;
                }
            }

            // 找到合适的标签，绑定进去
            if (bestMatch != null) {
                Double speedKmh = bestMatch.getGnssInfo().getSpeedKmh();
                if (speedKmh != null) {
                    loc.setThirdSpeed(speedKmh / 3.6);
                }else {
                    loc.setThirdSpeed(0.0);
                }
                loc.setTagScanUwbData(bestMatch);
            }
            fusedResults.add(loc);
        }

        return fusedResults;
    }
}
