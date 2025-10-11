package com.jwzt.modules.experiment.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 信标信息对象 TAK_BEACON_INFO
 * 
 * @author lx
 * @date 2025-09-16
 */
public class TakBeaconInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private String id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 类型 */
    @Excel(name = "类型")
    private String type;

    /** 区域 */
    @Excel(name = "区域")
    private String area;

    /** 建筑名称 */
    @Excel(name = "建筑NAME")
    private String buildName;

    /** 建筑ID */
    @Excel(name = "建筑ID")
    private String buildId;

    /** 信标ID */
    @Excel(name = "信标ID")
    private String beaconId;

    /** 位置 */
    @Excel(name = "位置")
    private String location;

    /** 状态，0-正常，1-异常 */
    @Excel(name = "状态，0-正常，1-异常")
    private Integer status;

    public TakBeaconInfo(String buildId, String type, String location, String area) {
        super();
    }

    public TakBeaconInfo() {

    }

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }
    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }
    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }
    public void setBuildName(String buildName) { this.buildName = buildName; }

    public String getBuildName()
    {
        return buildName;
    }
    public void setBuildId(String buildId) { this.buildId = buildId; }

    public String getBuildId()
    {
        return buildId;
    }
    public void setArea(String area)
    {
        this.area = area;
    }

    public String getArea()
    {
        return area;
    }
    public void setBeaconId(String beaconId) 
    {
        this.beaconId = beaconId;
    }

    public String getBeaconId() 
    {
        return beaconId;
    }
    public void setLocation(String location) 
    {
        this.location = location;
    }

    public String getLocation() 
    {
        return location;
    }
    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("type", getType())
            .append("area", getArea())
            .append("buildName", getBuildName())
            .append("buildId", getBuildId())
            .append("beaconId", getBeaconId())
            .append("location", getLocation())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
