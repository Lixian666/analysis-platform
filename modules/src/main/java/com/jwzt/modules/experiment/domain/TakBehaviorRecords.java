package com.jwzt.modules.experiment.domain;

import java.util.List;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 行为记录对象 tak_behavior_records
 *
 * @author lx
 * @date 2025-07-10
 */
public class TakBehaviorRecords extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @Excel(name = "用户ID")
    private String cardId;

    /**
     * 货场ID
     */
    @Excel(name = "货场ID")
    private String yardId;

    /**
     * 轨迹编号（如: zhang001_20250705_1100）
     */
    @Excel(name = "轨迹编号", readConverterExp = "如=:,z=hang001_20250705_1100")
    private String trackId;

    /**
     * 作业任务数
     */
    private int taskCount;

    /**
     * 作业任务最后时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date taskLastTime;
    /**
     * 轨迹起始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 轨迹结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 上/下车识别时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date identifyTime;

    /**
     * 点数量
     */
    @Excel(name = "点数量")
    private Long pointCount;

    /**
     * 行为类型 0 到达卸车 1 发运装车 2 轿运车装车 3 轿运车卸车 4地跑入库 5 地跑出库
     */
    @Excel(name = "行为类型 0 到达卸车 1 发运装车 2 轿运车卸车 3 轿运车装车 4地跑入库 5 地跑出库")
    private Long type;

    /**
     * 持续时间
     */
    @Excel(name = "持续时间")
    private String duration;

    /**
     * 状态 完成 行驶中
     */
    @Excel(name = "状态 完成 行驶中")
    private String state;

    /**
     * 匹配状态：0-未匹配，1-匹配成功，2-作业数据多余（没有RFID匹配）
     */
    @Excel(name = "匹配状态", readConverterExp = "0=未匹配,1=匹配成功,2=作业数据多余")
    private Integer matchStatus;

    /**
     * 匹配的RFID记录ID
     */
    private Long matchedRfidId;

    /**
     * 匹配时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date matchTime;

    /**
     * 行为记录详情信息
     */
    private List<TakBehaviorRecordDetail> takBehaviorRecordDetailList;

    /**
     * 查询条件：时间类型 0：开始时间 1：结束时间 null 表示查询所有
     */
    private Integer queryTimeType;

    /**
     * 查询条件：开始时间（用于范围查询）
     */
    private String queryStartTime;

    /**
     * 查询条件：结束时间（用于范围查询）
     */
    private String queryEndTime;

    /** 会话最常命中的信标名称 */
    private String beaconName;
    /** 会话最常命中的信标RFID名称 */
    private String rfidName;
    /** 会话最常命中的信标区域 */
    private String area;

    /** 视觉匹配ID */
    private Long visionId;
    /** VIN码 */
    private String vehicleCode;

    /** 推送状态：0-已推送，1-未推送 */
    private Integer pushStatus;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setYardId(String yardId) {
        this.yardId = yardId;
    }

    public String getYardId() {
        return yardId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public Date getTaskLastTime() {
        return taskLastTime;
    }

    public void setTaskLastTime(Date taskLastTime) {
        this.taskLastTime = taskLastTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setIdentifyTime(Date identifyTime) { this.identifyTime = identifyTime; }

    public Date getIdentifyTime() { return identifyTime; }

    public void setPointCount(Long pointCount) {
        this.pointCount = pointCount;
    }

    public Long getPointCount() {
        return pointCount;
    }

    public void setType(Long type) {
        this.type = type;
    }

    public Long getType() {
        return type;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDuration() {
        return duration;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public Integer getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(Integer matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Long getMatchedRfidId() {
        return matchedRfidId;
    }

    public void setMatchedRfidId(Long matchedRfidId) {
        this.matchedRfidId = matchedRfidId;
    }

    public Date getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Date matchTime) {
        this.matchTime = matchTime;
    }

    public List<TakBehaviorRecordDetail> getTakBehaviorRecordDetailList() {
        return takBehaviorRecordDetailList;
    }

    public void setTakBehaviorRecordDetailList(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList) {
        this.takBehaviorRecordDetailList = takBehaviorRecordDetailList;
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

    public Integer getQueryTimeType() {
        return queryTimeType;
    }

    public void setQueryTimeType(Integer queryTimeType) {
        this.queryTimeType = queryTimeType;
    }

    public String getBeaconName() { return beaconName; }
    public void setBeaconName(String beaconName) { this.beaconName = beaconName; }
    public String getRfidName() { return rfidName; }
    public void setRfidName(String rfidName) { this.rfidName = rfidName; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public Long getVisionId() { return visionId; }
    public void setVisionId(Long visionId) { this.visionId = visionId; }

    public String getVehicleCode() { return vehicleCode; }
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    public Integer getPushStatus() { return pushStatus; }
    public void setPushStatus(Integer pushStatus) { this.pushStatus = pushStatus; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("id", getId()).append("cardId", getCardId()).append("yardId", getYardId()).append("trackId", getTrackId()).append("startTime", getStartTime()).append("endTime", getEndTime()).append("identifyTime", getIdentifyTime()).append("pointCount", getPointCount()).append("createTime", getCreateTime()).append("updateTime", getUpdateTime()).append("type", getType()).append("duration", getDuration()).append("state", getState()).append("takBehaviorRecordDetailList", getTakBehaviorRecordDetailList()).toString();
    }
}
