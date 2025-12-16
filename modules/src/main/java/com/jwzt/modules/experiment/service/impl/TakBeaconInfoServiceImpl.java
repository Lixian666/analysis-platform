package com.jwzt.modules.experiment.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
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
     * 修改信标启用状态
     * 
     * @param takBeaconInfo 信标信息（仅包含id和status）
     * @return 结果
     */
    @Override
    public int changeTakBeaconInfoStatus(TakBeaconInfo takBeaconInfo)
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
    
    /**
     * 导入信标信息数据
     * 
     * @param beaconInfoList 信标信息数据列表
     * @param updateSupport 是否更新支持，如果已存在，是否更新
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importTakBeaconInfo(List<TakBeaconInfo> beaconInfoList, boolean updateSupport, String operName)
    {
        if (StringUtils.isNull(beaconInfoList) || beaconInfoList.size() == 0)
        {
            throw new RuntimeException("导入信标信息数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (TakBeaconInfo beaconInfo : beaconInfoList)
        {
            try
            {
                // 验证是否存在这条记录
                TakBeaconInfo existBeaconInfo = new TakBeaconInfo();
                existBeaconInfo.setBeaconId(beaconInfo.getBeaconId());
                List<TakBeaconInfo> existBeaconInfoList = takBeaconInfoMapper.selectTakBeaconInfoList(existBeaconInfo);
                
                if (StringUtils.isNull(existBeaconInfoList) || existBeaconInfoList.size() == 0)
                {
                    beaconInfo.setCreateBy(operName);
                    beaconInfo.setCreateTime(DateUtils.getNowDate());
                    this.insertTakBeaconInfo(beaconInfo);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、信标ID " + beaconInfo.getBeaconId() + " 导入成功");
                }
                else if (updateSupport)
                {
                    beaconInfo.setUpdateBy(operName);
                    beaconInfo.setUpdateTime(DateUtils.getNowDate());
                    beaconInfo.setId(existBeaconInfoList.get(0).getId());
                    this.updateTakBeaconInfo(beaconInfo);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、信标ID " + beaconInfo.getBeaconId() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、信标ID " + beaconInfo.getBeaconId() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、信标ID " + beaconInfo.getBeaconId() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new RuntimeException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
