package com.jwzt.modules.experiment.mapper;

import java.util.List;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;

/**
 * 信标信息Mapper接口
 * 
 * @author lx
 * @date 2025-09-16
 */
public interface TakBeaconInfoMapper 
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
     * 删除信标信息
     * 
     * @param id 信标信息主键
     * @return 结果
     */
    public int deleteTakBeaconInfoById(String id);

    /**
     * 批量删除信标信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTakBeaconInfoByIds(String[] ids);
}
