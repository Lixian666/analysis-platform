package com.jwzt.modules.experiment.service;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBehaviorRecords;

/**
 * 行为记录Service接口
 * 
 * @author lx
 * @date 2025-07-09
 */
public interface ITakBehaviorRecordsService 
{
    /**
     * 查询行为记录
     * 
     * @param id 行为记录主键
     * @return 行为记录
     */
    public TakBehaviorRecords selectTakBehaviorRecordsById(Long id);

    /**
     * 查询行为记录列表
     * 
     * @param takBehaviorRecords 行为记录
     * @return 行为记录集合
     */
    public List<TakBehaviorRecords> selectTakBehaviorRecordsList(TakBehaviorRecords takBehaviorRecords);

    /**
     * 新增行为记录
     * 
     * @param takBehaviorRecords 行为记录
     * @return 结果
     */
    public int insertTakBehaviorRecords(TakBehaviorRecords takBehaviorRecords);

    /**
     * 修改行为记录
     * 
     * @param takBehaviorRecords 行为记录
     * @return 结果
     */
    public int updateTakBehaviorRecords(TakBehaviorRecords takBehaviorRecords);

    /**
     * 批量删除行为记录
     * 
     * @param ids 需要删除的行为记录主键集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordsByIds(Long[] ids);

    /**
     * 删除行为记录信息
     * 
     * @param id 行为记录主键
     * @return 结果
     */
    public int deleteTakBehaviorRecordsById(Long id);

    /**
     * 根据条件删除行为记录（用于聚合查询结果的删除）
     * 
     * @param takBehaviorRecords 包含cardId、yardId、startTime、endTime的查询条件
     * @return 结果
     */
    public int deleteTakBehaviorRecordsByCondition(TakBehaviorRecords takBehaviorRecords);

    public void deleteByCreationTime(String s);

    List<TakBehaviorRecords> selectTakBehaviorRecordsUserList(TakBehaviorRecords takBehaviorRecords);

    /**
     * 批量更新行为记录的匹配状态
     * 
     * @param records 行为记录列表
     * @return 结果
     */
    public int batchUpdateMatchStatus(List<TakBehaviorRecords> records);

    void updatePushStatus(String trackId, int pushStatus);
}
