package com.jwzt.modules.experiment.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 定位数据记录对象 LOC_TRACK_RECORD
 * 
 * @author lx
 * @date 2025-11-13
 */
public class LocTrackRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private String id;

    /** 基站唯一标识（-1表示无效） */
    @Excel(name = "基站唯一标识", readConverterExp = "-=1表示无效")
    private String stationId;

    /** 定位卡唯一标识 */
    @Excel(name = "定位卡唯一标识")
    private String cardId;

    /** 是否按键报警（0 否，1 是） */
    @Excel(name = "是否按键报警", readConverterExp = "0=,否=，1,是=")
    private String isAlarm;

    /** 定位卡是否低电（0 正常，1 低电） */
    @Excel(name = "定位卡是否低电", readConverterExp = "0=,正=常，1,低=电")
    private String isLowPower;

    /** 定位卡是否静止（0 移动，1 静止） */
    @Excel(name = "定位卡是否静止", readConverterExp = "0=,移=动，1,静=止")
    private String isStill;

    /** 数据来源编码 */
    @Excel(name = "数据来源编码")
    private String sourceCode;

    /** GPS是否有效（0 无效，1 有效） */
    @Excel(name = "GPS是否有效", readConverterExp = "0=,无=效，1,有=效")
    private String gpsIsValid;

    /** 纬度 */
    @Excel(name = "纬度")
    private String latitude;

    /** 经度 */
    @Excel(name = "经度")
    private String longitude;

    /** 速度（单位：m/s） */
    @Excel(name = "速度", readConverterExp = "单=位：m/s")
    private String speed;

    /** 信标数组 JSON 序列化 */
    @Excel(name = "信标数组 JSON 序列化")
    private String beaconsJson;

    /** UWB 基站数组 JSON 序列化 */
    @Excel(name = "UWB 基站数组 JSON 序列化")
    private String uwbsJson;

    /** 原始数据 JSON */
    @Excel(name = "原始数据 JSON")
    private String rawPayload;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String createdAt;

    /**
     * 查询条件：开始时间（用于范围查询）
     */
    private String queryStartTime;

    /**
     * 查询条件：结束时间（用于范围查询）
     */
    private String queryEndTime;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setStationId(String stationId) 
    {
        this.stationId = stationId;
    }

    public String getStationId() 
    {
        return stationId;
    }

    public void setCardId(String cardId) 
    {
        this.cardId = cardId;
    }

    public String getCardId() 
    {
        return cardId;
    }

    public void setIsAlarm(String isAlarm) 
    {
        this.isAlarm = isAlarm;
    }

    public String getIsAlarm() 
    {
        return isAlarm;
    }

    public void setIsLowPower(String isLowPower) 
    {
        this.isLowPower = isLowPower;
    }

    public String getIsLowPower() 
    {
        return isLowPower;
    }

    public void setIsStill(String isStill) 
    {
        this.isStill = isStill;
    }

    public String getIsStill() 
    {
        return isStill;
    }

    public void setSourceCode(String sourceCode) 
    {
        this.sourceCode = sourceCode;
    }

    public String getSourceCode() 
    {
        return sourceCode;
    }

    public void setGpsIsValid(String gpsIsValid) 
    {
        this.gpsIsValid = gpsIsValid;
    }

    public String getGpsIsValid() 
    {
        return gpsIsValid;
    }

    public void setLatitude(String latitude) 
    {
        this.latitude = latitude;
    }

    public String getLatitude() 
    {
        return latitude;
    }

    public void setLongitude(String longitude) 
    {
        this.longitude = longitude;
    }

    public String getLongitude() 
    {
        return longitude;
    }

    public void setSpeed(String speed) 
    {
        this.speed = speed;
    }

    public String getSpeed() 
    {
        return speed;
    }

    public void setBeaconsJson(String beaconsJson) 
    {
        this.beaconsJson = beaconsJson;
    }

    public String getBeaconsJson() 
    {
        return beaconsJson;
    }

    public void setUwbsJson(String uwbsJson) 
    {
        this.uwbsJson = uwbsJson;
    }

    public String getUwbsJson() 
    {
        return uwbsJson;
    }

    public void setRawPayload(String rawPayload) 
    {
        this.rawPayload = rawPayload;
    }

    public String getRawPayload() 
    {
        return rawPayload;
    }

    public void setCreatedAt(String createdAt) 
    {
        this.createdAt = createdAt;
    }

    public String getCreatedAt() 
    {
        return createdAt;
    }

    public String getQueryStartTime() {
        return queryStartTime;
    }

    public void setQueryStartTime(String queryStartTime) {
        this.queryStartTime = queryStartTime;
    }

    public String getQueryEndTime() {
        return queryEndTime;
    }

    public void setQueryEndTime(String queryEndTime) {
        this.queryEndTime = queryEndTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("stationId", getStationId())
            .append("cardId", getCardId())
            .append("isAlarm", getIsAlarm())
            .append("isLowPower", getIsLowPower())
            .append("isStill", getIsStill())
            .append("sourceCode", getSourceCode())
            .append("gpsIsValid", getGpsIsValid())
            .append("latitude", getLatitude())
            .append("longitude", getLongitude())
            .append("speed", getSpeed())
            .append("beaconsJson", getBeaconsJson())
            .append("uwbsJson", getUwbsJson())
            .append("rawPayload", getRawPayload())
            .append("createdAt", getCreatedAt())
            .toString();
    }
}
