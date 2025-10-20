package com.jwzt.modules.experiment.utils.third.manage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jwzt.modules.experiment.utils.http.HttpServiceUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.AssignmentRecordRequest;
import com.jwzt.modules.experiment.utils.third.manage.domain.BeaconPushRequest;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import com.jwzt.modules.experiment.utils.third.manage.domain.VehicleEntryExitRequest;
import com.jwzt.modules.experiment.utils.third.manage.domain.VehicleTrackRequest;
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
    // 进场、出场推送
    @Value("${experiment.base.card-analysis.vehicle-entry-exit}")
    private String vehicleEntryExitUrl;
    // 车辆停放车位、车辆离开车位
    @Value("${experiment.base.card-analysis.assignment-record}")
    private String assignmentRecordUrl;
    // 实时轨迹
    @Value("${experiment.base.card-analysis.vehicle-track}")
    private String vehicleTrackUrl;
    // UWB推送
    @Value("${experiment.base.card-analysis.beacon-push}")
    private String beaconPushUrl;
    // 车辆删除
    @Value("${experiment.base.card-analysis.remove-vehicle}")
    private String removeVehicleUrl;

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

    /**
     * 车辆进场、出场推送
     * @param request 请求参数
     * @return
     */
    public JSONObject vehicleEntryAndExit(VehicleEntryExitRequest request) {
        JSONObject result = null;
        try {
            if("1".equals(centerStatus)) {
                log.info("车辆进场、出场推送-Start-开始推送：type={}, vehicleThirdId={}, vehicleTime={}", 
                    request.getType(), request.getVehicleThirdId(), request.getVehicleTime());
                
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("type", request.getType());
                map.put("vehicleThirdId", request.getVehicleThirdId());
                map.put("vehicleTime", request.getVehicleTime());
                map.put("recordFiles", request.getRecordFiles());
                map.put("cameraId", request.getCameraId());
                map.put("through", request.getThrough());
                map.put("weft", request.getWeft());
                map.put("vehicleCode", request.getVehicleCode());
                map.put("vehicleThirdBrand", request.getVehicleThirdBrand());
                map.put("vehicleColor", request.getVehicleColor());
                map.put("vehicleType", request.getVehicleType());
                map.put("vehicleShape", request.getVehicleShape());
                map.put("regionType", request.getRegionType());
                map.put("carType", request.getCarType());
                
                log.info("车辆进场、出场推送-请求参数："+JSON.toJSONString(map));
                result = httpServiceUtils.POSTForEntityJson(map, centerIP + vehicleEntryExitUrl);
                log.info("车辆进场、出场推送-响应结果："+result.toJSONString());
                
                int code = result.getIntValue("code");
                if (code == 20000) {
                    log.info("车辆进场、出场推送-Success-推送成功");
                } else {
                    log.error("车辆进场、出场推送-ERROR-推送失败："+result.toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("车辆进场、出场推送-Exception-异常：", e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 车辆停放车位、车辆离开车位
     * @param request 请求参数
     * @return
     */
    public JSONObject assignmentRecord(AssignmentRecordRequest request) {
        JSONObject result = null;
        try {
            if("1".equals(centerStatus)) {
                log.info("车辆停放/离开车位-Start-开始推送：type={}, vehicleThirdId={}, vehicleTime={}", 
                    request.getType(), request.getVehicleThirdId(), request.getVehicleTime());
                
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("type", request.getType());
                map.put("vehicleThirdId", request.getVehicleThirdId());
                map.put("vehicleTime", request.getVehicleTime());
                map.put("recordFiles", request.getRecordFiles());
                map.put("cameraId", request.getCameraId());
                map.put("through", request.getThrough());
                map.put("weft", request.getWeft());
                map.put("vehicleCode", request.getVehicleCode());
                map.put("vehicleThirdBrand", request.getVehicleThirdBrand());
                map.put("vehicleColor", request.getVehicleColor());
                map.put("vehicleType", request.getVehicleType());
                map.put("vehicleShape", request.getVehicleShape());
                map.put("regionType", request.getRegionType());
                map.put("carType", request.getCarType());
                
                log.info("车辆停放/离开车位-请求参数："+JSON.toJSONString(map));
                result = httpServiceUtils.POSTForEntityJson(map, centerIP + assignmentRecordUrl);
                log.info("车辆停放/离开车位-响应结果："+result.toJSONString());
                
                int code = result.getIntValue("code");
                if (code == 20000) {
                    log.info("车辆停放/离开车位-Success-推送成功");
                } else {
                    log.error("车辆停放/离开车位-ERROR-推送失败："+result.toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("车辆停放/离开车位-Exception-异常：", e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 实时轨迹推送
     * @param request 请求参数
     * @return
     */
    public JSONObject vehicleTrack(VehicleTrackRequest request) {
        JSONObject result = null;
        try {
            if("1".equals(centerStatus)) {
                log.info("实时轨迹推送-Start-开始推送：vehicleThirdId={}, vehicleTime={}", 
                    request.getVehicleThirdId(), request.getVehicleTime());
                
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("vehicleThirdId", request.getVehicleThirdId());
                map.put("vehicleTime", request.getVehicleTime());
                map.put("cameraId", request.getCameraId());
                map.put("through", request.getThrough());
                map.put("weft", request.getWeft());
                map.put("carType", request.getCarType());
                
                log.info("实时轨迹推送-请求参数："+JSON.toJSONString(map));
                result = httpServiceUtils.POSTForEntityJson(map, centerIP + vehicleTrackUrl);
                log.info("实时轨迹推送-响应结果："+result.toJSONString());
                
                int code = result.getIntValue("code");
                if (code == 20000) {
                    log.info("实时轨迹推送-Success-推送成功");
                } else {
                    log.error("实时轨迹推送-ERROR-推送失败："+result.toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("实时轨迹推送-Exception-异常：", e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * UWB推送
     * RFID绑定UWB信标数据推送、识别车车辆距离信标3米内时上报轨迹
     * @param request 请求参数
     * @return
     */
    public JSONObject beaconPush(BeaconPushRequest request) {
        JSONObject result = null;
        try {
            if("1".equals(centerStatus)) {
                log.info("UWB推送-Start-开始推送：type={}, vehicleTime={}, distance={}", 
                    request.getType(), request.getVehicleTime(), request.getDistance());
                
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("type", request.getType());
                map.put("vehicleTime", request.getVehicleTime());
                map.put("cameraId", request.getCameraId());
                map.put("through", request.getThrough());
                map.put("weft", request.getWeft());
                map.put("distance", request.getDistance());
                
                log.info("UWB推送-请求参数："+JSON.toJSONString(map));
                result = httpServiceUtils.POSTForEntityJson(map, centerIP + beaconPushUrl);
                log.info("UWB推送-响应结果："+result.toJSONString());
                
                int code = result.getIntValue("code");
                if (code == 20000) {
                    log.info("UWB推送-Success-推送成功");
                } else {
                    log.error("UWB推送-ERROR-推送失败："+result.toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("UWB推送-Exception-异常：", e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 车辆删除
     * @param vehicleThirdId 视频分析系统生产车辆ID
     * @return
     */
    public JSONObject removeVehicle(String vehicleThirdId) {
        JSONObject result = null;
        try {
            if("1".equals(centerStatus)) {
                log.info("车辆删除-Start-开始删除：vehicleThirdId={}", vehicleThirdId);
                
                String url = centerIP + removeVehicleUrl + "?vehicleThirdId=" + vehicleThirdId;
                log.info("车辆删除-请求地址："+url);
                result = httpServiceUtils.GETForEntity(url);
                log.info("车辆删除-响应结果："+result.toJSONString());
                
                int code = result.getIntValue("code");
                if (code == 20000) {
                    log.info("车辆删除-Success-删除成功");
                } else {
                    log.error("车辆删除-ERROR-删除失败："+result.toJSONString());
                }
            }
        } catch (Exception e) {
            log.error("车辆删除-Exception-异常：", e);
            e.printStackTrace();
        }
        return result;
    }
}
