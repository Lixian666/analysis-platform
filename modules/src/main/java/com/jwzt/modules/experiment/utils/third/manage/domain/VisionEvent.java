package com.jwzt.modules.experiment.utils.third.manage.domain;

import java.io.Serializable;

/**
 * Vision事件响应数据
 */
public class VisionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private Long id;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件时间
     */
    private String eventTime;

    /**
     * 摄像机ID
     */
    private String cameraId;

    /**
     * 摄像机IP
     */
    private String cameraIp;

    /**
     * 车辆ID
     */
    private String carId;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 维度
     */
    private Double latitude;

    /**
     * 图片URL
     */
    private String pictureUrl;

    /**
     * 商品车辆数量
     */
    private Integer commodityVehicleCount;

    /**
     * 车牌号
     */
    private String carNumber;

    /**
     * vin码
     */
    private String vin;

    /**
     * 创建时间
     */
    private String createDate;

    /**
     * 更新时间
     */
    private String updateDate;

    /**
     * 匹配标识
     * 0：已匹配 1：未匹配
     */
    private Integer matched;

    public VisionEvent() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraIp() {
        return cameraIp;
    }

    public void setCameraIp(String cameraIp) {
        this.cameraIp = cameraIp;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public Double getLongitude() { return longitude; }

    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public Integer getCommodityVehicleCount() {
        return commodityVehicleCount;
    }

    public void setCommodityVehicleCount(Integer commodityVehicleCount) {
        this.commodityVehicleCount = commodityVehicleCount;
    }

    public String getCarNumber() {
        return carNumber;
    }

    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
    }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getMatched() { return matched; }
    public void setMatched(Integer matched) { this.matched = matched; }
}

