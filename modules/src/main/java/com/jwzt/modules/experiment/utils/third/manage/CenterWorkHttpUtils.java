package com.jwzt.modules.experiment.utils.third.manage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jwzt.modules.experiment.utils.http.HttpServiceUtils;
import com.jwzt.modules.experiment.utils.http.HttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Component
public class CenterWorkHttpUtils {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HttpServiceUtils httpServiceUtils;

    @Value("${experiment.base.sw-center.status}")
    private String centerStatus;
    // 中心平台地址
    @Value("${experiment.base.sw-center.center-ip}")
    private String centerIP;
    // Rfid列表
    @Value("${experiment.base.sw-center.get-list}")
    private String getRfidList;

    /**
     * 查询RFID记录
     * @param dataTenantId 必填 场站ID
     * @param start   必填     开始时间yyyy-MM-dd HH:mm:ss SSS
     * @param end	非必填	结束时间 yyyy-MM-dd HH:mm:ss SSS
     * @return
     */
    /**
     * 查询RFID记录
     * @param dataTenantId 必填 场站ID
     * @param start   必填     开始时间yyyy-MM-dd HH:mm:ss SSS
     * @param end	非必填	结束时间 yyyy-MM-dd HH:mm:ss SSS
     * @return
     */
    public List<ReqVehicleCode> getRfidList(Long dataTenantId, String start,String end) {
        List<ReqVehicleCode> vehicleCodes = null;
        try {
            if("1".equals(centerStatus)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
                SimpleDateFormat formatReq = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                log.info("中心拉取RFID识别记录：开始时间-{} ", start);
                JSONObject object = new JSONObject();
                object.put("startDate", start);
                if(StringUtils.isNotBlank(end)) {
                    object.put("endDate", end);
                }
                object.put("tenantId", dataTenantId);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("param", object);
                log.info("中心拉取RFID-Start-开始拉取RFID识别记录："+JSON.toJSONString(map));
                JSONObject postForEntityJson = httpServiceUtils.POSTForEntityJson(map, centerIP + getRfidList);
                log.info("中心拉取RFID-IN-结束拉取RFID识别记录");
                int code = postForEntityJson.getIntValue("code");
                if (code == 20000) {
                    vehicleCodes = new ArrayList<ReqVehicleCode>();
                    JSONArray datas = postForEntityJson.getJSONArray("data");
                    log.info("中心拉取RFID-Success-结束拉取RFID识别记录，本次查询数据条数="+datas.size());
                    for (int i = 0; i < datas.size(); i++) {
                        JSONObject data = datas.getJSONObject(i);
                        String updateTime = data.getString("updateTime");// 作业记录 更新时间
                        String thirdId = data.getString("itemId");
                        String rfid = data.getString("rfid");
                        String vin = data.getString("vin");// 车辆vin
                        data.getString("orderId");// 订单id
                        String driver = data.getString("driver");// 司机名称
                        String gatherTime = data.getString("gatherTime");// 采集时间(yyyy-MM-dd HH:mm:ss SSS)
                        String gatherEquip = data.getString("gatherEquip");// 采集设备
                        String operateStationId = data.getString("operateStationId");// 操作站id
                        String operateStation = data.getString("operateStation");// 操作站
                        // 1、入库记录
                        if (StringUtils.isNotBlank(gatherTime) && StringUtils.isNotBlank(vin)) {
                            ReqVehicleCode reqVehicleCode = new ReqVehicleCode();
                            reqVehicleCode.setRecordCode(vin);
                            reqVehicleCode.setRedisSqlNum(0);
                            reqVehicleCode.setRegionId(gatherEquip);
                            reqVehicleCode.setVehicleTime(formatReq.format(format.parse(gatherTime)));
                            reqVehicleCode.setDriver(driver);
                            reqVehicleCode.setOperateStation(operateStation);
                            reqVehicleCode.setOperateStationId(operateStationId);
                            reqVehicleCode.setRfid(rfid);
                            reqVehicleCode.setThirdId(thirdId);
                            if (StringUtils.isNotBlank(updateTime)) {
                                Date upTime = format.parse(updateTime);
                                reqVehicleCode.setUpdateTime(upTime);
                            }
                            reqVehicleCode.setUpdateTimeStr(updateTime);
                            vehicleCodes.add(reqVehicleCode);
                        }
                    }
                } else {
                    log.error("中心拉取RFID-ERROR-中心拉取RFID识别记录服务异常."+postForEntityJson.toJSONString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vehicleCodes;
    }
}
