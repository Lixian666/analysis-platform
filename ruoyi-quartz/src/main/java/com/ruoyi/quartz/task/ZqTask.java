package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocTrackRecord;
import com.jwzt.modules.experiment.service.ILocTrackRecordService;
import com.jwzt.modules.experiment.utils.DataAcquisition;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 真趣定时任务调度
 *
 * @author lixian
 */
@Component("zqTask")
public class ZqTask {
    @Autowired
    private ZQOpenApi zqOpenApi;

    private static final Logger log = LoggerFactory.getLogger(ZqTask.class);


    @Value("${server.servlet.domain-name}")
    private String domain;
    @Value("${server.port}")
    private String port;
    @Value("${experiment.base.joysuch.building-id}")
    private String buildingId;

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private ApplicationContext applicationContext;  // 用于获取 prototype bean

    @Autowired
    private ILocTrackRecordService locTrackRecordService;

    /**
     * 卡号和UWB数据持久化
     * @param startTimeStr 开始时间
     * @param endTimeStr 结束时间
     */
    public void ZQTagAndUWBPersistent(String startTimeStr, String endTimeStr)
    {
        DataAcquisition dataAcquisition = applicationContext.getBean(DataAcquisition.class);
        List<String> cards = dataAcquisition.getCardIdList(1);
        for (String cardId : cards){
            List<LocationPoint> allPoints1 = dataAcquisition.getLocationAndUWBData(cardId, buildingId, startTimeStr, endTimeStr);

            if (allPoints1 == null || allPoints1.isEmpty()) {
                log.debug("卡号 {} 在时间范围 [{}, {}] 内无数据", cardId, startTimeStr, endTimeStr);
                continue;
            }

            // 转换为 LocTrackRecord
            List<LocTrackRecord> records = convertToLocTrackRecord(allPoints1);
            if (records.isEmpty()) {
                log.debug("卡号 {} 转换后无有效记录", cardId);
                continue;
            }

            // 去重：查询已存在的记录
            Set<String> existingKeys = getExistingRecordKeys(cardId, records);

            // 过滤出未入库的记录
            List<LocTrackRecord> newRecords = records.stream()
                    .filter(record -> {
                        String key = buildRecordKey(record);
                        return !existingKeys.contains(key);
                    })
                    .collect(Collectors.toList());

            if (newRecords.isEmpty()) {
                log.debug("卡号 {} 所有记录已存在，跳过插入", cardId);
                continue;
            }

            // 批量插入
            int insertedCount = 0;
            for (LocTrackRecord record : newRecords) {
                try {
                    locTrackRecordService.insertLocTrackRecord(record);
                    insertedCount++;
                } catch (Exception e) {
                    log.error("插入定位记录失败: cardId={}, acceptTime={}, error={}",
                            record.getCardId(), record.getCreatedAt(), e.getMessage(), e);
                }
            }

            log.info("卡号 {} 处理完成: 总记录数={}, 已存在={}, 新插入={}",
                    cardId, records.size(), records.size() - newRecords.size(), insertedCount);
        }
    }

    /**
     * 将 LocationPoint 列表转换为 LocTrackRecord 列表
     */
    private List<LocTrackRecord> convertToLocTrackRecord(List<LocationPoint> points) {
        List<LocTrackRecord> records = new ArrayList<>();
        for (LocationPoint point : points) {
            if (point == null) {
                continue;
            }

            LocTrackRecord record = new LocTrackRecord();

            // 基本字段
            String cardId = point.getCardUUID();
            if (cardId == null && point.getCardId() != null) {
                cardId = String.valueOf(point.getCardId());
            }
            record.setCardId(cardId);
            record.setStationId("-1");
            record.setSourceCode("zq");

            // 坐标
            if (point.getLatitude() != null) {
                record.setLatitude(String.valueOf(point.getLatitude()));
            }
            if (point.getLongitude() != null) {
                record.setLongitude(String.valueOf(point.getLongitude()));
            }

            // 时间：acceptTime -> createdAt
            record.setCreatedAt(point.getAcceptTime());

            // 速度
            if (point.getSpeed() != null) {
                record.setSpeed(String.valueOf(point.getSpeed()));
            }

            // UWB 数据序列化
            if (point.getTagScanUwbData() != null && point.getTagScanUwbData().getUwbBeaconList() != null) {
                List<TagScanUwbData.BltScanUwbBeacon> uwbBeaconList = point.getTagScanUwbData().getUwbBeaconList();
                if (!uwbBeaconList.isEmpty()) {
                    List<JSONObject> uwbJsonList = new ArrayList<>();
                    for (TagScanUwbData.BltScanUwbBeacon beacon : uwbBeaconList) {
                        JSONObject uwbObj = new JSONObject();
                        uwbObj.put("uwbId", beacon.getUwbBeaconMac());
                        uwbObj.put("distance", beacon.getDistance());
                        uwbJsonList.add(uwbObj);
                    }
                    record.setUwbsJson(JSONArray.toJSONString(uwbJsonList));
                }
            }

            // GPS有效性：如果有坐标则认为有效
            if (point.getLatitude() != null && point.getLongitude() != null) {
                record.setGpsIsValid("1");
            } else {
                record.setGpsIsValid("0");
            }

            records.add(record);
        }
        return records;
    }

    /**
     * 获取已存在记录的键集合（用于去重）
     * 键格式：cardId:createdAt
     * 优化：使用时间范围查询，一次性获取该卡号在时间范围内的所有记录
     */
    private Set<String> getExistingRecordKeys(String cardId, List<LocTrackRecord> records) {
        Set<String> existingKeys = new HashSet<>();

        if (records == null || records.isEmpty()) {
            return existingKeys;
        }

        // 收集所有 acceptTime，找出最小和最大时间用于范围查询
        List<String> acceptTimes = records.stream()
                .map(LocTrackRecord::getCreatedAt)
                .filter(time -> time != null && !time.isEmpty())
                .sorted()
                .collect(Collectors.toList());

        if (acceptTimes.isEmpty()) {
            return existingKeys;
        }

        // 使用时间范围查询，一次性获取该卡号在时间范围内的所有记录
        String minTime = acceptTimes.get(0);
        String maxTime = acceptTimes.get(acceptTimes.size() - 1);

        LocTrackRecord query = new LocTrackRecord();
        query.setCardId(cardId);
        query.setQueryStartTime(minTime);
        query.setQueryEndTime(maxTime);

        List<LocTrackRecord> existing = locTrackRecordService.selectLocTrackRecordList(query);
        if (existing != null && !existing.isEmpty()) {
            for (LocTrackRecord exist : existing) {
                existingKeys.add(buildRecordKey(exist));
            }
        }

        return existingKeys;
    }

    /**
     * 构建记录的唯一键：cardId:createdAt
     */
    private String buildRecordKey(LocTrackRecord record) {
        String cardId = record.getCardId() != null ? record.getCardId() : "";
        String createdAt = record.getCreatedAt() != null ? record.getCreatedAt() : "";
        return cardId + ":" + createdAt;
    }

    public void ZQGetTagStateHistoryOfTagID()
    {
        String cardId = "1918B3000BA8";
        String startTimeStr = "2025-09-12 14:17:00";
        String endTimeStr = "2025-09-12 14:24:00";
        JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(null, cardId, startTimeStr, endTimeStr));
        JSONArray data = jsonObject.getJSONArray("data");
        List<TagScanUwbData> tagScanUwbDataList = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            TagScanUwbData tag = data.getObject(i, TagScanUwbData.class);
            tag.setDateTime(DateTimeUtils.timestampToDateTimeStr(tag.getTime()));
            tagScanUwbDataList.add(tag);
        }
        JSONArray newData = new JSONArray(tagScanUwbDataList);
        jsonObject.put("data", newData);
        System.out.println(jsonObject);
    }

    public void ZQHttpSubscriber(String type)
    {
        SubReceiveData data = new SubReceiveData();
        List<String> buildIds = new ArrayList<>();
        buildIds.add(buildingId);
        data.setBuildIds(buildIds);
        data.setServerUrl(domain + ":" + port + "/subscribe/callback/zqTagScanUwbBeacon");
        SubscribeResult result = zqOpenApi.httpSubscriber(type,data);
        log.debug("订阅结果：{}",result);
    }

    public void ZQHttpUnSubscriber()
    {
        SubscribeResult result = zqOpenApi.httpUnSubscriber("82");
        log.debug("订阅结果：{}",result);
    }

}
