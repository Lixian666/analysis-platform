package com.jwzt.modules.experiment.domain.vo;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据匹配结果
 * 
 * @author lx
 * @date 2025-01-20
 */
@Data
public class DataMatchResult {
    
    /**
     * 匹配成功的数据对队列
     */
    private List<MatchedPair> matchedPairs = new ArrayList<>();
    
    /**
     * 作业数据多的队列（作业数据没有匹配到RFID）
     */
    private List<TakBehaviorRecords> excessJobData = new ArrayList<>();
    
    /**
     * RFID数据多的队列（RFID数据没有匹配到作业数据）
     */
    private List<ReqVehicleCode> excessRfidData = new ArrayList<>();
    
    /**
     * RFID数据重复队列（RFID数据在匹配过程中被替换，即在自动分析时被用于匹配但后续被更优的RFID替换）
     */
    private List<ReqVehicleCode> duplicateRfidData = new ArrayList<>();
    
    /**
     * 匹配对，包含作业数据和RFID数据
     */
    @Data
    public static class MatchedPair {
        private TakBehaviorRecords jobData;
        private ReqVehicleCode rfidData;
        
        public MatchedPair(TakBehaviorRecords jobData, ReqVehicleCode rfidData) {
            this.jobData = jobData;
            this.rfidData = rfidData;
        }
    }
}

