package com.jwzt.modules.experiment.utils.third.manage.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 车辆进场、出场推送请求参数
 */
public class VehicleEntryExitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 0 进 1 出
     */
    private Integer type;

    /**
     * 车辆ID
     */
    private String vehicleThirdId;

    /**
     * 识别时间 yyyy-MM-dd HH:mm:ss:SSS
     */
    private String vehicleTime;

    /**
     * 照片
     */
    private List<String> recordFiles;

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
     * 车辆VIN码
     */
    private String vehicleCode;

    /**
     * 车牌
     */
    private String vehicleThirdBrand;

    /**
     * 颜色
     */
    private String vehicleColor;

    /**
     * 品牌
     */
    private String vehicleType;

    /**
     * 车型
     */
    private String vehicleShape;

    /**
     * 装卸类型：1=地跑、2=J车（火车）、4轿运车（板车）
     */
    private Integer regionType;

    /**
     * "transportCar"板车、"car"为非板车
     */
    private String carType;

    public VehicleEntryExitRequest() {
        super();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public List<String> getRecordFiles() {
        return recordFiles;
    }

    public void setRecordFiles(List<String> recordFiles) {
        this.recordFiles = recordFiles;
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

    public String getVehicleCode() {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode) {
        this.vehicleCode = vehicleCode;
    }

    public String getVehicleThirdBrand() {
        return vehicleThirdBrand;
    }

    public void setVehicleThirdBrand(String vehicleThirdBrand) {
        this.vehicleThirdBrand = vehicleThirdBrand;
    }

    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleShape() {
        return vehicleShape;
    }

    public void setVehicleShape(String vehicleShape) {
        this.vehicleShape = vehicleShape;
    }

    public Integer getRegionType() {
        return regionType;
    }

    public void setRegionType(Integer regionType) {
        this.regionType = regionType;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }
}

