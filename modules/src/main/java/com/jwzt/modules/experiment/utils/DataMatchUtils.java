package com.jwzt.modules.experiment.utils;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.domain.vo.DataMatchResult;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据匹配工具类
 * 用于匹配作业数据和RFID数据
 * 
 * @author lx
 * @date 2025-01-20
 */
public class DataMatchUtils {
    
    private static final SimpleDateFormat VEHICLE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat VEHICLE_TIME_FORMAT_COLON = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    static {
        // 尝试解析带毫秒的格式（可能有冒号分隔）
        try {
            VEHICLE_TIME_FORMAT.parse("2025-01-01 12:00:00.000");
        } catch (ParseException e) {
            // ignore
        }
    }
    
    /**
     * 匹配作业数据和RFID数据
     * 
     * @param jobDataList 作业数据列表
     * @param rfidDataList RFID数据列表
     * @param timeIntervalSeconds 时间间隔（秒），如果为null则自动分析
     * @return 匹配结果
     */
    public static DataMatchResult matchData(
            List<TakBehaviorRecords> jobDataList,
            List<ReqVehicleCode> rfidDataList,
            Integer timeIntervalSeconds) {
        
        DataMatchResult result = new DataMatchResult();
        
        if (CollectionUtils.isEmpty(jobDataList) || CollectionUtils.isEmpty(rfidDataList)) {
            if (!CollectionUtils.isEmpty(jobDataList)) {
                result.getExcessJobData().addAll(jobDataList);
            }
            if (!CollectionUtils.isEmpty(rfidDataList)) {
                result.getExcessRfidData().addAll(rfidDataList);
            }
            return result;
        }
        
        // 如果时间间隔为null，自动分析
        if (timeIntervalSeconds == null) {
            timeIntervalSeconds = analyzeTimeInterval(jobDataList, rfidDataList);
        }
        
        // 将时间转换为时间戳进行匹配
        List<JobDataWithTimestamp> jobDataWithTimestamps = jobDataList.stream()
                .filter(data -> data.getIdentifyTime() != null)
                .map(JobDataWithTimestamp::new)
                .sorted(Comparator.comparingLong(JobDataWithTimestamp::getTimestamp))
                .collect(Collectors.toList());
        
        List<RfidDataWithTimestamp> rfidDataWithTimestamps = rfidDataList.stream()
                .filter(data -> data.getVehicleTime() != null && !data.getVehicleTime().isEmpty())
                .map(RfidDataWithTimestamp::new)
                .filter(data -> data.getTimestamp() != null)
                .sorted(Comparator.comparingLong(RfidDataWithTimestamp::getTimestamp))
                .collect(Collectors.toList());
        
        // 执行匹配
        matchDataWithTimestamps(jobDataWithTimestamps, rfidDataWithTimestamps, timeIntervalSeconds, result);
        
        return result;
    }
    
    /**
     * 自动分析时间间隔
     * 通过分析匹配成功的数据对的时间差来确定合适的时间间隔
     * 优化：利用RECORD_CODE相同特性，同一车辆的多条识别记录只选择时间最接近的
     */
    private static int analyzeTimeInterval(
            List<TakBehaviorRecords> jobDataList,
            List<ReqVehicleCode> rfidDataList) {
        
        if (CollectionUtils.isEmpty(jobDataList) || CollectionUtils.isEmpty(rfidDataList)) {
            // 默认返回5分钟
            return 300;
        }
        
        // 按RECORD_CODE分组RFID数据，每组选择最接近作业数据时间的作为代表
        Map<String, List<ReqVehicleCode>> rfidByRecordCode = rfidDataList.stream()
            .filter(rfid -> rfid.getRecordCode() != null && !rfid.getRecordCode().isEmpty())
            .collect(Collectors.groupingBy(ReqVehicleCode::getRecordCode));
        
        List<Long> timeDiffs = new ArrayList<>();
        
        for (TakBehaviorRecords jobData : jobDataList) {
            if (jobData.getStartTime() == null) {
                continue;
            }
            
            long jobTimestamp = jobData.getStartTime().getTime();
            ReqVehicleCode closestRfid = null;
            long minDiff = Long.MAX_VALUE;
            
            // 先遍历按RECORD_CODE分组的数据，每组选择时间最接近的
            for (List<ReqVehicleCode> sameCodeList : rfidByRecordCode.values()) {
                for (ReqVehicleCode rfid : sameCodeList) {
                    if (rfid.getVehicleTime() == null || rfid.getVehicleTime().isEmpty()) {
                        continue;
                    }
                    try {
                        long rfidTimestamp = parseVehicleTime(rfid.getVehicleTime());
                        long diff = Math.abs(rfidTimestamp - jobTimestamp);
                        if (diff < minDiff) {
                            minDiff = diff;
                            closestRfid = rfid;
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
            
            // 再遍历没有RECORD_CODE的RFID数据
            for (ReqVehicleCode rfid : rfidDataList) {
                if (rfid.getRecordCode() == null || rfid.getRecordCode().isEmpty()) {
                    if (rfid.getVehicleTime() == null || rfid.getVehicleTime().isEmpty()) {
                        continue;
                    }
                    try {
                        long rfidTimestamp = parseVehicleTime(rfid.getVehicleTime());
                        long diff = Math.abs(rfidTimestamp - jobTimestamp);
                        if (diff < minDiff) {
                            minDiff = diff;
                            closestRfid = rfid;
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
            
            if (closestRfid != null && closestRfid.getVehicleTime() != null) {
                try {
                    long rfidTimestamp = parseVehicleTime(closestRfid.getVehicleTime());
                    long diff = Math.abs(rfidTimestamp - jobTimestamp);
                    timeDiffs.add(diff);
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        if (timeDiffs.isEmpty()) {
            // 默认返回5分钟
            return 300;
        }
        
        // 计算中位数作为参考
        Collections.sort(timeDiffs);
        int medianIndex = timeDiffs.size() / 2;
        long medianDiff = timeDiffs.get(medianIndex);
        
        // 转换为秒，并加一些容差（加50%）
        int intervalSeconds = (int) (medianDiff / 1000);
        return Math.max(60, intervalSeconds * 3 / 2); // 至少1分钟，容差为1.5倍
    }
    
    /**
     * 使用时间戳进行匹配
     * 优化：利用RECORD_CODE相同特性，同一车辆的多条识别记录只选择时间最接近的作为代表
     */
    private static void matchDataWithTimestamps(
            List<JobDataWithTimestamp> jobDataList,
            List<RfidDataWithTimestamp> rfidDataList,
            int timeIntervalSeconds,
            DataMatchResult result) {
        
        boolean[] jobMatched = new boolean[jobDataList.size()];
        boolean[] rfidMatched = new boolean[rfidDataList.size()];
        // 用于记录RFID是否在匹配过程中被替换（在同一个时间范围内，有多个RFID候选，但只有最优的被匹配，其他被替换的计入重复队列）
        boolean[] rfidReplaced = new boolean[rfidDataList.size()];
        // 用于记录相同RECORD_CODE的RFID索引映射（RECORD_CODE -> List<索引>）
        Map<String, List<Integer>> recordCodeMap = new HashMap<>();
        
        // 构建RECORD_CODE索引映射
        for (int j = 0; j < rfidDataList.size(); j++) {
            ReqVehicleCode rfidData = rfidDataList.get(j).getRfidData();
            String recordCode = rfidData.getRecordCode();
            if (recordCode != null && !recordCode.isEmpty()) {
                recordCodeMap.computeIfAbsent(recordCode, k -> new ArrayList<>()).add(j);
            }
        }
        
        long intervalMillis = timeIntervalSeconds * 1000L;
        
        // 遍历作业数据，找到匹配的RFID数据
        for (int i = 0; i < jobDataList.size(); i++) {
            if (jobMatched[i]) {
                continue;
            }
            
            JobDataWithTimestamp jobData = jobDataList.get(i);
            long jobTimestamp = jobData.getTimestamp();
            String jobRfidName = jobData.getJobData().getRfidName();
            
            // 找到时间范围内所有可能的RFID数据
            List<RfidCandidate> candidates = new ArrayList<>();
            
            for (int j = 0; j < rfidDataList.size(); j++) {
                if (rfidMatched[j]) {
                    continue;
                }
                
                RfidDataWithTimestamp rfidData = rfidDataList.get(j);
                
                // 先检查rfidName与regionId是否相等
                String rfidRegionId = rfidData.getRfidData().getRegionId();
                // 如果两个值都不为空，则必须相等；如果两个值都为空，则允许匹配（保持向后兼容）
                boolean rfidNameMatch = (jobRfidName == null && rfidRegionId == null) || 
                                       (jobRfidName != null && rfidRegionId != null && jobRfidName.equals(rfidRegionId));
                
                if (!rfidNameMatch) {
                    continue; // 不匹配则跳过，不检查时间差
                }
                
                // 再检查时间差是否在范围内
                long rfidTimestamp = rfidData.getTimestamp();
                long diff = Math.abs(rfidTimestamp - jobTimestamp);
                
                if (diff <= intervalMillis) {
                    candidates.add(new RfidCandidate(j, rfidData, diff));
                }
            }
            
            if (!candidates.isEmpty()) {
                // 按RECORD_CODE分组候选RFID，对于相同RECORD_CODE的，只保留时间最接近的
                Map<String, RfidCandidate> bestByRecordCode = new HashMap<>();
                List<RfidCandidate> otherCandidates = new ArrayList<>();
                
                for (RfidCandidate candidate : candidates) {
                    String recordCode = candidate.rfidData.getRfidData().getRecordCode();
                    if (recordCode != null && !recordCode.isEmpty()) {
                        // 如果有RECORD_CODE，按RECORD_CODE分组，每组只保留最接近的
                        RfidCandidate existing = bestByRecordCode.get(recordCode);
                        if (existing == null || candidate.diff < existing.diff) {
                            if (existing != null) {
                                otherCandidates.add(existing);
                            }
                            bestByRecordCode.put(recordCode, candidate);
                        } else {
                            otherCandidates.add(candidate);
                        }
                    } else {
                        // 没有RECORD_CODE的，直接加入候选
                        bestByRecordCode.put("NO_RECORD_CODE_" + candidate.index, candidate);
                    }
                }
                
                // 从优化后的候选中选择时间最接近的
                List<RfidCandidate> optimizedCandidates = new ArrayList<>(bestByRecordCode.values());
                optimizedCandidates.sort(Comparator.comparingLong(c -> c.diff));
                RfidCandidate bestCandidate = optimizedCandidates.get(0);
                
                // 标记已匹配
                result.getMatchedPairs().add(
                    new DataMatchResult.MatchedPair(jobData.getJobData(), bestCandidate.rfidData.getRfidData()));
                jobMatched[i] = true;
                rfidMatched[bestCandidate.index] = true;
                
                // 处理其他候选：
                // 1. 相同RECORD_CODE的其他RFID数据（计入重复队列）
                // 2. 不同RECORD_CODE但在时间范围内的RFID数据（计入重复队列）
                String matchedRecordCode = bestCandidate.rfidData.getRfidData().getRecordCode();
                
                // 标记相同RECORD_CODE的其他RFID为重复
                if (matchedRecordCode != null && !matchedRecordCode.isEmpty()) {
                    List<Integer> sameRecordCodeIndices = recordCodeMap.get(matchedRecordCode);
                    if (sameRecordCodeIndices != null) {
                        for (Integer idx : sameRecordCodeIndices) {
                            if (idx != bestCandidate.index && !rfidMatched[idx]) {
                                rfidReplaced[idx] = true;
                            }
                        }
                    }
                }
                
                // 标记其他时间范围内的候选为重复
                for (int k = 1; k < optimizedCandidates.size(); k++) {
                    int replacedIndex = optimizedCandidates.get(k).index;
                    rfidReplaced[replacedIndex] = true;
                }
                
                // 标记没有选中的其他候选为重复
                for (RfidCandidate other : otherCandidates) {
                    rfidReplaced[other.index] = true;
                }
            }
        }
        
        // 收集未匹配的作业数据
        for (int i = 0; i < jobDataList.size(); i++) {
            if (!jobMatched[i]) {
                result.getExcessJobData().add(jobDataList.get(i).getJobData());
            }
        }
        
        // 收集未匹配的RFID数据
        for (int j = 0; j < rfidDataList.size(); j++) {
            if (!rfidMatched[j]) {
                // 如果RFID在匹配过程中被替换过，计入重复队列
                if (rfidReplaced[j]) {
                    result.getDuplicateRfidData().add(rfidDataList.get(j).getRfidData());
                } else {
                    // 否则计入多余队列
                    result.getExcessRfidData().add(rfidDataList.get(j).getRfidData());
                }
            }
        }
    }
    
    /**
     * RFID候选数据结构
     */
    private static class RfidCandidate {
        int index;
        RfidDataWithTimestamp rfidData;
        long diff;
        
        RfidCandidate(int index, RfidDataWithTimestamp rfidData, long diff) {
            this.index = index;
            this.rfidData = rfidData;
            this.diff = diff;
        }
    }
    
    /**
     * 解析RFID时间字符串为时间戳
     */
    private static long parseVehicleTime(String vehicleTime) throws ParseException {
        if (vehicleTime == null || vehicleTime.isEmpty()) {
            throw new IllegalArgumentException("车辆时间为空");
        }
        
        // 尝试替换最后一个冒号为点（如果格式是 yyyy-MM-dd HH:mm:ss:SSS）
        String normalizedTime = vehicleTime;
        int lastColonIndex = normalizedTime.lastIndexOf(':');
        if (lastColonIndex > 18) { // 确保是毫秒部分的冒号
            normalizedTime = normalizedTime.substring(0, lastColonIndex) + "." 
                    + normalizedTime.substring(lastColonIndex + 1);
        }
        
        try {
            return VEHICLE_TIME_FORMAT.parse(normalizedTime).getTime();
        } catch (ParseException e) {
            // 尝试不带毫秒的格式
            try {
                return VEHICLE_TIME_FORMAT_COLON.parse(normalizedTime).getTime();
            } catch (ParseException e2) {
                // 使用工具类的方法
                return DateTimeUtils.convertToTimestamp(normalizedTime);
            }
        }
    }
    
    /**
     * 带时间戳的作业数据包装类
     */
    private static class JobDataWithTimestamp {
        private TakBehaviorRecords jobData;
        private Long timestamp;
        
        public JobDataWithTimestamp(TakBehaviorRecords jobData) {
            this.jobData = jobData;
            this.timestamp = jobData.getIdentifyTime() != null ? jobData.getIdentifyTime().getTime() : null;
        }
        
        public TakBehaviorRecords getJobData() {
            return jobData;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * 带时间戳的RFID数据包装类
     */
    private static class RfidDataWithTimestamp {
        private ReqVehicleCode rfidData;
        private Long timestamp;
        
        public RfidDataWithTimestamp(ReqVehicleCode rfidData) {
            this.rfidData = rfidData;
            try {
                this.timestamp = parseVehicleTime(rfidData.getVehicleTime());
            } catch (Exception e) {
                this.timestamp = null;
            }
        }
        
        public ReqVehicleCode getRfidData() {
            return rfidData;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
    }
}

