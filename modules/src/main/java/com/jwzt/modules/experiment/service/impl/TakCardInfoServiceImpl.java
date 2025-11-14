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

    @Override
    public String importTakCardInfo(List<TakCardInfo> cardInfoList, boolean updateSupport, String operName) {
        if (cardInfoList == null || cardInfoList.isEmpty()) {
            throw new RuntimeException("导入定位卡数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (TakCardInfo card : cardInfoList) {
            try {
                // 按 cardId 判断是否存在
                TakCardInfo query = new TakCardInfo();
                query.setCardId(card.getCardId());
                List<TakCardInfo> exist = takCardInfoMapper.selectTakCardInfoList(query);
                if (exist == null || exist.isEmpty()) {
                    card.setCreateBy(operName);
                    card.setCreateTime(com.ruoyi.common.utils.DateUtils.getNowDate());
                    insertTakCardInfo(card);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、卡ID ").append(card.getCardId()).append(" 导入成功");
                } else if (updateSupport) {
                    TakCardInfo existOne = exist.get(0);
                    card.setId(existOne.getId());
                    card.setUpdateBy(operName);
                    card.setUpdateTime(com.ruoyi.common.utils.DateUtils.getNowDate());
                    updateTakCardInfo(card);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、卡ID ").append(card.getCardId()).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、卡ID ").append(card.getCardId()).append(" 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、卡ID " + card.getCardId() + " 导入失败：";
                failureMsg.append(msg).append(e.getMessage());
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new RuntimeException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    public List<String> selectTakCardIdList(TakCardInfo takCardInfo) {
        return takCardInfoMapper.selectTakCardIdList(takCardInfo);
    }
}

