package com.jwzt.modules.experiment.utils.third.manage.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 仓储入场推送车辆RFID
 *
 */
public class ReqVehicleCode implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * RFID车辆认证码
     */
    private String recordCode;

    /**
     * 识别时间 yyyy-MM-dd HH:mm:ss:SSS
     */
    private String vehicleTime;
    /**
     * RFID识别设备
     */
    private String regionId;
    /**
     * Redis缓存消息处理队列次数
     * 超过5次后进场备份集
     */
    private Integer redisSqlNum;
    /**
     * 司机名称
     */
    private String driver;
    /**
     * 操作站id
     */
    private String operateStationId;
    /**
     * 操作站
     */
    private String operateStation;
    /**
     * rfid
     */
    private String rfid;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 更新时间
     */
    private String updateTimeStr;
    /**
     * 第三方主键ID
     */
    private String thirdId;

    public String getUpdateTimeStr() {
        return updateTimeStr;
    }

    public void setUpdateTimeStr(String updateTimeStr) {
        this.updateTimeStr = updateTimeStr;
    }

    public String getThirdId() {
        return thirdId;
    }

    public void setThirdId(String thirdId) {
        this.thirdId = thirdId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getOperateStationId() {
        return operateStationId;
    }

    public void setOperateStationId(String operateStationId) {
        this.operateStationId = operateStationId;
    }

    public String getOperateStation() {
        return operateStation;
    }

    public void setOperateStation(String operateStation) {
        this.operateStation = operateStation;
    }

    public Integer getRedisSqlNum() {
        return redisSqlNum;
    }

    public void setRedisSqlNum(Integer redisSqlNum) {
        this.redisSqlNum = redisSqlNum;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getVehicleTime() {
        return vehicleTime;
    }

    public void setVehicleTime(String vehicleTime) {
        this.vehicleTime = vehicleTime;
    }

    public ReqVehicleCode() {
        super();
    }

}
