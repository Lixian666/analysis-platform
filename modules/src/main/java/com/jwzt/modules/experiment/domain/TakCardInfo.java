package com.jwzt.modules.experiment.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 定位卡信息对象 TAK_CARD_INFO
 *
 * @author lx
 * @date 2025-11-13
 */
public class TakCardInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 卡ID */
    @Excel(name = "卡ID")
    private String cardId;

    /** 货场ID */
    @Excel(name = "货场ID")
    private String yardId;

    /** 货场名称 */
    @Excel(name = "货场名称")
    private String yardName;

    /** 定位卡类型（zq真趣，xrkc新锐科创） */
    @Excel(name = "定位卡类型", readConverterExp = "zq=真趣,xrkc=新锐科创")
    private String type;

    /** 业务类型（0板车 1火车） */
    @Excel(name = "业务类型", readConverterExp = "0=板车,1=火车")
    private Integer bizType;

    /** 是否启用（0启用 1禁用） */
    @Excel(name = "是否启用", readConverterExp = "0=启用,1=禁用")
    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getYardId() {
        return yardId;
    }

    public void setYardId(String yardId) {
        this.yardId = yardId;
    }

    public String getYardName() {
        return yardName;
    }

    public void setYardName(String yardName) {
        this.yardName = yardName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getBizType() {
        return bizType;
    }

    public void setBizType(Integer bizType) {
        this.bizType = bizType;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("cardId", getCardId())
            .append("yardId", getYardId())
            .append("yardName", getYardName())
            .append("type", getType())
            .append("bizType", getBizType())
            .append("enabled", getEnabled())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

