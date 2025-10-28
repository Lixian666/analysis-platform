package com.ruoyi.quartz.task;

import com.jwzt.modules.experiment.domain.TakBehaviorRecords;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class BALastProcessTask {

    @Value("${experiment.base.joysuch.building-id}")
    private String yardId;
    @Value("${experiment.base.sw-center.tenant-id}")
    private Long tenantId;

    @Autowired
    private ITakBehaviorRecordsService takBehaviorRecordsService;

    @Autowired
    private CenterWorkHttpUtils centerWorkHttpUtils;

    //作业数据与rfid数据匹配
    public void theJobDataMatchesTheRFIDData() {
        String cardId = "1918B3000561";
        String startTimeStr = "2025-10-16 18:25:00";
        String endTimeStr = "2025-10-16 19:50:00";
        // TODO: 2025/7/10 获取作业数据
        TakBehaviorRecords tr = new TakBehaviorRecords();
        tr.setYardId(yardId);
        tr.setType(0L);
        tr.setState("完成");
        tr.setCardId(cardId);
        tr.setQueryStartTime(startTimeStr);
        tr.setQueryEndTime(endTimeStr);
        List<TakBehaviorRecords> takBehaviorRecords = takBehaviorRecordsService.selectTakBehaviorRecordsList();
        // TODO: 2025/7/10 获取rfid数据
        List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(tenantId, startTimeStr + " 000", endTimeStr + " 000");
        // TODO: 2025/7/10 匹配数据
        // TODO: 2025/7/10 保存匹配数据
         // TODO: 区分匹配结果 作业数据多识别列表、rfid数据多识别列表、正确匹配列表
        // TODO: 2025/7/10 删除匹配数据
        System.out.println("BALastProcessTask.process()");
    }
}
