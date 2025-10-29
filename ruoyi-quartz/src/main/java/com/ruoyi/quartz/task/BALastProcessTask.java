package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.domain.TakRfidRecord;
import com.jwzt.modules.experiment.domain.vo.DataMatchResult;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.service.ITakRfidRecordService;
import com.jwzt.modules.experiment.utils.DataMatchUtils;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import com.ruoyi.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component("BALastProcessTask")
public class BALastProcessTask {

    private static final Logger log = LoggerFactory.getLogger(BALastProcessTask.class);

    @Value("${experiment.base.yard-name}")
    private String yardName;
    @Value("${experiment.base.sw-center.tenant-id}")
    private Long tenantId;
    
    /**
     * 时间间隔配置（秒），如果为null则自动分析
     */
    @Value("${experiment.base.data-match.time-interval-seconds:}")
    private String timeIntervalSecondsStr;
    
    /**
     * 是否忽略已匹配的数据（true-忽略已匹配的数据，false-包含所有数据）
     */
    @Value("${experiment.base.data-match.ignore-matched:false}")
    private boolean ignoreMatched;
    
    /**
     * 是否更新匹配状态（true-更新状态，false-不更新）
     */
    @Value("${experiment.base.data-match.update-match-status:true}")
    private boolean updateMatchStatus;
    
    /**
     * 是否保存RFID数据到数据库（true-保存，false-不保存）
     */
    @Value("${experiment.base.data-match.save-rfid-data:true}")
    private boolean saveRfidData;
    
    /**
     * 获取时间间隔配置（秒），如果配置为空或null则返回null（表示自动分析）
     */
    private Integer getTimeIntervalSeconds() {
        if (StringUtils.hasText(timeIntervalSecondsStr)) {
            try {
                return Integer.parseInt(timeIntervalSecondsStr.trim());
            } catch (NumberFormatException e) {
                log.warn("时间间隔配置格式错误：{}，将使用自动分析", timeIntervalSecondsStr);
                return null;
            }
        }
        return null;
    }

    @Autowired
    private ITakBehaviorRecordsService takBehaviorRecordsService;

    @Autowired
    private ITakRfidRecordService takRfidRecordService;

    @Autowired
    private CenterWorkHttpUtils centerWorkHttpUtils;

    /**
     * 作业数据与rfid数据匹配
     */
    public void theJobDataMatchesTheRFIDData() {
        String cardId = "1918B3000561";
        String startTimeStr = "2025-10-16 18:25:00";
        String endTimeStr = "2025-10-16 19:50:00";
        try {
            // 获取作业数据
            log.info("开始获取作业数据，时间范围：{} - {}", startTimeStr, endTimeStr);
            log.info("配置信息 - 忽略已匹配：{}, 更新状态：{}, 保存RFID：{}", ignoreMatched, updateMatchStatus, saveRfidData);
            TakBehaviorRecords tr = new TakBehaviorRecords();
            tr.setYardId(yardName);
            tr.setType(0L);
            tr.setState("完成");
            tr.setCardId(cardId);
            tr.setQueryTimeType(0);
            tr.setQueryStartTime(startTimeStr);
            tr.setQueryEndTime(endTimeStr);
            // 如果开启忽略已匹配数据，则只查询未匹配的数据
            if (ignoreMatched) {
                tr.setMatchStatus(0); // 0-未匹配
            }
            List<TakBehaviorRecords> takBehaviorRecords = takBehaviorRecordsService.selectTakBehaviorRecordsList(tr);
            log.info("获取到作业数据 {} 条", takBehaviorRecords != null ? takBehaviorRecords.size() : 0);
            
            // 获取rfid数据
            log.info("开始获取RFID数据");
            String rfidStartTime = startTimeStr + " 000";
            String rfidEndTime = endTimeStr + " 000";
            List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(tenantId, rfidStartTime, rfidEndTime);
            log.info("获取到RFID数据 {} 条", reqVehicleCodes != null ? reqVehicleCodes.size() : 0);
            
            // 如果开启忽略已匹配，从数据库查询已保存的RFID数据并过滤
            List<ReqVehicleCode> rfidDataToMatch = reqVehicleCodes;
            if (ignoreMatched && saveRfidData) {
                List<TakRfidRecord> existingRfidRecords = takRfidRecordService.selectTakRfidRecordByTimeRange(
                    rfidStartTime, rfidEndTime, null); // 查询所有状态的RFID记录
                if (existingRfidRecords != null && !existingRfidRecords.isEmpty()) {
                    // 提取已匹配的RFID（匹配成功=1）和重复的RFID（重复数据=3）的thirdId集合
                    java.util.Set<String> excludedThirdIds = existingRfidRecords.stream()
                        .filter(r -> r.getMatchStatus() != null && (r.getMatchStatus() == 1 || r.getMatchStatus() == 3)) // 1-匹配成功，3-重复数据
                        .map(TakRfidRecord::getThirdId)
                        .filter(thirdId -> thirdId != null && !thirdId.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
                    
                    // 过滤掉已匹配和重复的RFID数据
                    if (!excludedThirdIds.isEmpty()) {
                        rfidDataToMatch = reqVehicleCodes.stream()
                            .filter(rfid -> !excludedThirdIds.contains(rfid.getThirdId()))
                            .collect(Collectors.toList());
                        log.info("过滤已匹配和重复的RFID数据后，剩余 {} 条", rfidDataToMatch.size());
                    }
                }
            }
            
            // 匹配数据
            Integer timeInterval = getTimeIntervalSeconds();
            log.info("开始匹配数据，时间间隔配置：{}秒（null表示自动分析）", timeInterval);
            DataMatchResult matchResult = DataMatchUtils.matchData(takBehaviorRecords, rfidDataToMatch, timeInterval);
            
            // 输出匹配结果统计
            log.info("匹配完成，统计结果：");
            log.info("  - 匹配成功：{} 对", matchResult.getMatchedPairs().size());
            log.info("  - 作业数据多余：{} 条", matchResult.getExcessJobData().size());
            log.info("  - RFID数据多余：{} 条", matchResult.getExcessRfidData().size());
            log.info("  - RFID数据重复（被替换）：{} 条", matchResult.getDuplicateRfidData().size());
            
            // 保存RFID数据到数据库（如果需要）
            if (saveRfidData && reqVehicleCodes != null && !reqVehicleCodes.isEmpty()) {
                saveRfidDataToDatabase(reqVehicleCodes, rfidStartTime, rfidEndTime);
            }
            
            // 处理匹配结果并更新状态（如果需要）
            processMatchResult(matchResult);
            
            log.info("数据匹配处理完成");
        } catch (Exception e) {
            log.error("数据匹配处理失败", e);
        }
    }
    
    /**
     * 处理匹配结果
     * 以RFID数据为标准进行数据的删除或新增
     */
    private void processMatchResult(DataMatchResult matchResult) {
        Date now = DateUtils.getNowDate();
        List<TakBehaviorRecords> jobRecordsToUpdate = new ArrayList<>();
        List<TakRfidRecord> rfidRecordsToUpdate = new ArrayList<>();
        
        // 1. 处理匹配成功的数据
        for (DataMatchResult.MatchedPair pair : matchResult.getMatchedPairs()) {
            if (updateMatchStatus) {
                TakBehaviorRecords jobData = pair.getJobData();
                jobData.setMatchStatus(1); // 1-匹配成功
                jobData.setMatchTime(now);
                if (saveRfidData && pair.getRfidData().getThirdId() != null) {
                    // 需要先查询RFID记录的ID
                    TakRfidRecord rfidQuery = new TakRfidRecord();
                    rfidQuery.setThirdId(pair.getRfidData().getThirdId());
                    List<TakRfidRecord> rfidRecords = takRfidRecordService.selectTakRfidRecordList(rfidQuery);
                    if (rfidRecords != null && !rfidRecords.isEmpty()) {
                        jobData.setMatchedRfidId(rfidRecords.get(0).getId());
                        // 更新RFID记录状态
                        TakRfidRecord rfidRecord = rfidRecords.get(0);
                        rfidRecord.setMatchStatus(1); // 1-匹配成功
                        rfidRecord.setMatchedJobId(jobData.getId());
                        rfidRecord.setMatchTime(now);
                        rfidRecordsToUpdate.add(rfidRecord);
                    }
                }
                jobRecordsToUpdate.add(jobData);
            }
        }
        
        // 2. 处理作业数据多的情况（标记为2-作业数据多余）
        for (TakBehaviorRecords jobData : matchResult.getExcessJobData()) {
            if (updateMatchStatus) {
                jobData.setMatchStatus(2); // 2-作业数据多余
                jobData.setMatchTime(now);
                jobRecordsToUpdate.add(jobData);
            }
        }
        
        // 3. 处理RFID数据重复的情况（标记为3-重复数据）
        if (saveRfidData && updateMatchStatus) {
            for (ReqVehicleCode rfidData : matchResult.getDuplicateRfidData()) {
                TakRfidRecord rfidQuery = new TakRfidRecord();
                rfidQuery.setThirdId(rfidData.getThirdId());
                List<TakRfidRecord> rfidRecords = takRfidRecordService.selectTakRfidRecordList(rfidQuery);
                if (rfidRecords != null && !rfidRecords.isEmpty()) {
                    TakRfidRecord rfidRecord = rfidRecords.get(0);
                    rfidRecord.setMatchStatus(3); // 3-重复数据
                    rfidRecord.setMatchTime(now);
                    rfidRecordsToUpdate.add(rfidRecord);
                }
            }
        }
        
        // 4. 处理RFID数据多的情况（标记为2-多余数据）
        if (saveRfidData && updateMatchStatus) {
            for (ReqVehicleCode rfidData : matchResult.getExcessRfidData()) {
                TakRfidRecord rfidQuery = new TakRfidRecord();
                rfidQuery.setThirdId(rfidData.getThirdId());
                List<TakRfidRecord> rfidRecords = takRfidRecordService.selectTakRfidRecordList(rfidQuery);
                if (rfidRecords != null && !rfidRecords.isEmpty()) {
                    TakRfidRecord rfidRecord = rfidRecords.get(0);
                    rfidRecord.setMatchStatus(2); // 2-多余数据
                    rfidRecord.setMatchTime(now);
                    rfidRecordsToUpdate.add(rfidRecord);
                }
            }
        }
        
        // 批量更新状态
        if (updateMatchStatus && !jobRecordsToUpdate.isEmpty()) {
            int updated = takBehaviorRecordsService.batchUpdateMatchStatus(jobRecordsToUpdate);
            log.info("已更新 {} 条作业数据的匹配状态", updated);
        }
        
        if (updateMatchStatus && saveRfidData && !rfidRecordsToUpdate.isEmpty()) {
            int updated = takRfidRecordService.batchUpdateMatchStatus(rfidRecordsToUpdate);
            log.info("已更新 {} 条RFID数据的匹配状态", updated);
        }
    }
    
    /**
     * 保存RFID数据到数据库
     */
    private void saveRfidDataToDatabase(List<ReqVehicleCode> rfidDataList, String startTime, String endTime) {
        if (rfidDataList == null || rfidDataList.isEmpty()) {
            return;
        }
        
        log.info("开始保存RFID数据到数据库，共 {} 条", rfidDataList.size());
        
        // 先查询已存在的RFID数据（根据thirdId）
        List<TakRfidRecord> existingRecords = takRfidRecordService.selectTakRfidRecordByTimeRange(startTime, endTime, null);
        java.util.Set<String> existingThirdIds = new java.util.HashSet<>();
        if (existingRecords != null && !existingRecords.isEmpty()) {
            existingThirdIds = existingRecords.stream()
                .map(TakRfidRecord::getThirdId)
                .filter(thirdId -> thirdId != null && !thirdId.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        }
        
        List<TakRfidRecord> recordsToSave = new ArrayList<>();
        for (ReqVehicleCode rfidData : rfidDataList) {
            // 如果已存在，跳过
            if (rfidData.getThirdId() != null && existingThirdIds.contains(rfidData.getThirdId())) {
                continue;
            }
            
            try {
                TakRfidRecord record = new TakRfidRecord();
                record.setRecordCode(rfidData.getRecordCode());
                record.setVehicleTime(parseRfidTime(rfidData.getVehicleTime()));
                record.setRegionId(rfidData.getRegionId());
                record.setDriver(rfidData.getDriver());
                record.setOperateStationId(rfidData.getOperateStationId());
                record.setOperateStation(rfidData.getOperateStation());
                record.setRfid(rfidData.getRfid());
                record.setThirdId(rfidData.getThirdId());
                record.setMatchStatus(0); // 0-未匹配
                recordsToSave.add(record);
            } catch (Exception e) {
                log.warn("转换RFID数据失败，RFID时间：{}, thirdId：{}", rfidData.getVehicleTime(), rfidData.getThirdId(), e);
            }
        }
        
        if (!recordsToSave.isEmpty()) {
            int saved = takRfidRecordService.batchInsertTakRfidRecord(recordsToSave);
            log.info("已保存 {} 条新的RFID数据到数据库", saved);
        } else {
            log.info("所有RFID数据已存在，无需保存");
        }
    }
    
    /**
     * 根据RFID数据创建作业数据
     */
    private TakBehaviorRecords createJobDataFromRfid(ReqVehicleCode rfidData) throws ParseException {
        TakBehaviorRecords jobData = new TakBehaviorRecords();
        
        // 设置基本信息
        jobData.setYardId(yardName);
        jobData.setCardId(rfidData.getRfid()); // 使用RFID作为cardId
        
        // 解析RFID时间
        Date rfidDate = parseRfidTime(rfidData.getVehicleTime());
        jobData.setStartTime(rfidDate);
        
        // 设置其他默认值
        jobData.setType(0L); // 根据实际业务调整
        jobData.setState("完成");
        
        return jobData;
    }
    
    /**
     * 解析RFID时间字符串
     */
    private Date parseRfidTime(String vehicleTime) throws ParseException {
        if (vehicleTime == null || vehicleTime.isEmpty()) {
            throw new IllegalArgumentException("车辆时间为空");
        }
        
        // 处理时间格式（可能是 yyyy-MM-dd HH:mm:ss:SSS 或 yyyy-MM-dd HH:mm:ss.SSS）
        String normalizedTime = vehicleTime;
        int lastColonIndex = normalizedTime.lastIndexOf(':');
        if (lastColonIndex > 18) {
            normalizedTime = normalizedTime.substring(0, lastColonIndex) + "." 
                    + normalizedTime.substring(lastColonIndex + 1);
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return sdf.parse(normalizedTime);
        } catch (ParseException e) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(normalizedTime);
        }
    }
}
