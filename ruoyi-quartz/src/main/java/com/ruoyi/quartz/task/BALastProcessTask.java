package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.domain.vo.DataMatchResult;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.utils.DataMatchUtils;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component("BALastProcessTask")
public class BALastProcessTask {

    private static final Logger log = LoggerFactory.getLogger(BALastProcessTask.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            TakBehaviorRecords tr = new TakBehaviorRecords();
            tr.setYardId(yardName);
            tr.setType(0L);
            tr.setState("完成");
            tr.setCardId(cardId);
            tr.setQueryTimeType(0);
            tr.setQueryStartTime(startTimeStr);
            tr.setQueryEndTime(endTimeStr);
            List<TakBehaviorRecords> takBehaviorRecords = takBehaviorRecordsService.selectTakBehaviorRecordsList(tr);
            log.info("获取到作业数据 {} 条", takBehaviorRecords != null ? takBehaviorRecords.size() : 0);
            
            // 获取rfid数据
            log.info("开始获取RFID数据");
            String rfidStartTime = startTimeStr + " 000";
            String rfidEndTime = endTimeStr + " 000";
            List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(tenantId, rfidStartTime, rfidEndTime);
            log.info("获取到RFID数据 {} 条", reqVehicleCodes != null ? reqVehicleCodes.size() : 0);
            
            // 匹配数据
            Integer timeInterval = getTimeIntervalSeconds();
            log.info("开始匹配数据，时间间隔配置：{}秒（null表示自动分析）", timeInterval);
            DataMatchResult matchResult = DataMatchUtils.matchData(takBehaviorRecords, reqVehicleCodes, timeInterval);
            
            // 输出匹配结果统计
            log.info("匹配完成，统计结果：");
            log.info("  - 匹配成功：{} 对", matchResult.getMatchedPairs().size());
            log.info("  - 作业数据多余：{} 条", matchResult.getExcessJobData().size());
            log.info("  - RFID数据多余：{} 条", matchResult.getExcessRfidData().size());
            log.info("  - RFID数据重复（被替换）：{} 条", matchResult.getDuplicateRfidData().size());
            
            // 处理匹配结果
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
        // 1. 处理匹配成功的数据（可能需要更新某些字段）
        processMatchedPairs(matchResult.getMatchedPairs());
        
        // 2. 处理作业数据多的情况（这些作业数据没有对应的RFID，可能需要删除或标记）
        processExcessJobData(matchResult.getExcessJobData());
        
        // 3. 处理RFID数据重复的情况（这些RFID在匹配过程中被替换，不计入多余队列）
        processDuplicateRfidData(matchResult.getDuplicateRfidData());
        
        // 4. 处理RFID数据多的情况（这些RFID没有对应的作业数据，可能需要新增作业记录）
        processExcessRfidData(matchResult.getExcessRfidData());
    }
    
    /**
     * 处理匹配成功的数据对
     */
    private void processMatchedPairs(List<DataMatchResult.MatchedPair> matchedPairs) {
        if (matchedPairs == null || matchedPairs.isEmpty()) {
            return;
        }
        
        log.info("处理匹配成功的数据对，共 {} 对", matchedPairs.size());
        
        for (DataMatchResult.MatchedPair pair : matchedPairs) {
            TakBehaviorRecords jobData = pair.getJobData();
            ReqVehicleCode rfidData = pair.getRfidData();
            
            // 可以根据业务需求，更新作业数据的某些字段
            // 例如：使用RFID的时间或其他信息更新作业数据
            try {
                @SuppressWarnings("unused")
                Date rfidDate = parseRfidTime(rfidData.getVehicleTime());
                
//                 如果需要，可以在这里更新jobData的某些字段
//                 jobData.setStartTime(rfidDate); /·/ 示例：使用RFID时间更新作业开始时间
                
                // 更新作业数据
                // takBehaviorRecordsService.updateTakBehaviorRecords(jobData);
                
                log.debug("匹配对 - 作业ID：{}, 作业时间: {}, RFID时间：{}", jobData.getId(), jobData.getStartTime(), rfidData.getVehicleTime());
            } catch (Exception e) {
                log.warn("处理匹配对失败，作业ID：{}, 作业时间: {}, RFID时间：{}", jobData.getId(), jobData.getStartTime(), rfidData.getVehicleTime(), e);
            }
        }
    }
    
    /**
     * 处理多余的作业数据（没有匹配到RFID的作业数据）
     */
    private void processExcessJobData(List<TakBehaviorRecords> excessJobData) {
        if (excessJobData == null || excessJobData.isEmpty()) {
            return;
        }
        
        log.info("处理多余的作业数据，共 {} 条", excessJobData.size());
        
        for (TakBehaviorRecords jobData : excessJobData) {
            try {
                // 根据业务需求，可以选择删除或标记这些数据
                // 选项1：直接删除
                // takBehaviorRecordsService.deleteTakBehaviorRecordsById(jobData.getId());
                
                // 选项2：标记为无效或待处理
                // jobData.setState("待处理");
                // takBehaviorRecordsService.updateTakBehaviorRecords(jobData);
                
                log.debug("多余的作业数据 - ID：{}, 开始时间：{}", jobData.getId(), 
                    jobData.getStartTime() != null ? DATE_FORMAT.format(jobData.getStartTime()) : "未知");
            } catch (Exception e) {
                log.warn("处理多余作业数据失败，ID：{}", jobData.getId(), e);
            }
        }
    }
    
    /**
     * 处理重复的RFID数据（在匹配过程中被替换的RFID数据，不计入多余队列）
     */
    private void processDuplicateRfidData(List<ReqVehicleCode> duplicateRfidData) {
        if (duplicateRfidData == null || duplicateRfidData.isEmpty()) {
            return;
        }
        
        log.info("处理重复的RFID数据（被替换），共 {} 条", duplicateRfidData.size());
        
        for (ReqVehicleCode rfidData : duplicateRfidData) {
            try {
                // 这些RFID数据在匹配过程中被替换（同一时间范围内有多个RFID，只有最优的被匹配）
                // 根据业务需求，可以选择忽略或记录这些重复数据
                log.debug("重复的RFID数据（被替换）- RFID时间：{}, RFID：{}", 
                    rfidData.getVehicleTime(), rfidData.getRfid());
            } catch (Exception e) {
                log.warn("处理重复RFID数据失败，RFID时间：{}", rfidData.getVehicleTime(), e);
            }
        }
    }
    
    /**
     * 处理多余的RFID数据（没有匹配到作业数据的RFID）
     */
    private void processExcessRfidData(List<ReqVehicleCode> excessRfidData) {
        if (excessRfidData == null || excessRfidData.isEmpty()) {
            return;
        }
        
        log.info("处理多余的RFID数据，共 {} 条", excessRfidData.size());
        
        for (ReqVehicleCode rfidData : excessRfidData) {
            try {
                // 根据RFID数据创建新的作业记录
                @SuppressWarnings("unused")
                TakBehaviorRecords newJobData = createJobDataFromRfid(rfidData);
                
                // 新增作业记录
                // takBehaviorRecordsService.insertTakBehaviorRecords(newJobData);
                
                log.debug("根据RFID新增作业数据 - RFID时间：{}, RFID：{}", 
                    rfidData.getVehicleTime(), rfidData.getRfid());
            } catch (Exception e) {
                log.warn("根据RFID创建作业数据失败，RFID时间：{}", rfidData.getVehicleTime(), e);
            }
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
