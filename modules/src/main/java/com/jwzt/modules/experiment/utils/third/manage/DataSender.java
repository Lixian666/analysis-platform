package com.jwzt.modules.experiment.utils.third.manage;

import com.alibaba.fastjson.JSONObject;
import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.AssignmentRecordRequest;
import com.jwzt.modules.experiment.utils.third.manage.domain.VehicleEntryExitRequest;
import com.jwzt.modules.experiment.utils.third.manage.domain.VehicleTrackRequest;
import com.jwzt.modules.experiment.vo.EventState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataSender {

    @Autowired
    private CenterWorkHttpUtils centerWorkHttpUtils;

    // 中心平台地址
    @Value("${experiment.base.push-data}")
    private boolean pushData;

    /**
     * 推送车辆进场信息
     * @param sess
     * @param vehicleType
     * @return
     */
    public JSONObject inYardPush(RealTimeDriverTracker.TrackSession sess, RealTimeDriverTracker.VehicleType vehicleType) {
        if (!pushData){
            // 不推送数据时，直接构造返回对象，避免无效 JSON 解析
            JSONObject result = new JSONObject();
            result.put("code", 50000);
            result.put("message", "不推送数据");
            result.put("data", new JSONObject());
            result.put("success", true);
            return result;
        }
        VehicleEntryExitRequest request = new VehicleEntryExitRequest();
        if (vehicleType == RealTimeDriverTracker.VehicleType.TRUCK) {
            request.setType(0);
            request.setVehicleThirdId(sess.sessionId);
            request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.startTime));
            request.setCameraId(sess.cardId);
            request.setThrough(String.valueOf(sess.startLongitude));
            request.setWeft(String.valueOf(sess.startLatitude));
            request.setRegionType(4);
            request.setCarType("car");
        }
        if (vehicleType == RealTimeDriverTracker.VehicleType.CAR) {
            request.setType(0);
            request.setVehicleThirdId(sess.sessionId);
            request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.startTime));
            request.setCameraId(sess.cardId);
            request.setThrough(String.valueOf(sess.startLongitude));
            request.setWeft(String.valueOf(sess.startLatitude));
            request.setRegionType(2);
            request.setCarType("car");
            request.setVehicleCode(sess.vin);
        }
        JSONObject result = centerWorkHttpUtils.vehicleEntryAndExit(request);
        System.out.println("推送车辆进场信息：" + result);
        return result;
    }

    /**
     * 推送车辆出场信息
     * @param sess
     * @param vehicleType
     * @return
     */
    public JSONObject outYardPush(RealTimeDriverTracker.TrackSession sess, RealTimeDriverTracker.VehicleType vehicleType) {
        if (!pushData){
            // 不推送数据时，直接构造返回对象，避免无效 JSON 解析
            JSONObject result = new JSONObject();
            result.put("code", 50000);
            result.put("message", "不推送数据");
            result.put("data", new JSONObject());
            result.put("success", true);
            return result;
        }
        VehicleEntryExitRequest request = new VehicleEntryExitRequest();
        if (vehicleType == RealTimeDriverTracker.VehicleType.CAR) {
            request.setType(1);
            request.setVehicleThirdId(sess.sessionId);
            request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.endTime));
            request.setCameraId(sess.cardId);
            request.setThrough(String.valueOf(sess.endLongitude));
            request.setWeft(String.valueOf(sess.endLatitude));
            request.setRegionType(2);
            request.setCarType("car");
            request.setVehicleCode(sess.vin);
        }
        JSONObject result = centerWorkHttpUtils.vehicleEntryAndExit(request);
        System.out.println("推送车辆出场信息：" + result);
        return result;
    }

    /**
     * 推送车辆入库信息
     * @param sess
     * @param vehicleType
     * @return
     */
    public JSONObject inParkPush(RealTimeDriverTracker.TrackSession sess, RealTimeDriverTracker.VehicleType vehicleType){
        if (!pushData){
            // 不推送数据时，直接构造返回对象，避免无效 JSON 解析
            JSONObject result = new JSONObject();
            result.put("code", 50000);
            result.put("message", "不推送数据");
            result.put("data", new JSONObject());
            result.put("success", true);
            return result;
        }
        AssignmentRecordRequest request = new AssignmentRecordRequest();
        if (vehicleType == RealTimeDriverTracker.VehicleType.TRUCK) {
            request.setType(0);
            request.setVehicleThirdId(sess.sessionId);
            request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.endTime));
            request.setCameraId(sess.cardId);
            request.setThrough(String.valueOf(sess.endLongitude));
            request.setWeft(String.valueOf(sess.endLatitude));
            request.setRegionType(4);
            request.setCarType("car");
            request.setVehicleCode(sess.vin);
        }
        if (vehicleType == RealTimeDriverTracker.VehicleType.CAR){
            if (sess.kind == RealTimeDriverTracker.EventKind.CAR_ARRIVED || sess.kind == RealTimeDriverTracker.EventKind.CAR_SEND){
                request.setType(0);
                request.setVehicleThirdId(sess.sessionId);
                request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.endTime));
                request.setCameraId(sess.cardId);
                request.setThrough(String.valueOf(sess.endLongitude));
                request.setWeft(String.valueOf(sess.endLatitude));
                request.setRegionType(1);
                request.setCarType("car");
                request.setVehicleCode(sess.vin);
            }
            if (sess.kind == RealTimeDriverTracker.EventKind.ARRIVED || sess.kind == RealTimeDriverTracker.EventKind.SEND){
                request.setType(0);
                request.setVehicleThirdId(sess.sessionId);
                request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.endTime));
                request.setCameraId(sess.cardId);
                request.setThrough(String.valueOf(sess.endLongitude));
                request.setWeft(String.valueOf(sess.endLatitude));
                request.setRegionType(2);
                request.setCarType("car");
                request.setVehicleCode(sess.vin);
            }

        }
        JSONObject result = centerWorkHttpUtils.assignmentRecord(request);
        System.out.println("推送车辆入库信息：" + result);
        return result;
    }

    /**
     * 推送车辆出库信息
     * @param sess
     * @param vehicleType
     * @return
     */
    public JSONObject outParkPush(RealTimeDriverTracker.TrackSession sess, RealTimeDriverTracker.VehicleType vehicleType){
        if (!pushData){
            // 不推送数据时，直接构造返回对象，避免无效 JSON 解析
            JSONObject result = new JSONObject();
            result.put("code", 50000);
            result.put("message", "不推送数据");
            result.put("data", new JSONObject());
            result.put("success", true);
            return result;
        }
        AssignmentRecordRequest request = new AssignmentRecordRequest();
        if (vehicleType == RealTimeDriverTracker.VehicleType.CAR) {
            request.setType(1);
            request.setVehicleThirdId(sess.sessionId);
            request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(sess.startTime));
            request.setCameraId(sess.cardId);
            request.setThrough(String.valueOf(sess.startLongitude));
            request.setWeft(String.valueOf(sess.startLatitude));
            request.setRegionType(2);
            request.setCarType("car");
            request.setVehicleCode(sess.vin);
        }
        JSONObject result = centerWorkHttpUtils.assignmentRecord(request);
        System.out.println("推送车辆出库信息：" + result);
        return result;
    }

    /**
     * 推送车辆轨迹信息
     * @param es
     * @param sess
     * @param vehicleType
     * @return
     */
    public JSONObject trackPush(EventState es, LocationPoint point, RealTimeDriverTracker.TrackSession sess, RealTimeDriverTracker.VehicleType vehicleType){
        if (!pushData){
            // 不推送数据时，直接构造返回对象，避免无效 JSON 解析
            JSONObject result = new JSONObject();
            result.put("code", 50000);
            result.put("message", "不推送数据");
            result.put("data", new JSONObject());
            result.put("success", true);
            return result;
        }
        if (point == null){
            point = new LocationPoint();
            point.setTimestamp(es.getTimestamp());
            point.setLongitude(es.getLang());
            point.setLatitude(es.getLat());
        }
        VehicleTrackRequest request = new VehicleTrackRequest();
        request.setVehicleThirdId(sess.sessionId);
        request.setVehicleTime(DateTimeUtils.timestampToDateTimeSSSStr(point.getTimestamp()));
        request.setCameraId(sess.cardId);
        request.setThrough(String.valueOf(point.getLongitude()));
        request.setWeft(String.valueOf(point.getLatitude()));
        request.setCarType("car");
        JSONObject result = centerWorkHttpUtils.vehicleTrack(request);
        System.out.println("推送车辆轨迹信息：" + result);
        return result;
    }


    /**
     * 删除指定车辆信息行为信息
     * @param vehicleThirdId
     * @return
     */
    public JSONObject removeVehicle(String vehicleThirdId){
        JSONObject result = centerWorkHttpUtils.removeVehicle(vehicleThirdId);
        return result;
    }

}
