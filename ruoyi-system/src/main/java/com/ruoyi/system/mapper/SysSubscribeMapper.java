package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysSubscribe;

/**
 * 订阅管理Mapper接口
 * 
 * @author lx
 * @date 2025-09-09
 */
public interface SysSubscribeMapper 
{
    /**
     * 查询订阅管理
     * 
     * @param ID 订阅管理主键
     * @return 订阅管理
     */
    public SysSubscribe selectSysSubscribeByID(String ID);

    /**
     * 查询订阅管理列表
     * 
     * @param sysSubscribe 订阅管理
     * @return 订阅管理集合
     */
    public List<SysSubscribe> selectSysSubscribeList(SysSubscribe sysSubscribe);

    /**
     * 新增订阅管理
     * 
     * @param sysSubscribe 订阅管理
     * @return 结果
     */
    public int insertSysSubscribe(SysSubscribe sysSubscribe);

    /**
     * 修改订阅管理
     * 
     * @param sysSubscribe 订阅管理
     * @return 结果
     */
    public int updateSysSubscribe(SysSubscribe sysSubscribe);

    /**
     * 删除订阅管理
     * 
     * @param ID 订阅管理主键
     * @return 结果
     */
    public int deleteSysSubscribeByID(String ID);

    /**
     * 批量删除订阅管理
     * 
     * @param IDs 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysSubscribeByIDs(String[] IDs);
}
