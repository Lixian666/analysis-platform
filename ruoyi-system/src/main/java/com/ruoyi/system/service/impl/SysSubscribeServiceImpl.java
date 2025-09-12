package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysSubscribeMapper;
import com.ruoyi.system.domain.SysSubscribe;
import com.ruoyi.system.service.ISysSubscribeService;

/**
 * 订阅管理Service业务层处理
 * 
 * @author lx
 * @date 2025-09-09
 */
@Service
public class SysSubscribeServiceImpl implements ISysSubscribeService 
{
    @Autowired
    private SysSubscribeMapper sysSubscribeMapper;

    /**
     * 查询订阅管理
     * 
     * @param ID 订阅管理主键
     * @return 订阅管理
     */
    @Override
    public SysSubscribe selectSysSubscribeByID(String ID)
    {
        return sysSubscribeMapper.selectSysSubscribeByID(ID);
    }

    /**
     * 查询订阅管理列表
     * 
     * @param sysSubscribe 订阅管理
     * @return 订阅管理
     */
    @Override
    public List<SysSubscribe> selectSysSubscribeList(SysSubscribe sysSubscribe)
    {
        return sysSubscribeMapper.selectSysSubscribeList(sysSubscribe);
    }

    /**
     * 新增订阅管理
     * 
     * @param sysSubscribe 订阅管理
     * @return 结果
     */
    @Override
    public int insertSysSubscribe(SysSubscribe sysSubscribe)
    {
        sysSubscribe.setCreateTime(DateUtils.getNowDate());
        return sysSubscribeMapper.insertSysSubscribe(sysSubscribe);
    }

    /**
     * 修改订阅管理
     * 
     * @param sysSubscribe 订阅管理
     * @return 结果
     */
    @Override
    public int updateSysSubscribe(SysSubscribe sysSubscribe)
    {
        sysSubscribe.setUpdateTime(DateUtils.getNowDate());
        return sysSubscribeMapper.updateSysSubscribe(sysSubscribe);
    }

    /**
     * 批量删除订阅管理
     * 
     * @param IDs 需要删除的订阅管理主键
     * @return 结果
     */
    @Override
    public int deleteSysSubscribeByIDs(String[] IDs)
    {
        return sysSubscribeMapper.deleteSysSubscribeByIDs(IDs);
    }

    /**
     * 删除订阅管理信息
     * 
     * @param ID 订阅管理主键
     * @return 结果
     */
    @Override
    public int deleteSysSubscribeByID(String ID)
    {
        return sysSubscribeMapper.deleteSysSubscribeByID(ID);
    }

//    /**
//     * 发布事件给所有订阅者
//     */
//    public void publish(String type, String message) {
//        List<SysSubscribe> subs = sysSubscribeMapper.selectByType(type);
//        for (SysSubscribe sub : subs) {
//            if ("ACTIVE".equals(sub.getStatus())) {
//                if ("HTTP".equalsIgnoreCase(sub.getMode())) {
//                    sendHttp(sub.getEndpoint(), message);
//                } else if ("MQ".equalsIgnoreCase(sub.getMode())) {
//                    mqProducer.send(sub.getEndpoint(), message);
//                }
//            }
//        }
//    }
//
//    private void sendHttp(String url, String message) {
//        try {
//            SubReceiveData data = new SubReceiveData();
//            List<String> buildIds = new ArrayList<>();
//            buildIds.add(buildingId);
//            data.setBuildIds(buildIds);
//            data.setServerUrl(domain + ":" + port + "/subscribe/callback/zqTagScanUwbBeacon");
//            SubscribeResult result = ZQOpenApi.httpSubscriber(type,data);
//        } catch (Exception e) {
//            System.err.println("HTTP 回调失败: " + e.getMessage());
//        }
//    }
}
