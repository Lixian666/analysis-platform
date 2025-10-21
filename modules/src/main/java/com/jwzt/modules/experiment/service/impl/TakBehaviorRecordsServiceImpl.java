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
        // 删除旧的子表数据（根据trackId）
        if (StringUtils.isNotEmpty(takBehaviorRecords.getTrackId())) {
            takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackId(takBehaviorRecords.getTrackId());
        }
        // 插入新的子表数据
        insertTakBehaviorRecordDetail(takBehaviorRecords);
        return takBehaviorRecordsMapper.updateTakBehaviorRecords(takBehaviorRecords);
    }

    /**
     * 批量删除行为记录（同时删除关联的子数据）
     * 
     * @param ids 需要删除的行为记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTakBehaviorRecordsByIds(Long[] ids)
    {
        // 先根据主键ID查询出对应的trackId列表
        List<String> trackIdList = new ArrayList<>();
        for (Long id : ids) {
            TakBehaviorRecords record = takBehaviorRecordsMapper.selectTakBehaviorRecordsById(id);
            if (record != null && StringUtils.isNotEmpty(record.getTrackId())) {
                trackIdList.add(record.getTrackId());
            }
        }
        // 删除子表数据（根据trackId）
        if (!trackIdList.isEmpty()) {
            String[] trackIds = trackIdList.toArray(new String[0]);
            takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackIds(trackIds);
        }
        // 删除主表数据
        return takBehaviorRecordsMapper.deleteTakBehaviorRecordsByIds(ids);
    }

    /**
     * 删除行为记录信息（同时删除关联的子数据）
     * 
     * @param id 行为记录主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTakBehaviorRecordsById(Long id)
    {
        // 先根据主键ID查询出对应的trackId
        TakBehaviorRecords record = takBehaviorRecordsMapper.selectTakBehaviorRecordsById(id);
        if (record != null && StringUtils.isNotEmpty(record.getTrackId())) {
            // 删除子表数据（根据trackId）
            takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackId(record.getTrackId());
        }
        // 删除主表数据
        return takBehaviorRecordsMapper.deleteTakBehaviorRecordsById(id);
    }

    /**
     * 根据条件删除行为记录（用于聚合查询结果的删除）
     * 删除指定卡号、货场、任务日期的所有行为记录及其详情
     * 
     * @param takBehaviorRecords 包含cardId、yardId、startTime、endTime的查询条件
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteTakBehaviorRecordsByCondition(TakBehaviorRecords takBehaviorRecords)
    {
        // 先查询出符合条件的所有记录
        List<TakBehaviorRecords> recordsToDelete = takBehaviorRecordsMapper.selectTakBehaviorRecordsByCondition(takBehaviorRecords);
        
        if (recordsToDelete == null || recordsToDelete.isEmpty()) {
            return 0;
        }
        
        // 收集所有的trackId
        List<String> trackIdList = new ArrayList<>();
        List<Long> idList = new ArrayList<>();
        for (TakBehaviorRecords record : recordsToDelete) {
            if (record.getId() != null) {
                idList.add(record.getId());
            }
            if (StringUtils.isNotEmpty(record.getTrackId())) {
                trackIdList.add(record.getTrackId());
            }
        }
        
        // 删除子表数据（根据trackId）
        if (!trackIdList.isEmpty()) {
            String[] trackIds = trackIdList.toArray(new String[0]);
            takBehaviorRecordsMapper.deleteTakBehaviorRecordDetailByTrackIds(trackIds);
        }
        
        // 删除主表数据
        if (!idList.isEmpty()) {
            Long[] ids = idList.toArray(new Long[0]);
            return takBehaviorRecordsMapper.deleteTakBehaviorRecordsByIds(ids);
        }
        
        return 0;
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
