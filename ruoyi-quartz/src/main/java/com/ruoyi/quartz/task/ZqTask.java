package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.util.JobInvokeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 真趣定时任务调度
 *
 * @author lixian
 */
@Component("zqTask")
public class ZqTask {
    @Autowired
    private ZQOpenApi zqOpenApi;

    private static final Logger log = LoggerFactory.getLogger(ZqTask.class);


    @Value("${server.servlet.domain-name}")
    private String domain;
    @Value("${server.port}")
    private String port;
    @Value("${experiment.base.joysuch.building-id}")
    private String buildingId;

    public void ZQGetTagStateHistoryOfTagID()
    {
        String cardId = "1918B3000BA8";
        String startTimeStr = "2025-09-12 14:17:00";
        String endTimeStr = "2025-09-12 14:24:00";
        JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(null, cardId, startTimeStr, endTimeStr));
        JSONArray data = jsonObject.getJSONArray("data");
        List<TagScanUwbData> tagScanUwbDataList = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            TagScanUwbData tag = data.getObject(i, TagScanUwbData.class);
            tag.setDateTime(DateTimeUtils.timestampToDateTimeStr(tag.getTime()));
            tagScanUwbDataList.add(tag);
        }
        JSONArray newData = new JSONArray(tagScanUwbDataList);
        jsonObject.put("data", newData);
        System.out.println(jsonObject);
    }

    public void ZQHttpSubscriber(String type)
    {
        SubReceiveData data = new SubReceiveData();
        List<String> buildIds = new ArrayList<>();
        buildIds.add(buildingId);
        data.setBuildIds(buildIds);
        data.setServerUrl(domain + ":" + port + "/subscribe/callback/zqTagScanUwbBeacon");
        SubscribeResult result = zqOpenApi.httpSubscriber(type,data);
        log.debug("订阅结果：{}",result);
    }

    public void ZQHttpUnSubscriber()
    {
        SubscribeResult result = zqOpenApi.httpUnSubscriber("82");
        log.debug("订阅结果：{}",result);
    }

}
