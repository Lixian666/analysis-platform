package com.jwzt.modules.experiment.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.mapper.TakBehaviorRecordsMapper;
import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;

/**
 * 行为记录Service业务层处理
 * 
 * @author lx
 * @date 2025-07-09
 */
@Service
public class TakBehaviorRecordsServiceImpl implements ITakBehaviorRecordsService 
{
    @Autowired
    private TakBehaviorRecordsMapper takBehaviorRecordsMapper;

    /**
     * 查询行为记录
     * 
     * @param id 行为记录主键
     * @return 行为记录
     */
    @Override
    public TakBehaviorRecords selectTakBehaviorRecordsById(Long id)
    {
        return takBehaviorRecordsMapper.selectTakBehaviorRecordsById(id);
    }

    /**
     * 查询行为记录列表
     * 
     * @param takBehaviorRecords 行为记录
     * @return 行为记录
     */
    @Override
    public List<TakBehaviorRecords> selectTakBehaviorRecordsList(TakBehaviorRecords takBehaviorRecords)
    {
        return takBehaviorRecordsMapper.selectTakBehaviorRecordsList(takBehaviorRecords);
    }

    /**
     * 新增行为记录
     * 
     * @param takBehaviorRecords 行为记录
     * @return 结果
     */
    @Transactional
    @Override
    public int insertTakBehaviorRecords(TakBehaviorRecords takBehaviorRecords)
    {
        takBehaviorRecords.setCreateTime(DateUtils.getNowDate());
        takBehaviorRecords.setId(IdUtils.snowflakeId());
        int rows = takBehaviorRecordsMapper.insertTakBehaviorRecords(takBehaviorRecords);
//        insertTakBehaviorRecordDetail(takBehaviorRecords);
        return rows;
    }

    /**
     * 修改行为记录
     * 
     * @param takBehaviorRecords 行为记录
     * @return 结果
     */
    @Transactional
    @Override
    public int updateTakBehaviorRecords(TakBehaviorRecords takBehaviorRecords)
    {
        takBehaviorRecords.setUpdateTime(DateUtils.getNowDate());
        takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackId(takBehaviorRecords.getId());
        insertTakBehaviorRecordDetail(takBehaviorRecords);
        return takBehaviorRecordsMapper.updateTakBehaviorRecords(takBehaviorRecords);
    }

    /**
     * 批量删除行为记录
     * 
     * @param ids 需要删除的行为记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTakBehaviorRecordsByIds(Long[] ids)
    {
        takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackIds(ids);
        return takBehaviorRecordsMapper.deleteTakBehaviorRecordsByIds(ids);
    }

    /**
     * 删除行为记录信息
     * 
     * @param id 行为记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTakBehaviorRecordsById(Long id)
    {
        takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackId(id);
        return takBehaviorRecordsMapper.deleteTakBehaviorRecordsById(id);
    }

    public void deleteByCreationTime(String s){
        takBehaviorRecordsMapper.deleteByCreationTime(s);
    }

    public List<TakBehaviorRecords> selectTakBehaviorRecordsUserList(TakBehaviorRecords takBehaviorRecords){
        return takBehaviorRecordsMapper.selectTakBehaviorRecordsUserList(takBehaviorRecords);
    }

    /**
     * 新增行为记录详情信息
     * 
     * @param takBehaviorRecords 行为记录对象
     */
    public void insertTakBehaviorRecordDetail(TakBehaviorRecords takBehaviorRecords)
    {
        List<TakBehaviorRecordDetail> takBehaviorRecordDetailList = takBehaviorRecords.getTakBehaviorRecordDetailList();
//        long id = IdUtils.SnowflakeId();
        String trackId = takBehaviorRecords.getTrackId();
        if (StringUtils.isNotNull(takBehaviorRecordDetailList))
        {
            List<TakBehaviorRecordDetail> list = new ArrayList<TakBehaviorRecordDetail>();
            for (TakBehaviorRecordDetail takBehaviorRecordDetail : takBehaviorRecordDetailList)
            {

//                takBehaviorRecordDetail.setId(id);
                takBehaviorRecordDetail.setTrackId(trackId);
                list.add(takBehaviorRecordDetail);
            }
            if (list.size() > 0)
            {
                takBehaviorRecordsMapper.batchTakBehaviorRecordDetail(list);
            }
        }
    }
}
