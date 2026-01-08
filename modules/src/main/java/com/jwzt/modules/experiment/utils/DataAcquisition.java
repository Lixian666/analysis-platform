package com.jwzt.modules.experiment.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.LocTrackRecord;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.domain.TakCardInfo;
import com.jwzt.modules.experiment.service.ILocTrackRecordService;
import com.jwzt.modules.experiment.service.ITakCardInfoService;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.beacon.BeaconFaultDetector;
import com.jwzt.modules.experiment.utils.third.zq.beacon.BeaconRepair;
import com.jwzt.modules.experiment.utils.third.zq.beacon.DriverLocation;
import com.jwzt.modules.experiment.utils.third.zq.beacon.LocationSolver;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据获取类
 * 用于获取不同厂商定位数据
 *
 * @author lx
 * @date 2025-11-13
 */
@Component
public class DataAcquisition {

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private ILocTrackRecordService locTrackRecordService;

    @Autowired
    private ITakCardInfoService takCardInfoService;

    /**
     * 获取卡列表
     *
     * @param type 业务类型
     * @return
     */
    public List<String> getCardIdList(Integer type) {
        TakCardInfo takCardInfo = new TakCardInfo();
        takCardInfo.setYardId(baseConfig.getYardName());
        takCardInfo.setType(baseConfig.getLocateDataSources());
        takCardInfo.setBizType(type);
        takCardInfo.setEnabled(0);
        List<String> carCards = takCardInfoService.selectTakCardIdList(takCardInfo);
        return carCards;
    }

    /**
     * 获取定位数据（包含信标测距）
     *
     * @param cardId
     * @param buildId
     * @param startTimeStr
     * @param endTimeStr
     * @return
     */
    public List<LocationPoint> getLocationAndUWBData(String cardId, String buildId, String startTimeStr, String endTimeStr) {
        List<LocationPoint> dataList = null;
        String vendor = baseConfig.getLocateDataSources();
        switch (vendor) {
            case "zq":
                dataList = getLocationAndUWBDataZq(cardId, buildId, startTimeStr, endTimeStr);
                break;
            case "xrkc":
                dataList = getLocationAndUWBDataXrkc(cardId, buildId, startTimeStr, endTimeStr);
                break;
            default:
                break;
        }
        return dataList;
    }

    private List<LocationPoint> getLocationAndUWBDataXrkc(String cardId, String buildId, String startTimeStr, String endTimeStr) {
        List<LocationPoint> locationPoints = new ArrayList<>();
        LocTrackRecord params = new LocTrackRecord();
        params.setCardId(cardId);
        params.setGpsIsValid("1");
        params.setQueryStartTime(startTimeStr);
        params.setQueryEndTime(endTimeStr);
        List<LocTrackRecord> locTrackRecords = locTrackRecordService.selectLocTrackRecordList(params);
        if (locTrackRecords != null && !locTrackRecords.isEmpty()) {
            for (LocTrackRecord locTrackRecord : locTrackRecords) {
                LocationPoint point = toLocationPoint(locTrackRecord);
                if (point != null) {
                    locationPoints.add(point);
                }
            }
            return locationPoints;
        }
        return locationPoints;
    }

    private List<LocationPoint> getLocationAndUWBDataZq(String cardId, String buildId, String startTimeStr, String endTimeStr) {
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);

        // 获取位置点数据 - 添加空值检查
        String pointsResponse = zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr);
        if (pointsResponse == null) {
            throw new RuntimeException("获取位置点数据返回null");
        }
        JSONObject jsonObject = JSONObject.parseObject(pointsResponse);

        String tagResponse = zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId,
                DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)),
                DateTimeUtils.localDateTime2String(endTime.plusSeconds(2)));
        if (tagResponse == null) {
            throw new RuntimeException("获取标签状态数据返回null");
        }
        JSONObject tagJsonObject = JSONObject.parseObject(tagResponse);

        JSONArray points = jsonObject.getJSONArray("data");
        JSONArray tagData = tagJsonObject.getJSONArray("data");
        List<LocationPoint> LocationPoints = new ArrayList<>();

        // 基础空值保护：任意一侧为 null 直接返回空列表
        if (points == null || tagData == null){
            return LocationPoints;
        }

        // tag 数据为空：保持原有行为，直接返回空列表（不返回纯 GPS）
        if (tagData.isEmpty()) {
            return LocationPoints;
        }

        // points 完全为空，但 tag 有数据：以 tag 补齐虚拟点（经纬度填 0,0）
        if (points.isEmpty() && !tagData.isEmpty()) {
            List<TagScanUwbData> tagList = tagData.toJavaList(TagScanUwbData.class);
            // 对 tagList 中所有 BltScanUwbBeacon 的 distance 做单位转换（与 FusionData 保持一致，/1000）
            for (TagScanUwbData tag : tagList) {
                if (tag.getUwbBeaconList() != null) {
                    for (TagScanUwbData.BltScanUwbBeacon beacon : tag.getUwbBeaconList()) {
                        if (beacon.getDistance() != null) {
                            beacon.setDistance(beacon.getDistance() / 1000.0);
                        }
                    }
                }
            }
            for (TagScanUwbData tag : tagList) {
                LocationPoint lp = new LocationPoint(
                        cardId,
                        0.0,
                        0.0,
                        DateTimeUtils.timestampToDateTimeStr(tag.getTime()),
                        tag.getTime()
                );
                // 写入第三方速度（与 FusionData 保持一致）
                if (tag.getGnssInfo() != null && tag.getGnssInfo().getSpeedKmh() != null) {
                    lp.setThirdSpeed(tag.getGnssInfo().getSpeedKmh() / 3.6);
                } else {
                    lp.setThirdSpeed(0.0);
                }
                lp.setTagScanUwbData(tag);
                LocationPoints.add(lp);
            }
            LocationPoints.sort(Comparator.comparing(
                    LocationPoint::getTimestamp,
                    Comparator.nullsLast(Long::compareTo)
            ));
            return LocationPoints;
        }

        // 正常情况：以 points 为主，先解析所有定位点
        for (int i = 0; i < points.size(); i++){
            JSONObject js = (JSONObject) points.get(i);
            JSONArray plist = js.getJSONArray("points");
            if (plist != null) {
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
        }

        // 融合位置数据和标签数据（保持原有行为）
        if (!LocationPoints.isEmpty()) {
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints, tagData);
        }

        // 在已有融合结果基础上，再用 tag 数据补齐“points 中间缺口”的虚拟点（经纬度 0,0，cardUUID=cardId）
        if (!LocationPoints.isEmpty() && tagData != null && !tagData.isEmpty()) {
            // 使用与 FusionData 相同的时间窗口（毫秒）
            final long TIME_WINDOW_MS = 1000L;

            List<TagScanUwbData> tagList = tagData.toJavaList(TagScanUwbData.class);
            // 与 FusionData 一致，对 distance 再做一次单位转换
            for (TagScanUwbData tag : tagList) {
                if (tag.getUwbBeaconList() != null) {
                    for (TagScanUwbData.BltScanUwbBeacon beacon : tag.getUwbBeaconList()) {
                        if (beacon.getDistance() != null) {
                            beacon.setDistance(beacon.getDistance() / 1000.0);
                        }
                    }
                }
            }

            for (TagScanUwbData tag : tagList) {
                boolean existsNearbyPoint = false;
                for (LocationPoint loc : LocationPoints) {
                    Long ts = loc.getTimestamp();
                    if (ts == null) {
                        continue;
                    }
                    long diff = Math.abs(tag.getTime() - ts);
                    if (diff <= TIME_WINDOW_MS) {
                        existsNearbyPoint = true;
                        break;
                    }
                }
                // 已有 1 秒内的真实点，则不再补虚拟点
                if (existsNearbyPoint) {
                    continue;
                }

                // points 在这一秒完全缺失，但 tag 有数据：补一个 0,0 的虚拟点
                LocationPoint lp = new LocationPoint(
                        cardId,
                        0.0,
                        0.0,
                        DateTimeUtils.timestampToDateTimeStr(tag.getTime()),
                        tag.getTime()
                );
                if (tag.getGnssInfo() != null && tag.getGnssInfo().getSpeedKmh() != null) {
                    lp.setThirdSpeed(tag.getGnssInfo().getSpeedKmh() / 3.6);
                } else {
                    lp.setThirdSpeed(0.0);
                }
                lp.setTagScanUwbData(tag);
                LocationPoints.add(lp);
            }

            // 重新按时间排序，保证整体时序连续
            LocationPoints.sort(Comparator.comparing(
                    LocationPoint::getTimestamp,
                    Comparator.nullsLast(Long::compareTo)
            ));
        }

        for (LocationPoint point : LocationPoints){
            // 空值保护：检查 tagScanUwbData 和 uwbBeaconList
            if (point.getTagScanUwbData() == null || point.getTagScanUwbData().getUwbBeaconList() == null) {
                continue;
            }
            
            Map<String, Double> deviceReport = new HashMap<>();
            for (TagScanUwbData.BltScanUwbBeacon beacon : point.getTagScanUwbData().getUwbBeaconList()){
                deviceReport.put(beacon.getUwbBeaconMac(), beacon.getDistance());
            }
            try {
                if (deviceReport.size() > 3){
                    BeaconRepair beaconRepair = new BeaconRepair();
                    deviceReport = beaconRepair.repairBeaconDistance(deviceReport, 2.0);
                    if (deviceReport != null && !deviceReport.isEmpty()){
                        List<TagScanUwbData.BltScanUwbBeacon> uwbBeaconList = point.getTagScanUwbData().getUwbBeaconList();

                        // 记录改动
                        List<String> addedBeacons = new ArrayList<>();
                        List<String> removedBeacons = new ArrayList<>();
                        List<String> modifiedBeacons = new ArrayList<>();

                        // 同步 deviceReport 到 uwbBeaconList
                        Map<String, TagScanUwbData.BltScanUwbBeacon> existingBeaconMap = new HashMap<>();
                        for (TagScanUwbData.BltScanUwbBeacon beacon : uwbBeaconList) {
                            if (beacon.getUwbBeaconMac() != null) {
                                existingBeaconMap.put(beacon.getUwbBeaconMac(), beacon);
                            }
                        }

                        // 更新或新增信标
                        for (Map.Entry<String, Double> entry : deviceReport.entrySet()) {
                            String beaconId = entry.getKey();
                            Double newDistance = entry.getValue();

                            if (beaconId == null || newDistance == null) {
                                continue;
                            }

                            TagScanUwbData.BltScanUwbBeacon existingBeacon = existingBeaconMap.get(beaconId);
                            if (existingBeacon != null) {
                                // 已存在，检查距离是否不同
                                Double oldDistance = existingBeacon.getDistance();
                                if (oldDistance == null || !oldDistance.equals(newDistance)) {
                                    existingBeacon.setDistance(newDistance);
                                    modifiedBeacons.add(String.format("%s: %.3f -> %.3f", beaconId,
                                            oldDistance != null ? oldDistance : 0.0, newDistance));
                                }
                            } else {
                                // 不存在，新增
                                TagScanUwbData.BltScanUwbBeacon newBeacon = new TagScanUwbData.BltScanUwbBeacon();
                                newBeacon.setUwbBeaconMac(beaconId);
                                newBeacon.setDistance(newDistance);
                                uwbBeaconList.add(newBeacon);
                                addedBeacons.add(String.format("%s: %.3f", beaconId, newDistance));
                            }
                        }

                        // 移除不存在的信标
                        Set<String> deviceReportIds = deviceReport.keySet();
                        Iterator<TagScanUwbData.BltScanUwbBeacon> iterator = uwbBeaconList.iterator();
                        while (iterator.hasNext()) {
                            TagScanUwbData.BltScanUwbBeacon beacon = iterator.next();
                            String beaconId = beacon.getUwbBeaconMac();
                            if (beaconId != null && !deviceReportIds.contains(beaconId)) {
                                Double oldDistance = beacon.getDistance();
                                iterator.remove();
                                removedBeacons.add(String.format("%s: %.3f", beaconId,
                                        oldDistance != null ? oldDistance : 0.0));
                            }
                        }

                        // 输出改动日志
                        if (!addedBeacons.isEmpty() || !removedBeacons.isEmpty() || !modifiedBeacons.isEmpty()) {
                            StringBuilder log = new StringBuilder();
                            if (point.getAcceptTime() != null) {
                                log.append("[").append(point.getAcceptTime()).append("] ");
                            }
                            log.append("信标同步日志");

                            if (!addedBeacons.isEmpty()) {
                                log.append(" - 新增: ").append(String.join(", ", addedBeacons));
                            }
                            if (!modifiedBeacons.isEmpty()) {
                                log.append(" - 修改: ").append(String.join(", ", modifiedBeacons));
                            }
                            if (!removedBeacons.isEmpty()) {
                                log.append(" - 移除: ").append(String.join(", ", removedBeacons));
                            }

//                            System.out.println(log.toString());
                        }
                    }
                }
            } catch (Exception e){
//                System.out.println(point.getAcceptTime() + "信标距离计算处理异常: " + e.getMessage() );
            }
            // 计算位置经纬度
//            LocationSolver locationSolver = new LocationSolver();
//            DriverLocation results = locationSolver.calculateUserLocation(deviceReport);
        }

        return LocationPoints;
    }

    /**
     * 将 LocTrackRecord 映射为 LocationPoint（一条记录 -> 一个点）
     * 保护性处理：坐标或时间不可用时返回 null
     */
    private LocationPoint toLocationPoint(LocTrackRecord r) {
        if (r == null) {
            return null;
        }
        // 基本字段
        String cardUUID = r.getCardId();
        String latStr = r.getLatitude();
        String lonStr = r.getLongitude();
        String acceptTime = r.getCreatedAt();

        // 坐标解析
        Double lat = null;
        Double lon = null;
        try {
            if (latStr != null && !latStr.isEmpty()) {
                lat = Double.parseDouble(latStr);
            }
            if (lonStr != null && !lonStr.isEmpty()) {
                lon = Double.parseDouble(lonStr);
            }
        } catch (Exception ignore) {
            return null; // 坐标非法，丢弃该点
        }
        if (lat == null || lon == null) {
            return null; // 缺少必要坐标
        }

        // 时间戳解析（允许为空；为空则只填 acceptTime）
        Long ts = null;
        if (acceptTime != null && !acceptTime.isEmpty()) {
            try {
                ts = DateTimeUtils.convertToTimestamp(acceptTime);
            } catch (Exception ignore) {
                // 保留 acceptTime，timestamp 为空
            }
        }

        LocationPoint point = new LocationPoint();
        point.setCardUUID(cardUUID);
        point.setLatitude(lat);
        point.setLongitude(lon);
        point.setAcceptTime(acceptTime);
        point.setTimestamp(ts);

        // 速度（如果有且可解析）
        try {
            if (r.getSpeed() != null && !r.getSpeed().isEmpty()) {
                point.setSpeed(Double.parseDouble(r.getSpeed()));
            }
        } catch (Exception ignore) {
            // 跳过不可解析速度
        }

        // 解析 UWB 基站数组 JSON -> TagScanUwbData.uwbBeaconList
        String uwbsJson = r.getUwbsJson();
        if (uwbsJson != null && !uwbsJson.isEmpty()) {
            try {
                JSONArray arr = JSONArray.parseArray(uwbsJson);
                if (arr != null && !arr.isEmpty()) {
                    TagScanUwbData tagScanUwbData = new TagScanUwbData();
                    List<TagScanUwbData.BltScanUwbBeacon> uwbBeaconList = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        if (obj == null) {
                            continue;
                        }
                        String uwbId = obj.getString("uwbId");
                        Double distance = null;
                        try {
                            // distance 可能为数字或字符串，统一转 Double
                            Object distObj = obj.get("distance");
                            if (distObj instanceof Number) {
                                distance = ((Number) distObj).doubleValue();
                            } else if (distObj instanceof String && !((String) distObj).isEmpty()) {
                                distance = Double.parseDouble((String) distObj);
                            }
                        } catch (Exception ignore) {
                            // 单条 distance 解析失败，跳过该项
                        }
                        if (uwbId == null || uwbId.isEmpty() || distance == null) {
                            continue;
                        }
                        TagScanUwbData.BltScanUwbBeacon beacon = new TagScanUwbData.BltScanUwbBeacon();
                        beacon.setUwbBeaconMac(uwbId);
                        beacon.setDistance(distance);
                        uwbBeaconList.add(beacon);
                    }
                    if (!uwbBeaconList.isEmpty()) {
                        tagScanUwbData.setUwbBeaconList(uwbBeaconList);
                        point.setTagScanUwbData(tagScanUwbData);
                    }
                }
            } catch (Exception ignore) {
                // 非法 JSON，忽略 UWB 映射，不影响其它字段
            }
        }

        return point;
    }
}
