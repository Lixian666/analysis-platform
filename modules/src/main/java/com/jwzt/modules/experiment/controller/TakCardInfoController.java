package com.jwzt.modules.experiment.controller;

import com.jwzt.modules.experiment.domain.TakCardInfo;
import com.jwzt.modules.experiment.service.ITakCardInfoService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 定位卡信息Controller
 *
 * @author lx
 * @date 2025-11-13
 */
@RestController
@RequestMapping("/experiment/cardInfo")
public class TakCardInfoController extends BaseController {

    @Autowired
    private ITakCardInfoService takCardInfoService;

    /**
     * 查询定位卡信息列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(TakCardInfo takCardInfo) {
        startPage();
        List<TakCardInfo> list = takCardInfoService.selectTakCardInfoList(takCardInfo);
        return getDataTable(list);
    }

    /**
     * 导出定位卡信息列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:export')")
    @Log(title = "定位卡信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TakCardInfo takCardInfo) {
        List<TakCardInfo> list = takCardInfoService.selectTakCardInfoList(takCardInfo);
        ExcelUtil<TakCardInfo> util = new ExcelUtil<>(TakCardInfo.class);
        util.exportExcel(response, list, "定位卡信息数据");
    }

    /**
     * 获取定位卡信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(takCardInfoService.selectTakCardInfoById(id));
    }

    /**
     * 新增定位卡信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:add')")
    @Log(title = "定位卡信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TakCardInfo takCardInfo) {
        validCardInfo(takCardInfo);
        return toAjax(takCardInfoService.insertTakCardInfo(takCardInfo));
    }

    /**
     * 修改定位卡信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:edit')")
    @Log(title = "定位卡信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TakCardInfo takCardInfo) {
        if (takCardInfo.getId() == null) {
            return error("定位卡ID不能为空");
        }
        validCardInfo(takCardInfo);
        return toAjax(takCardInfoService.updateTakCardInfo(takCardInfo));
    }

    /**
     * 修改定位卡启用状态
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:edit')")
    @Log(title = "定位卡信息", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult changeStatus(@RequestBody TakCardInfo takCardInfo) {
        if (takCardInfo.getId() == null) {
            return error("定位卡ID不能为空");
        }
        if (takCardInfo.getEnabled() == null) {
            return error("启用状态不能为空");
        }
        return toAjax(takCardInfoService.changeTakCardInfoStatus(takCardInfo));
    }

    /**
     * 删除定位卡信息
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:remove')")
    @Log(title = "定位卡信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(takCardInfoService.deleteTakCardInfoByIds(ids));
    }

    /**
     * 导入定位卡信息数据
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:import')")
    @Log(title = "定位卡信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TakCardInfo> util = new ExcelUtil<>(TakCardInfo.class);
        List<TakCardInfo> cardInfoList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = takCardInfoService.importTakCardInfo(cardInfoList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("@ss.hasPermi('experiment:cardInfo:import')")
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil<TakCardInfo> util = new ExcelUtil<>(TakCardInfo.class);
        util.importTemplateExcel(response, "定位卡信息数据");
    }

    private void validCardInfo(TakCardInfo takCardInfo) {
        if (StringUtils.isEmpty(takCardInfo.getCardId())) {
            throw new IllegalArgumentException("卡ID不能为空");
        }
        if (StringUtils.isEmpty(takCardInfo.getYardId())) {
            throw new IllegalArgumentException("货场ID不能为空");
        }
        if (StringUtils.isEmpty(takCardInfo.getYardName())) {
            throw new IllegalArgumentException("货场名称不能为空");
        }
        if (StringUtils.isEmpty(takCardInfo.getType())) {
            throw new IllegalArgumentException("定位卡类型不能为空");
        }
        if (takCardInfo.getBizType() == null) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (takCardInfo.getEnabled() == null) {
            takCardInfo.setEnabled(1);
        }
    }
}

