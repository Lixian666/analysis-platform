package com.jwzt.modules.experiment.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.vo.TakBehaviorRecordDetailVo;
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
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 行为记录详情Controller
 * 
 * @author lx
 * @date 2025-07-09
 */
@RestController
@RequestMapping("/experiment/detail")
public class TakBehaviorRecordDetailController extends BaseController
{
    @Autowired
    private ITakBehaviorRecordDetailService takBehaviorRecordDetailService;

    @Autowired
    private ITakBehaviorRecordsService takBehaviorRecordsService;

    /**
     * 查询行为记录详情列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:list')")
    @GetMapping("/list")
    public TableDataInfo list(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
//        startPage();
        List<TakBehaviorRecordDetail> list = takBehaviorRecordDetailService.selectTakBehaviorRecordDetailList(takBehaviorRecordDetail);
        return getDataTable(list);
    }

    /**
     * 查询行为记录详情列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:list')")
    @RequestMapping("/listByUserId")
    public TableDataInfo listAllByUserId(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
//        startPage();
        TakBehaviorRecords takBehaviorRecords = new TakBehaviorRecords();
        takBehaviorRecords.setCardId(takBehaviorRecordDetail.getCardId());
        List<TakBehaviorRecordDetail> list = takBehaviorRecordDetailService.selectTakBehaviorRecordDetailList(takBehaviorRecordDetail);
        Map<String, List<TakBehaviorRecordDetail>> grouped = list.stream()
                .collect(Collectors.groupingBy(TakBehaviorRecordDetail::getTrackId));

        grouped.replaceAll((trackId, l) -> l.stream()
                        .sorted(Comparator.comparing(TakBehaviorRecordDetail::getRecordTime))
                        .collect(Collectors.toList())
        );
        List<TakBehaviorRecords> RecordsList = takBehaviorRecordsService.selectTakBehaviorRecordsList(takBehaviorRecords);
        for (TakBehaviorRecords record : RecordsList){
            List<TakBehaviorRecordDetail> details = grouped.get(record.getTrackId());
            record.setTakBehaviorRecordDetailList(details);

        }
        return getDataTable(RecordsList);
    }

    /**
     * 导出行为记录详情列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:export')")
    @Log(title = "行为记录详情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        List<TakBehaviorRecordDetail> list = takBehaviorRecordDetailService.selectTakBehaviorRecordDetailList(takBehaviorRecordDetail);
        ExcelUtil<TakBehaviorRecordDetail> util = new ExcelUtil<TakBehaviorRecordDetail>(TakBehaviorRecordDetail.class);
        util.exportExcel(response, list, "行为记录详情数据");
    }

    /**
     * 获取行为记录详情详细信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(takBehaviorRecordDetailService.selectTakBehaviorRecordDetailById(id));
    }

    /**
     * 新增行为记录详情
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:add')")
    @Log(title = "行为记录详情", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        return toAjax(takBehaviorRecordDetailService.insertTakBehaviorRecordDetail(takBehaviorRecordDetail));
    }

    /**
     * 修改行为记录详情
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:edit')")
    @Log(title = "行为记录详情", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        return toAjax(takBehaviorRecordDetailService.updateTakBehaviorRecordDetail(takBehaviorRecordDetail));
    }

    /**
     * 删除行为记录详情
     */
    @PreAuthorize("@ss.hasPermi('experiment:detail:remove')")
    @Log(title = "行为记录详情", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(takBehaviorRecordDetailService.deleteTakBehaviorRecordDetailByIds(ids));
    }
}
