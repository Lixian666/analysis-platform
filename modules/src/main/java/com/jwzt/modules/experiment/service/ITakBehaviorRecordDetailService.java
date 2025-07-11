package com.jwzt.modules.experiment.service;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;

/**
 * 行为记录详情Service接口
 * 
 * @author lx
 * @date 2025-07-09
 */
public interface ITakBehaviorRecordDetailService 
{
    /**
     * 查询行为记录详情
     * 
     * @param id 行为记录详情主键
     * @return 行为记录详情
     */
    public TakBehaviorRecordDetail selectTakBehaviorRecordDetailById(Long id);

    /**
     * 查询行为记录详情列表
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 行为记录详情集合
     */
    public List<TakBehaviorRecordDetail> selectTakBehaviorRecordDetailList(TakBehaviorRecordDetail takBehaviorRecordDetail);

    /**
     * 新增行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    public int insertTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail);

    /**
     * 修改行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    public int updateTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail);

    /**
     * 批量删除行为记录详情
     * 
     * @param ids 需要删除的行为记录详情主键集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByIds(Long[] ids);

    /**
     * 删除行为记录详情信息
     * 
     * @param id 行为记录详情主键
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailById(Long id);

    public void deleteByCreationTime(String s);
}
