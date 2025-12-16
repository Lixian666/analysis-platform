package com.jwzt.modules.experiment.service;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;

/**
 * 信标信息Service接口
 * 
 * @author lx
 * @date 2025-09-16
 */
public interface ITakBeaconInfoService 
{
    /**
     * 查询信标信息
     * 
     * @param id 信标信息主键
     * @return 信标信息
     */
    public TakBeaconInfo selectTakBeaconInfoById(String id);

    /**
     * 查询信标信息列表
     * 
     * @param takBeaconInfo 信标信息
     * @return 信标信息集合
     */
    public List<TakBeaconInfo> selectTakBeaconInfoList(TakBeaconInfo takBeaconInfo);

    /**
     * 新增信标信息
     * 
     * @param takBeaconInfo 信标信息
     * @return 结果
     */
    public int insertTakBeaconInfo(TakBeaconInfo takBeaconInfo);

    /**
     * 修改信标信息
     * 
     * @param takBeaconInfo 信标信息
     * @return 结果
     */
    public int updateTakBeaconInfo(TakBeaconInfo takBeaconInfo);

    /**
     * 修改信标启用状态
     * 
     * @param takBeaconInfo 信标信息（仅包含id和status）
     * @return 结果
     */
    public int changeTakBeaconInfoStatus(TakBeaconInfo takBeaconInfo);

    /**
     * 批量删除信标信息
     * 
     * @param ids 需要删除的信标信息主键集合
     * @return 结果
     */
    public int deleteTakBeaconInfoByIds(String[] ids);

    /**
     * 删除信标信息信息
     * 
     * @param id 信标信息主键
     * @return 结果
     */
    public int deleteTakBeaconInfoById(String id);
    
    /**
     * 导入信标信息数据
     * 
     * @param beaconInfoList 信标信息数据列表
     * @param updateSupport 是否更新支持，如果已存在，是否更新
     * @param operName 操作用户
     * @return 结果
     */
    public String importTakBeaconInfo(List<TakBeaconInfo> beaconInfoList, boolean updateSupport, String operName);
}
