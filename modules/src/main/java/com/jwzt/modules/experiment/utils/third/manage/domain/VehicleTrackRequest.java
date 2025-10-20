package com.jwzt.modules.experiment.utils.third.manage.domain;

import java.io.Serializable;

/**
 * 实时轨迹请求参数
 */
public class VehicleTrackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生产车辆ID
     */
    private String vehicleThirdId;

    /**
     * 时间 yyyy-MM-dd HH:mm:ss:SSS
     */
    private String vehicleTime;

    /**
     * 摄像机ID（定位设备）
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
     * "transportCar"板车、"car"为非板车
     */
    private String carType;

    public VehicleTrackRequest() {
        super();
    }

    public String getVehicleThirdId() {
        return vehicleThirdId;
    }

    public void setVehicleThirdId(String vehicleThirdId) {
        this.vehicleThirdId = vehicleThirdId;
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

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }
}

