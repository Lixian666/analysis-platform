package com.jwzt.modules.experiment.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
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
import com.jwzt.modules.experiment.domain.TakBeaconInfo;
import com.jwzt.modules.experiment.service.ITakBeaconInfoService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 信标信息Controller
 * 
 * @author lx
 * @date 2025-09-16
 */
@RestController
@RequestMapping("/experiment/beaconInfo")
public class TakBeaconInfoController extends BaseController
{

    @Autowired
    private ZQOpenApi zqOpenApi;
    @Autowired
    private ITakBeaconInfoService takBeaconInfoService;

    @Value("${experiment.base.joysuch.building-id}")
    private String buildId;
    @Value("${experiment.base.joysuch.building-name}")
    private String buildName;

    /**
     * 查询信标信息列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(TakBeaconInfo takBeaconInfo)
    {
        startPage();
        List<TakBeaconInfo> list = takBeaconInfoService.selectTakBeaconInfoList(takBeaconInfo);
        return getDataTable(list);
    }

    /**
     * 查询信标ID列表
     */
//    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:list')")
    @GetMapping("/beaconIds")
    public List<String> beaconIds()
    {
        String UWBBeaconsStr = zqOpenApi.getListOfUWBBeacons();
        JSONArray jsonArray = JSONObject.parseObject(UWBBeaconsStr).getJSONObject("data").getJSONArray("data");
        List<String> beaconIds = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++){
            beaconIds.add(jsonArray.getJSONObject(i).getString("mac"));
        }
        return beaconIds;
    }

    /**
     * 导出信标信息列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:export')")
    @Log(title = "信标信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TakBeaconInfo takBeaconInfo)
    {
        List<TakBeaconInfo> list = takBeaconInfoService.selectTakBeaconInfoList(takBeaconInfo);
        ExcelUtil<TakBeaconInfo> util = new ExcelUtil<TakBeaconInfo>(TakBeaconInfo.class);
        util.exportExcel(response, list, "信标信息数据");
    }

    /**
     * 获取信标信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(takBeaconInfoService.selectTakBeaconInfoById(id));
    }

    /**
     * 新增信标信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:add')")
    @Log(title = "信标信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TakBeaconInfo takBeaconInfo)
    {
        if (takBeaconInfo!= null && takBeaconInfo.getBuildId() == null){
            takBeaconInfo.setBuildId(buildId);
            takBeaconInfo.setBuildName(buildName);
        }
        return toAjax(takBeaconInfoService.insertTakBeaconInfo(takBeaconInfo));
    }

    /**
     * 修改信标信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:edit')")
    @Log(title = "信标信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TakBeaconInfo takBeaconInfo)
    {
        return toAjax(takBeaconInfoService.updateTakBeaconInfo(takBeaconInfo));
    }

    /**
     * 删除信标信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:beaconInfo:remove')")
    @Log(title = "信标信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(takBeaconInfoService.deleteTakBeaconInfoByIds(ids));
    }
}
