package com.jwzt.modules.experiment.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class TakBehaviorRecords extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private String cardId;

    /** 货场ID */
    @Excel(name = "货场ID")
    private String yardId;

    /** 轨迹编号（如: zhang001_20250705_1100） */
    @Excel(name = "轨迹编号", readConverterExp = "如=:,z=hang001_20250705_1100")
    private String trackId;

    /** 轨迹起始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "轨迹起始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 轨迹结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "轨迹结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 点数量 */
    @Excel(name = "点数量")
    private Long pointCount;

    /** 行为类型 0 到达卸车 1 发运装车 2 轿运车装车 3 轿运车卸车 4地跑入库 5 地跑出库 */
    @Excel(name = "行为类型 0 到达卸车 1 发运装车 2 轿运车装车 3 轿运车卸车 4地跑入库 5 地跑出库")
    private Long type;

    /** 持续时间 */
    @Excel(name = "持续时间")
    private String duration;

    /** 状态 完成 行驶中 */
    @Excel(name = "状态 完成 行驶中")
    private String state;

    /** 行为记录详情信息 */
    private List<TakBehaviorRecordDetail> takBehaviorRecordDetailList;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setCardId(String cardId) 
    {
        this.cardId = cardId;
    }

    public String getCardId() 
    {
        return cardId;
    }
    public void setYardId(String yardId) 
    {
        this.yardId = yardId;
    }

    public String getYardId() 
    {
        return yardId;
    }
    public void setTrackId(String trackId) 
    {
        this.trackId = trackId;
    }

    public String getTrackId() 
    {
        return trackId;
    }
    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }
    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }
    public void setPointCount(Long pointCount) 
    {
        this.pointCount = pointCount;
    }

    public Long getPointCount() 
    {
        return pointCount;
    }
    public void setType(Long type) 
    {
        this.type = type;
    }

    public Long getType() 
    {
        return type;
    }
    public void setDuration(String duration) 
    {
        this.duration = duration;
    }

    public String getDuration() 
    {
        return duration;
    }
    public void setState(String state) 
    {
        this.state = state;
    }

    public String getState() 
    {
        return state;
    }

    public List<TakBehaviorRecordDetail> getTakBehaviorRecordDetailList()
    {
        return takBehaviorRecordDetailList;
    }

    public void setTakBehaviorRecordDetailList(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList)
    {
        this.takBehaviorRecordDetailList = takBehaviorRecordDetailList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("cardId", getCardId())
            .append("yardId", getYardId())
            .append("trackId", getTrackId())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("pointCount", getPointCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("type", getType())
            .append("duration", getDuration())
            .append("state", getState())
            .append("takBehaviorRecordDetailList", getTakBehaviorRecordDetailList())
            .toString();
    }
}
