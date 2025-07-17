package com.jwzt.modules.experiment.service.impl;

import java.util.List;

import com.jwzt.modules.experiment.vo.TakBehaviorRecordDetailVo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jwzt.modules.experiment.mapper.TakBehaviorRecordDetailMapper;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;

/**
 * 行为记录详情Service业务层处理
 * 
 * @author lx
 * @date 2025-07-09
 */
@Service
public class TakBehaviorRecordDetailServiceImpl implements ITakBehaviorRecordDetailService 
{
    @Autowired
    private TakBehaviorRecordDetailMapper takBehaviorRecordDetailMapper;

    /**
     * 查询行为记录详情
     * 
     * @param id 行为记录详情主键
     * @return 行为记录详情
     */
    @Override
    public TakBehaviorRecordDetail selectTakBehaviorRecordDetailById(Long id)
    {
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailById(id);
    }

    /**
     * 查询行为记录详情列表
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 行为记录详情
     */
    @Override
    public List<TakBehaviorRecordDetail> selectTakBehaviorRecordDetailList(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailList(takBehaviorRecordDetail);
    }

    /**
     * 新增行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    @Override
    public int insertTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        takBehaviorRecordDetail.setCreateTime(DateUtils.getNowDate());
        takBehaviorRecordDetail.setId(IdUtils.snowflakeId());
        return takBehaviorRecordDetailMapper.insertTakBehaviorRecordDetail(takBehaviorRecordDetail);
    }

    @Override
    public void insertTakBehaviorRecordDetailAll(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList){
        takBehaviorRecordDetailList.forEach(takBehaviorRecordDetail -> {
            takBehaviorRecordDetail.setCreateTime(DateUtils.getNowDate());
            takBehaviorRecordDetail.setId(IdUtils.snowflakeId());
        });
        takBehaviorRecordDetailMapper.insertTakBehaviorRecordDetailAll(takBehaviorRecordDetailList);
    }

    /**
     * 修改行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    @Override
    public int updateTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        takBehaviorRecordDetail.setUpdateTime(DateUtils.getNowDate());
        return takBehaviorRecordDetailMapper.updateTakBehaviorRecordDetail(takBehaviorRecordDetail);
    }

    /**
     * 批量删除行为记录详情
     * 
     * @param ids 需要删除的行为记录详情主键
     * @return 结果
     */
    @Override
    public int deleteTakBehaviorRecordDetailByIds(Long[] ids)
    {
        return takBehaviorRecordDetailMapper.deleteTakBehaviorRecordDetailByIds(ids);
    }

    /**
     * 删除行为记录详情信息
     * 
     * @param id 行为记录详情主键
     * @return 结果
     */
    @Override
    public int deleteTakBehaviorRecordDetailById(Long id)
    {
        return takBehaviorRecordDetailMapper.deleteTakBehaviorRecordDetailById(id);
    }

    public void deleteByCreationTime(String s){
        takBehaviorRecordDetailMapper.deleteByCreationTime(s);
    }

    public List<TakBehaviorRecordDetailVo> selectTakBehaviorRecordDetailListByUserId(TakBehaviorRecordDetail takBehaviorRecordDetail){
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailListByUserId(takBehaviorRecordDetail);
    }
}
