package com.jwzt.modules.experiment.service;

import com.jwzt.modules.experiment.domain.TakRfidRecord;
import java.util.List;

/**
 * RFID识别记录Service接口
 * 
 * @author lx
 * @date 2025-01-20
 */
public interface ITakRfidRecordService {
    /**
     * 查询RFID识别记录
     * 
     * @param id RFID识别记录主键
     * @return RFID识别记录
     */
    public TakRfidRecord selectTakRfidRecordById(Long id);

    /**
     * 查询RFID识别记录列表
     * 
     * @param takRfidRecord RFID识别记录
     * @return RFID识别记录集合
     */
    public List<TakRfidRecord> selectTakRfidRecordList(TakRfidRecord takRfidRecord);

    /**
     * 新增RFID识别记录
     * 
     * @param takRfidRecord RFID识别记录
     * @return 结果
     */
    public int insertTakRfidRecord(TakRfidRecord takRfidRecord);

    /**
     * 批量新增RFID识别记录
     * 
     * @param takRfidRecordList RFID识别记录列表
     * @return 结果
     */
    public int batchInsertTakRfidRecord(List<TakRfidRecord> takRfidRecordList);

    /**
     * 修改RFID识别记录
     * 
     * @param takRfidRecord RFID识别记录
     * @return 结果
     */
    public int updateTakRfidRecord(TakRfidRecord takRfidRecord);

    /**
     * 批量更新RFID识别记录的匹配状态
     * 
     * @param records RFID识别记录列表
     * @return 结果
     */
    public int batchUpdateMatchStatus(List<TakRfidRecord> records);

    /**
     * 删除RFID识别记录
     * 
     * @param id RFID识别记录主键
     * @return 结果
     */
    public int deleteTakRfidRecordById(Long id);

    /**
     * 批量删除RFID识别记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTakRfidRecordByIds(Long[] ids);

    /**
     * 根据时间范围和匹配状态查询RFID记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param matchStatus 匹配状态（可选，null表示查询所有状态）
     * @return RFID识别记录集合
     */
    public List<TakRfidRecord> selectTakRfidRecordByTimeRange(String startTime, String endTime, Integer matchStatus);
}

