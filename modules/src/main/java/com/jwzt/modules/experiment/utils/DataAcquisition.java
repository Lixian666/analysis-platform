package com.jwzt.modules.experiment.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.LocTrackRecord;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.service.ILocTrackRecordService;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        LocTrackRecord params = new LocTrackRecord();
        params.setCardId(cardId);
        params.setQueryStartTime(startTimeStr);
        params.setQueryEndTime(endTimeStr);
        List<LocTrackRecord> locTrackRecords = locTrackRecordService.selectLocTrackRecordList(params);
        if (locTrackRecords != null && !locTrackRecords.isEmpty()) {
            List<LocationPoint> locationPoints = new ArrayList<>();
            for (LocTrackRecord locTrackRecord : locTrackRecords) {
                LocationPoint point = toLocationPoint(locTrackRecord);
                if (point != null) {
                    locationPoints.add(point);
                }
            }
            return locationPoints;
        }
        return null;
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

        if (points.size() == 0 || tagData.size() == 0){
            return null;
        };
        List<LocationPoint> LocationPoints = new ArrayList<>();
        if (points != null && !points.isEmpty()) {
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
        }

        // 融合位置数据和标签数据
        if (!LocationPoints.isEmpty()) {
            LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints, tagData);
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
