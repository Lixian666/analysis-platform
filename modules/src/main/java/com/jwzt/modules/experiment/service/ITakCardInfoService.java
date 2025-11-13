package com.jwzt.modules.experiment.service;

import com.jwzt.modules.experiment.domain.TakCardInfo;

import java.util.List;

/**
 * 定位卡信息Service接口
 *
 * @author lx
 * @date 2025-11-13
 */
public interface ITakCardInfoService {

    /**
     * 查询定位卡信息
     *
     * @param id 主键
     * @return 定位卡信息
     */
    TakCardInfo selectTakCardInfoById(Long id);

    /**
     * 查询定位卡信息列表
     *
     * @param takCardInfo 查询条件
     * @return 定位卡集合
     */
    List<TakCardInfo> selectTakCardInfoList(TakCardInfo takCardInfo);

    /**
     * 新增定位卡信息
     *
     * @param takCardInfo 实体
     * @return 结果
     */
    int insertTakCardInfo(TakCardInfo takCardInfo);

    /**
     * 修改定位卡信息
     *
     * @param takCardInfo 实体
     * @return 结果
     */
    int updateTakCardInfo(TakCardInfo takCardInfo);

    /**
     * 修改启用状态
     *
     * @param takCardInfo 实体（仅包含id和enabled）
     * @return 结果
     */
    int changeTakCardInfoStatus(TakCardInfo takCardInfo);

    /**
     * 批量删除定位卡信息
     *
     * @param ids 主键集合
     * @return 结果
     */
    int deleteTakCardInfoByIds(Long[] ids);

    /**
     * 删除定位卡信息
     *
     * @param id 主键
     * @return 结果
     */
    int deleteTakCardInfoById(Long id);
}

