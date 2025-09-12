package com.ruoyi.system.controller;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysSubscribe;
import com.ruoyi.system.service.ISysSubscribeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 订阅管理Controller
 * 
 * @author lx
 * @date 2025-09-09
 */
@RestController
@RequestMapping("/system/subscribe")
public class SysSubscribeController extends BaseController
{
    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private ISysSubscribeService sysSubscribeService;

    @Value("${experiment.base.building-id}")
    private String buildingId;

    @Value("${server.servlet.domain-name}")
    private String domain;

    @Value("${server.port}")
    private String port;

    /**
     * 查询订阅管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysSubscribe sysSubscribe)
    {
        startPage();
        List<SysSubscribe> list = sysSubscribeService.selectSysSubscribeList(sysSubscribe);
        return getDataTable(list);
    }

    /**
     * 导出订阅管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:export')")
    @Log(title = "订阅管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysSubscribe sysSubscribe)
    {
        List<SysSubscribe> list = sysSubscribeService.selectSysSubscribeList(sysSubscribe);
        ExcelUtil<SysSubscribe> util = new ExcelUtil<SysSubscribe>(SysSubscribe.class);
        util.exportExcel(response, list, "订阅管理数据");
    }

    /**
     * 获取订阅管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:query')")
    @GetMapping(value = "/{ID}")
    public AjaxResult getInfo(@PathVariable("ID") String ID)
    {
        return success(sysSubscribeService.selectSysSubscribeByID(ID));
    }

    /**
     * 新增订阅管理
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:add')")
    @Log(title = "订阅管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysSubscribe sysSubscribe)
    {
        if (sysSubscribe.getMODE().equals("HTTP")){
            if (sysSubscribe.getSOURCE().equals("ZQ")){
                SubscribeResult result = zqHttpSubscriber(sysSubscribe);
                if (result!= null && result.getErrorCode() == 0){
                    sysSubscribe.setSubscribeId(result.getData().getResultList().get(0).getSubscribeId());
                    return toAjax(sysSubscribeService.insertSysSubscribe(sysSubscribe));
                }
                if (result != null) {
                    return error(result.getErrorMsg().get(0));
                }
            }
        }
        return error("未适配此来源订阅");
    }

    /**
     * 真趣订阅
     */
    private SubscribeResult zqHttpSubscriber(SysSubscribe sysSubscribe)
    {
        SubscribeResult result = null;
        SubReceiveData data = new SubReceiveData();
        List<String> buildIds = new ArrayList<>();
        buildIds.add(buildingId);
        data.setBuildIds(buildIds);
        if (sysSubscribe.getENDPOINT() != null){
            data.setServerUrl(sysSubscribe.getENDPOINT());
        }else {
            if (sysSubscribe.getTYPE().equals("tagScanUwbBeacon")){
                data.setServerUrl(domain + ":" + port + "/subscribe/callback/zqTagScanUwbBeacon");
            }
        }
        result = zqOpenApi.httpSubscriber(sysSubscribe.getTYPE(),data);
        return result;
    }


    /**
     * 修改订阅管理
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:edit')")
    @Log(title = "订阅管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysSubscribe sysSubscribe)
    {
        return toAjax(sysSubscribeService.updateSysSubscribe(sysSubscribe));
    }

    /**
     * 删除订阅管理
     */
    @PreAuthorize("@ss.hasPermi('system:subscribe:remove')")
    @Log(title = "订阅管理", businessType = BusinessType.DELETE)
	@PostMapping("/{IDs}")
    public AjaxResult remove(@PathVariable String[] IDs)
    {
        SubscribeResult result = null;
        List<String> sIDs = new ArrayList<>();
        for (String ID : IDs){
            SysSubscribe sysSubscribe = sysSubscribeService.selectSysSubscribeByID(ID);
            if (sysSubscribe.getMODE().equals("HTTP")){
                if (sysSubscribe.getSOURCE().equals("ZQ")){
                    result = zqOpenApi.httpUnSubscriber(sysSubscribe.getSubscribeId());
                }
                if (result != null && result.getErrorCode() == 0){
                    sIDs.add(ID);
                }
            }
        }
        return toAjax(sysSubscribeService.deleteSysSubscribeByIDs(IDs));
    }
}
