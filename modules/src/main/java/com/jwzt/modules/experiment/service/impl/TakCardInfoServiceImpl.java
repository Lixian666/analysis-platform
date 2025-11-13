package com.jwzt.modules.experiment.service.impl;

import com.jwzt.modules.experiment.domain.TakCardInfo;
import com.jwzt.modules.experiment.mapper.TakCardInfoMapper;
import com.jwzt.modules.experiment.service.ITakCardInfoService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定位卡信息Service业务层处理
 *
 * @author lx
 * @date 2025-11-13
 */
@Service
public class TakCardInfoServiceImpl implements ITakCardInfoService {

    @Autowired
    private TakCardInfoMapper takCardInfoMapper;

    @Override
    public TakCardInfo selectTakCardInfoById(Long id) {
        return takCardInfoMapper.selectTakCardInfoById(id);
    }

    @Override
    public List<TakCardInfo> selectTakCardInfoList(TakCardInfo takCardInfo) {
        return takCardInfoMapper.selectTakCardInfoList(takCardInfo);
    }

    @Override
    public int insertTakCardInfo(TakCardInfo takCardInfo) {
        takCardInfo.setCreateTime(DateUtils.getNowDate());
        takCardInfo.setUpdateTime(DateUtils.getNowDate());
        return takCardInfoMapper.insertTakCardInfo(takCardInfo);
    }

    @Override
    public int updateTakCardInfo(TakCardInfo takCardInfo) {
        takCardInfo.setUpdateTime(DateUtils.getNowDate());
        return takCardInfoMapper.updateTakCardInfo(takCardInfo);
    }

    @Override
    public int changeTakCardInfoStatus(TakCardInfo takCardInfo) {
        takCardInfo.setUpdateTime(DateUtils.getNowDate());
        return takCardInfoMapper.updateTakCardInfo(takCardInfo);
    }

    @Override
    public int deleteTakCardInfoByIds(Long[] ids) {
        return takCardInfoMapper.deleteTakCardInfoByIds(ids);
    }

    @Override
    public int deleteTakCardInfoById(Long id) {
        return takCardInfoMapper.deleteTakCardInfoById(id);
    }
}

