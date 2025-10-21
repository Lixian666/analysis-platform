package com.jwzt.modules.experiment.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 行为记录Controller
 * 
 * @author lx
 * @date 2025-07-09
 */
@RestController
@RequestMapping("/experiment/experiment")
public class TakBehaviorRecordsController extends BaseController
{
    @Autowired
    private ITakBehaviorRecordsService takBehaviorRecordsService;

    /**
     * 查询行为记录列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:list')")
    @GetMapping("/list")
    public TableDataInfo list(TakBehaviorRecords takBehaviorRecords)
    {
//        startPage();
        List<TakBehaviorRecords> list = takBehaviorRecordsService.selectTakBehaviorRecordsList(takBehaviorRecords);
        return getDataTable(list);
    }

    /**
     * 查询行为记录列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:list')")
    @GetMapping("/userList")
    public TableDataInfo userList(TakBehaviorRecords takBehaviorRecords)
    {
//        startPage();
        List<TakBehaviorRecords> list = takBehaviorRecordsService.selectTakBehaviorRecordsUserList(takBehaviorRecords);
        return getDataTable(list);
    }

    /**
     * 导出行为记录列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:export')")
    @Log(title = "行为记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TakBehaviorRecords takBehaviorRecords)
    {
        List<TakBehaviorRecords> list = takBehaviorRecordsService.selectTakBehaviorRecordsList(takBehaviorRecords);
        ExcelUtil<TakBehaviorRecords> util = new ExcelUtil<TakBehaviorRecords>(TakBehaviorRecords.class);
        util.exportExcel(response, list, "行为记录数据");
    }

    /**
     * 获取行为记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(takBehaviorRecordsService.selectTakBehaviorRecordsById(id));
    }

    /**
     * 新增行为记录
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:add')")
    @Log(title = "行为记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TakBehaviorRecords takBehaviorRecords)
    {
        return toAjax(takBehaviorRecordsService.insertTakBehaviorRecords(takBehaviorRecords));
    }

    /**
     * 修改行为记录
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:edit')")
    @Log(title = "行为记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TakBehaviorRecords takBehaviorRecords)
    {
        return toAjax(takBehaviorRecordsService.updateTakBehaviorRecords(takBehaviorRecords));
    }

    /**
     * 删除行为记录（批量按条件删除）
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:remove')")
    @Log(title = "行为记录", businessType = BusinessType.DELETE)
	@PostMapping("/delete")
    public AjaxResult remove(@RequestBody List<TakBehaviorRecords> deleteParams)
    {
        if (deleteParams == null || deleteParams.isEmpty()) {
            return error("删除参数不能为空");
        }
        
        int totalDeleted = 0;
        for (TakBehaviorRecords deleteParam : deleteParams) {
            // 按条件删除（cardId、yardId、startTime）
            if (deleteParam.getCardId() != null && deleteParam.getYardId() != null && deleteParam.getStartTime() != null)
            {
                totalDeleted += takBehaviorRecordsService.deleteTakBehaviorRecordsByCondition(deleteParam);
            }
        }
        
        return totalDeleted > 0 ? success() : error("删除失败");
    }
    
    /**
     * 批量删除行为记录（按ID）
     */
    @PreAuthorize("@ss.hasPermi('experiment:experiment:remove')")
    @Log(title = "行为记录", businessType = BusinessType.DELETE)
	@PostMapping("/deleteByIds")
    public AjaxResult removeByIds(@RequestBody Long[] ids)
    {
        return toAjax(takBehaviorRecordsService.deleteTakBehaviorRecordsByIds(ids));
    }
}
