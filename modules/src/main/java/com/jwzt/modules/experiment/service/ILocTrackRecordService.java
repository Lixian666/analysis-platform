package com.jwzt.modules.experiment.service;

import java.util.List;
import com.jwzt.modules.experiment.domain.LocTrackRecord;

/**
 * 定位数据记录Service接口
 * 
 * @author lx
 * @date 2025-11-13
 */
public interface ILocTrackRecordService 
{
    /**
     * 查询定位数据记录
     * 
     * @param id 定位数据记录主键
     * @return 定位数据记录
     */
    public LocTrackRecord selectLocTrackRecordById(String id);

    /**
     * 查询定位数据记录列表
     * 
     * @param locTrackRecord 定位数据记录
     * @return 定位数据记录集合
     */
    public List<LocTrackRecord> selectLocTrackRecordList(LocTrackRecord locTrackRecord);

    /**
     * 新增定位数据记录
     * 
     * @param locTrackRecord 定位数据记录
     * @return 结果
     */
    public int insertLocTrackRecord(LocTrackRecord locTrackRecord);

    /**
     * 修改定位数据记录
     * 
     * @param locTrackRecord 定位数据记录
     * @return 结果
     */
    public int updateLocTrackRecord(LocTrackRecord locTrackRecord);

    /**
     * 批量删除定位数据记录
     * 
     * @param ids 需要删除的定位数据记录主键集合
     * @return 结果
     */
    public int deleteLocTrackRecordByIds(String[] ids);

    /**
     * 删除定位数据记录信息
     * 
     * @param id 定位数据记录主键
     * @return 结果
     */
    public int deleteLocTrackRecordById(String id);
}
