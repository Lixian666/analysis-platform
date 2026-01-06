package com.jwzt.modules.experiment.utils.third.manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.utils.http.HttpServiceUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.VisionEvent;

@Component
public class JobData {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HttpServiceUtils httpServiceUtils;

    @Autowired
    private BaseConfig baseConfig;

    /**
     * 获取Vision事件列表
     * @param startTime 开始时间 yyyy-MM-dd HH:mm:ss
     * @param endTime 结束时间 yyyy-MM-dd HH:mm:ss
     * @param cameraIds 摄像机ID列表
     * @return Vision事件列表
     */
    public List<VisionEvent> getVisionList(String startTime, String endTime, List<String> cameraIds) {
        List<VisionEvent> visionEvents = new ArrayList<>();
        try {
            log.info("获取Vision事件列表-Start-开始查询：startTime={}, endTime={}, cameraIds={}", 
                startTime, endTime, cameraIds);
            
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("cameraIds", cameraIds);
            
            String visualIdentifyBaseUrl = baseConfig.getCardAnalysis().getVisualIdentify().getBaseUrl();
            if (visualIdentifyBaseUrl == null || visualIdentifyBaseUrl.isEmpty()) {
                log.error("视觉识别服务地址未配置");
                return visionEvents;
            }
            String url = visualIdentifyBaseUrl + "/vision/visionevent/list";
            log.info("获取Vision事件列表-请求地址：{}", url);
            log.info("获取Vision事件列表-请求参数：{}", JSON.toJSONString(map));
            
            JSONObject response = httpServiceUtils.POSTForEntityJson(map, url);
            log.info("获取Vision事件列表-响应结果：{}", response.toJSONString());
            
            int code = response.getIntValue("code");
            if (code == 0) {
                JSONArray dataArray = response.getJSONArray("data");
                if (dataArray != null && !dataArray.isEmpty()) {
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject dataObj = dataArray.getJSONObject(i);
                        VisionEvent visionEvent = JSON.parseObject(dataObj.toJSONString(), VisionEvent.class);
                        visionEvents.add(visionEvent);
                    }
                    log.info("获取Vision事件列表-Success-查询成功，本次查询数据条数={}", visionEvents.size());
                } else {
                    log.info("获取Vision事件列表-Success-查询成功，但无数据");
                }
            } else {
                log.error("获取Vision事件列表-ERROR-查询失败：{}", response.toJSONString());
            }
        } catch (Exception e) {
            log.error("获取Vision事件列表-Exception-异常：", e);
            e.printStackTrace();
        }
        return visionEvents;
    }
}
