package com.jwzt.modules.experiment.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.utils.third.manage.JobData;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.ruoyi.common.utils.file.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.enums.BusinessType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 位置信息Controller
 * 
 * @author lx
 * @date 2025-11-04
 */
@RestController
@RequestMapping("/experiment/locationInfo")
public class LocationInfoController extends BaseController
{
    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private JobData jobData;

    @Autowired
    private BaseConfig baseConfig;

    @Value("${experiment.base.joysuch.building-id}")
    private String buildId;

    /**
     * 导出点位数据为JSON文件
     * 
     * @param locationType 类型（zq 或 xr，目前仅支持 zq）
     * @param cardId 卡片ID
     * @param startTimeStr 开始时间，格式：yyyy-MM-dd HH:mm:ss
     * @param endTimeStr 结束时间，格式：yyyy-MM-dd HH:mm:ss
     * @param response HTTP响应对象
     */
    @PreAuthorize("@ss.hasPermi('experiment:locationInfo:export')")
    @Log(title = "位置信息", businessType = BusinessType.EXPORT)
    @PostMapping("/exportPoints")
    public void exportPoints(
            @RequestParam("locationType") String locationType,
            @RequestParam("cardId") String cardId,
            @RequestParam("startTimeStr") String startTimeStr,
            @RequestParam("endTimeStr") String endTimeStr,
            HttpServletResponse response) throws IOException
    {
        try {
            // 验证类型
            if (!"zq".equals(locationType) && !"xr".equals(locationType)) {
                throw new IllegalArgumentException("不支持的类型: " + locationType + "，目前仅支持 zq");
            }
            
            JSONObject jsonObject;
            String fileName;
            
            // 根据类型处理
            if ("zq".equals(locationType)) {
                // 获取点位数据
                jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
                
                // 生成文件名: zq_points + 当前日期时间数字串
                fileName = "zq_points_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".json";
            } else if ("xr".equals(locationType)) {
                // XR 类型暂时不支持
                throw new UnsupportedOperationException("XR类型暂未实现，请选择ZQ类型");
            } else {
                throw new IllegalArgumentException("不支持的类型: " + locationType);
            }
            
            // 将 JSONObject 转换为格式化的 JSON 字符串
            String jsonString = com.alibaba.fastjson2.JSON.toJSONString(jsonObject, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
            
            // 设置响应头，返回文件供下载
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            FileUtils.setAttachmentResponseHeader(response, fileName);
            
            // 直接将 JSON 字符串写入响应流
            response.getOutputStream().write(jsonString.getBytes("UTF-8"));
            response.getOutputStream().flush();
        } catch (Exception e) {
            logger.error("导出点位数据失败，类型: " + locationType, e);
            throw new RuntimeException("导出点位数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出视觉识别事件数据为JSON文件
     * 
     * @param cameraIds 摄像机ID列表，多个ID用英文逗号分隔（可选，为空时使用配置文件中的默认值）
     * @param startTimeStr 开始时间，格式：yyyy-MM-dd HH:mm:ss
     * @param endTimeStr 结束时间，格式：yyyy-MM-dd HH:mm:ss
     * @param response HTTP响应对象
     */
    @PreAuthorize("@ss.hasPermi('experiment:locationInfo:export')")
    @Log(title = "视觉识别事件", businessType = BusinessType.EXPORT)
    @PostMapping("/exportVisionEvents")
    public void exportVisionEvents(
            @RequestParam(value = "cameraIds", required = false) String cameraIds,
            @RequestParam("startTimeStr") String startTimeStr,
            @RequestParam("endTimeStr") String endTimeStr,
            HttpServletResponse response) throws IOException
    {
        try {
            List<String> cameraIdList;
            
            // 如果前端未传入摄像机ID或为空，则使用配置文件中的默认值
            if (cameraIds == null || cameraIds.trim().isEmpty()) {
                cameraIdList = baseConfig.getCardAnalysis().getVisualIdentify().getCameraIds();
                logger.info("未指定摄像机ID，使用配置文件中的默认值：{}", cameraIdList);
            } else {
                // 解析前端传入的摄像机ID列表
                cameraIdList = Arrays.stream(cameraIds.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .collect(Collectors.toList());
                logger.info("使用前端指定的摄像机ID：{}", cameraIdList);
            }
            
            if (cameraIdList == null || cameraIdList.isEmpty()) {
                throw new IllegalArgumentException("摄像机ID列表不能为空，请在前端输入或检查配置文件");
            }
            
            logger.info("导出视觉识别事件数据：cameraIds={}, startTime={}, endTime={}", 
                    cameraIdList, startTimeStr, endTimeStr);
            
            // 调用 JobData 获取视觉识别事件列表
            List<VisionEvent> visionEvents = jobData.getVisionList(startTimeStr, endTimeStr, cameraIdList);
            
            // 生成文件名
            String fileName = "vision_events_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".json";
            
            // 将数据转换为格式化的 JSON 字符串
            String jsonString = com.alibaba.fastjson2.JSON.toJSONString(visionEvents, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
            
            // 设置响应头，返回文件供下载
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            FileUtils.setAttachmentResponseHeader(response, fileName);
            
            // 直接将 JSON 字符串写入响应流
            response.getOutputStream().write(jsonString.getBytes("UTF-8"));
            response.getOutputStream().flush();
            
            logger.info("导出视觉识别事件数据成功，共导出 {} 条记录", visionEvents.size());
        } catch (Exception e) {
            logger.error("导出视觉识别事件数据失败", e);
            throw new RuntimeException("导出视觉识别事件数据失败: " + e.getMessage(), e);
        }
    }
}
