package com.jwzt.modules.experiment.utils.third.manage.domain;

import java.io.Serializable;

/**
 * UWB推送请求参数
 * RFID绑定UWB信标数据推送、识别车车辆距离信标3米内时上报轨迹
 */
public class BeaconPushRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 0 前进 1 远离
     */
    private Integer type;

    /**
     * 识别时间 yyyy-MM-dd HH:mm:ss:SSS
     */
    private String vehicleTime;

    /**
     * 摄像ID（定位卡）
     */
    private String cameraId;

    /**
     * 经度
     */
    private String through;

    /**
     * 纬度
     */
    private String weft;

    /**
     * 距离：cm
     */
    private Integer distance;

    public BeaconPushRequest() {
        super();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getVehicleTime() {
        return vehicleTime;
    }

    public void setVehicleTime(String vehicleTime) {
        this.vehicleTime = vehicleTime;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getThrough() {
        return through;
    }

    public void setThrough(String through) {
        this.through = through;
    }

    public String getWeft() {
        return weft;
    }

    public void setWeft(String weft) {
        this.weft = weft;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}

