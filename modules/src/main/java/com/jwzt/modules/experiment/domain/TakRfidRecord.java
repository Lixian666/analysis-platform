package com.jwzt.modules.experiment.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * RFID识别记录对象 tak_rfid_record
 * 
 * @author lx
 * @date 2025-01-20
 */
public class TakRfidRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** RFID车辆认证码 */
    @Excel(name = "RFID车辆认证码")
    private String recordCode;

    /** 识别时间 */
    @Excel(name = "识别时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss:SSS")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private Date vehicleTime;

    /** RFID识别设备 */
    @Excel(name = "RFID识别设备")
    private String regionId;

    /** 司机名称 */
    @Excel(name = "司机名称")
    private String driver;

    /** 操作站id */
    @Excel(name = "操作站id")
    private String operateStationId;

    /** 操作站 */
    @Excel(name = "操作站")
    private String operateStation;

    /** RFID */
    @Excel(name = "RFID")
    private String rfid;

    /** 第三方主键ID */
    @Excel(name = "第三方主键ID")
    private String thirdId;

    /** 匹配状态：0-未匹配，1-匹配成功，2-多余数据，3-重复数据 */
    @Excel(name = "匹配状态", readConverterExp = "0=未匹配,1=匹配成功,2=多余数据,3=重复数据")
    private Integer matchStatus;

    /** 匹配的作业数据ID */
    private Long matchedJobId;

    /** 匹配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public Date getVehicleTime() {
        return vehicleTime;
    }

    public void setVehicleTime(Date vehicleTime) {
        this.vehicleTime = vehicleTime;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
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

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getThirdId() {
        return thirdId;
    }

    public void setThirdId(String thirdId) {
        this.thirdId = thirdId;
    }

    public Integer getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(Integer matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Long getMatchedJobId() {
        return matchedJobId;
    }

    public void setMatchedJobId(Long matchedJobId) {
        this.matchedJobId = matchedJobId;
    }

    public Date getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Date matchTime) {
        this.matchTime = matchTime;
    }
}

