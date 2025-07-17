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
     * 批量删除行为记录详情
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByTrackIds(Long[] ids);
    
    /**
     * 批量新增行为记录详情
     * 
     * @param takBehaviorRecordDetailList 行为记录详情列表
     * @return 结果
     */
    public int batchTakBehaviorRecordDetail(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList);
    

    /**
     * 通过行为记录主键删除行为记录详情信息
     * 
     * @param id 行为记录ID
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByTrackId(Long id);

    public void deleteByCreationTime(String createTime);

    List<TakBehaviorRecords> selectTakBehaviorRecordsUserList(TakBehaviorRecords takBehaviorRecords);
}
