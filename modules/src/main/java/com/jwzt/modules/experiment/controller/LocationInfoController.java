package com.jwzt.modules.experiment.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.domain.TakCardInfo;
import com.jwzt.modules.experiment.service.ITakCardInfoService;
import com.jwzt.modules.experiment.utils.third.manage.JobData;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.file.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.enums.BusinessType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @Autowired
    private ITakCardInfoService takCardInfoService;

    @Value("${experiment.base.joysuch.building-id}")
    private String buildId;

    /** 用于并发拉取卡点位数据的线程池，核心数8，最大16，队列128 */
    private static final ExecutorService CARD_FETCH_EXECUTOR = new ThreadPoolExecutor(
            8, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(128),
            r -> { Thread t = new Thread(r, "card-fetch-" + r.hashCode()); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 查询当前货场启用的卡ID列表
     *
     * @param yardId 货场ID（可选，为空则查所有货场）
     * @return 启用的卡ID列表
     */
    @PreAuthorize("@ss.hasPermi('experiment:locationInfo:export')")
    @GetMapping("/enabledCardIds")
    public AjaxResult getEnabledCardIds(
            @RequestParam(value = "yardId", required = false) String yardId)
    {
        TakCardInfo query = new TakCardInfo();
        query.setEnabled(0);  // 0=启用
        query.setType("zq");  // 仅查 zq 类型
        if (yardId != null && !yardId.trim().isEmpty()) {
            query.setYardId(yardId);
        }
        java.util.List<String> cardIds = takCardInfoService.selectTakCardIdList(query);
        return AjaxResult.success(cardIds);
    }

    /**
     * 导出点位数据为JSON文件（多卡时打包为ZIP）
     *
     * @param locationType 类型（zq 或 xr，目前仅支持 zq）
     * @param cardId 卡片ID，多张卡用英文逗号分隔
     * @param buildingId 货场ID（可选，不传时使用配置默认值）
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
            @RequestParam(value = "buildingId", required = false) String buildingId,
            @RequestParam("startTimeStr") String startTimeStr,
            @RequestParam("endTimeStr") String endTimeStr,
            HttpServletResponse response) throws IOException
    {
        try {
            // 验证类型
            if (!"zq".equals(locationType) && !"xr".equals(locationType)) {
                throw new IllegalArgumentException("不支持的类型: " + locationType + "，目前仅支持 zq");
            }
            if (!"zq".equals(locationType)) {
                throw new UnsupportedOperationException("XR类型暂未实现，请选择ZQ类型");
            }

            // 货场ID：前端传入优先，否则使用配置默认值
            String effectiveBuildId = (buildingId != null && !buildingId.trim().isEmpty()) ? buildingId.trim() : buildId;

            // 按逗号拆分卡ID列表
            List<String> cardIdList = Arrays.stream(cardId.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());

            if (cardIdList.isEmpty()) {
                throw new IllegalArgumentException("卡片ID不能为空");
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            // 并发拉取所有卡的点位数据，先全部收集到内存再写响应流，避免写流阶段因网络等待超时
            Map<String, byte[]> resultMap = new LinkedHashMap<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String cid : cardIdList) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        JSONObject jsonObject = JSONObject.parseObject(
                                zqOpenApi.getListOfPoints(cid, effectiveBuildId, startTimeStr, endTimeStr));
                        byte[] bytes = com.alibaba.fastjson2.JSON.toJSONBytes(
                                jsonObject, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
                        synchronized (resultMap) {
                            resultMap.put(cid, bytes);
                        }
                        logger.info("卡ID: {} 数据拉取成功", cid);
                    } catch (Exception e) {
                        logger.error("卡ID: {} 数据拉取失败，跳过", cid, e);
                    }
                }, CARD_FETCH_EXECUTOR));
            }
            // 等待所有任务完成，最多10分钟
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                logger.error("并发拉取卡数据超时，已完成: {}/{}", resultMap.size(), cardIdList.size());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("并发拉取卡数据异常", e);
            }

            if (resultMap.isEmpty()) {
                throw new RuntimeException("所有卡数据拉取均失败，请检查网络或卡ID是否正确");
            }

            if (resultMap.size() == 1) {
                // 单张卡（或仅1张成功）：直接返回 JSON 文件
                Map.Entry<String, byte[]> entry = resultMap.entrySet().iterator().next();
                String fileName = locationType + "_points_" + entry.getKey() + "_" + timestamp + ".json";
                response.setContentType("application/octet-stream");
                response.setCharacterEncoding("utf-8");
                FileUtils.setAttachmentResponseHeader(response, fileName);
                response.getOutputStream().write(entry.getValue());
                response.getOutputStream().flush();
            } else {
                // 多张卡：打包为 ZIP 文件
                String zipName = locationType + "_points_" + timestamp + ".zip";
                response.setContentType("application/zip");
                response.setCharacterEncoding("utf-8");
                FileUtils.setAttachmentResponseHeader(response, zipName);
                try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                    for (Map.Entry<String, byte[]> entry : resultMap.entrySet()) {
                        String entryName = locationType + "_points_" + entry.getKey() + ".json";
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.write(entry.getValue());
                        zos.closeEntry();
                    }
                }
            }
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
