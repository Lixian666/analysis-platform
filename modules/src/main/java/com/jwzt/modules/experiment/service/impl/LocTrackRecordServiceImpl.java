package com.jwzt.modules.experiment.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jwzt.modules.experiment.mapper.LocTrackRecordMapper;
import com.jwzt.modules.experiment.domain.LocTrackRecord;
import com.jwzt.modules.experiment.service.ILocTrackRecordService;

/**
 * 定位数据记录Service业务层处理
 * 
 * @author lx
 * @date 2025-11-13
 */
@Service
public class LocTrackRecordServiceImpl implements ILocTrackRecordService 
{
    @Autowired
    private LocTrackRecordMapper locTrackRecordMapper;

    /**
     * 查询定位数据记录
     * 
     * @param id 定位数据记录主键
     * @return 定位数据记录
     */
    @Override
    public LocTrackRecord selectLocTrackRecordById(String id)
    {
        return locTrackRecordMapper.selectLocTrackRecordById(id);
    }

    /**
     * 查询定位数据记录列表
     * 
     * @param locTrackRecord 定位数据记录
     * @return 定位数据记录
     */
    @Override
    public List<LocTrackRecord> selectLocTrackRecordList(LocTrackRecord locTrackRecord)
    {
        return locTrackRecordMapper.selectLocTrackRecordList(locTrackRecord);
    }

    /**
     * 新增定位数据记录
     * 
     * @param locTrackRecord 定位数据记录
     * @return 结果
     */
    @Override
    public int insertLocTrackRecord(LocTrackRecord locTrackRecord)
    {
        return locTrackRecordMapper.insertLocTrackRecord(locTrackRecord);
    }

    /**
     * 修改定位数据记录
     * 
     * @param locTrackRecord 定位数据记录
     * @return 结果
     */
    @Override
    public int updateLocTrackRecord(LocTrackRecord locTrackRecord)
    {
        return locTrackRecordMapper.updateLocTrackRecord(locTrackRecord);
    }

    /**
     * 批量删除定位数据记录
     * 
     * @param ids 需要删除的定位数据记录主键
     * @return 结果
     */
    @Override
    public int deleteLocTrackRecordByIds(String[] ids)
    {
        return locTrackRecordMapper.deleteLocTrackRecordByIds(ids);
    }

    /**
     * 删除定位数据记录信息
     * 
     * @param id 定位数据记录主键
     * @return 结果
     */
    @Override
    public int deleteLocTrackRecordById(String id)
    {
        return locTrackRecordMapper.deleteLocTrackRecordById(id);
    }
}
