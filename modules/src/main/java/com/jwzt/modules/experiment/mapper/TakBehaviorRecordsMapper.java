package com.jwzt.modules.experiment.mapper;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;

/**
 * 行为记录Mapper接口
 * 
 * @author lx
 * @date 2025-07-09
 */
public interface TakBehaviorRecordsMapper 
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
     * 删除行为记录
     * 
     * @param id 行为记录主键
     * @return 结果
     */
    public int deleteTakBehaviorRecordsById(Long id);

    /**
     * 批量删除行为记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordsByIds(Long[] ids);

    /**
     * 批量删除行为记录详情（根据trackId）
     * 
     * @param trackIds 需要删除的轨迹编号集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByTrackIds(String[] trackIds);
    
    /**
     * 批量新增行为记录详情
     * 
     * @param takBehaviorRecordDetailList 行为记录详情列表
     * @return 结果
     */
    public int batchTakBehaviorRecordDetail(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList);
    

    /**
     * 通过轨迹编号删除行为记录详情信息
     * 
     * @param trackId 轨迹编号
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByTrackId(String trackId);

    public void deleteByCreationTime(String createTime);

    List<TakBehaviorRecords> selectTakBehaviorRecordsUserList(TakBehaviorRecords takBehaviorRecords);

    /**
     * 根据条件查询行为记录（用于按条件删除前的查询）
     * 
     * @param takBehaviorRecords 查询条件（cardId、yardId、startTime、endTime）
     * @return 行为记录集合
     */
    List<TakBehaviorRecords> selectTakBehaviorRecordsByCondition(TakBehaviorRecords takBehaviorRecords);

    /**
     * 批量更新行为记录的匹配状态
     * 
     * @param records 行为记录列表
     * @return 结果
     */
    int batchUpdateMatchStatus(List<TakBehaviorRecords> records);

    void updatePushStatus(TakBehaviorRecords record);
}
