package com.jwzt.modules.experiment.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 行为记录详情对象 tak_behavior_record_detail
 * 
 * @author lx
 * @date 2025-07-09
 */
@Data
public class TakBehaviorRecordDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private String cardId;

    /** 轨迹编号 */
    @Excel(name = "轨迹编号")
    private String trackId;

    /** 点时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "点时间", width = 30)
    private Date recordTime;

    /** 点时间 */
    @Excel(name = "点时间")
    private Long timestampMs;

    /** 经度 */
    @Excel(name = "经度")
    private Double longitude;

    /** 维度 */
    @Excel(name = "维度")
    private Double latitude;
    /** 速度 */
    @Excel(name = "速度")
    private Double speed;

    /** 查询条件：开始时间 */
    private String startTime;

    /** 查询条件：结束时间 */
    private String endTime;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setTrackId(String trackId) 
    {
        this.trackId = trackId;
    }

    public String getTrackId() 
    {
        return trackId;
    }
    public void setRecordTime(Date recordTime) 
    {
        this.recordTime = recordTime;
    }

    public Date getRecordTime() 
    {
        return recordTime;
    }
    public void setTimestampMs(Long timestampMs) 
    {
        this.timestampMs = timestampMs;
    }

    public Long getTimestampMs() 
    {
        return timestampMs;
    }
    public void setLongitude(Double longitude)
    {
        this.longitude = longitude;
    }

    public Double getLongitude()
    {
        return longitude;
    }
    public void setLatitude(Double latitude)
    {
        this.latitude = latitude;
    }

    public Double getLatitude()
    {
        return latitude;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("trackId", getTrackId())
            .append("recordTime", getRecordTime())
            .append("timestampMs", getTimestampMs())
            .append("longitude", getLongitude())
            .append("latitude", getLatitude())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
