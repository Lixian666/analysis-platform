package com.jwzt.modules.experiment.mapper;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.vo.TakBehaviorRecordDetailVo;

/**
 * 行为记录详情Mapper接口
 * 
 * @author lx
 * @date 2025-07-09
 */
public interface TakBehaviorRecordDetailMapper 
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

    int insertTakBehaviorRecordDetailAll(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList);

    /**
     * 修改行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    public int updateTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail);

    /**
     * 删除行为记录详情
     * 
     * @param id 行为记录详情主键
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailById(Long id);

    /**
     * 批量删除行为记录详情
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTakBehaviorRecordDetailByIds(Long[] ids);

    public void deleteByCreationTime(String create_time);

    List<TakBehaviorRecordDetailVo> selectTakBehaviorRecordDetailListByUserId(TakBehaviorRecordDetail takBehaviorRecordDetail);
}
