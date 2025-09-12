package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.util.JobInvokeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    @Value("${experiment.base.building-id}")
    private String buildingId;

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
