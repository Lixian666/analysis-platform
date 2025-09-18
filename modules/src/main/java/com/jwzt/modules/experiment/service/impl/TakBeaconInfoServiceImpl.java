package com.jwzt.modules.experiment.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jwzt.modules.experiment.mapper.TakBeaconInfoMapper;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;
import com.jwzt.modules.experiment.service.ITakBeaconInfoService;

/**
 * 信标信息Service业务层处理
 * 
 * @author lx
 * @date 2025-09-16
 */
@Service
public class TakBeaconInfoServiceImpl implements ITakBeaconInfoService 
{
    @Autowired
    private TakBeaconInfoMapper takBeaconInfoMapper;

    /**
     * 查询信标信息
     * 
     * @param id 信标信息主键
     * @return 信标信息
     */
    @Override
    public TakBeaconInfo selectTakBeaconInfoById(String id)
    {
        return takBeaconInfoMapper.selectTakBeaconInfoById(id);
    }

    /**
     * 查询信标信息列表
     * 
     * @param takBeaconInfo 信标信息
     * @return 信标信息
     */
    @Override
    public List<TakBeaconInfo> selectTakBeaconInfoList(TakBeaconInfo takBeaconInfo)
    {
        return takBeaconInfoMapper.selectTakBeaconInfoList(takBeaconInfo);
    }

    /**
     * 新增信标信息
     * 
     * @param takBeaconInfo 信标信息
     * @return 结果
     */
    @Override
    public int insertTakBeaconInfo(TakBeaconInfo takBeaconInfo)
    {
        takBeaconInfo.setCreateTime(DateUtils.getNowDate());
        return takBeaconInfoMapper.insertTakBeaconInfo(takBeaconInfo);
    }

    /**
     * 修改信标信息
     * 
     * @param takBeaconInfo 信标信息
     * @return 结果
     */
    @Override
    public int updateTakBeaconInfo(TakBeaconInfo takBeaconInfo)
    {
        takBeaconInfo.setUpdateTime(DateUtils.getNowDate());
        return takBeaconInfoMapper.updateTakBeaconInfo(takBeaconInfo);
    }

    /**
     * 批量删除信标信息
     * 
     * @param ids 需要删除的信标信息主键
     * @return 结果
     */
    @Override
    public int deleteTakBeaconInfoByIds(String[] ids)
    {
        return takBeaconInfoMapper.deleteTakBeaconInfoByIds(ids);
    }

    /**
     * 删除信标信息信息
     * 
     * @param id 信标信息主键
     * @return 结果
     */
    @Override
    public int deleteTakBeaconInfoById(String id)
    {
        return takBeaconInfoMapper.deleteTakBeaconInfoById(id);
    }
}
