package com.ruoyi.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订阅管理对象 SYS_SUBSCRIBE
 * 
 * @author lx
 * @date 2025-09-09
 */
@Data
public class SysSubscribe extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private String ID;

    /** 订阅ID */
    private String subscribeId;

    /** 订阅名称 */
    @Excel(name = "订阅名称")
    private String NAME;

    /** 订阅类型，例如location、event */
    @Excel(name = "订阅类型，例如location、event")
    private String TYPE;

    /** 订阅模式: HTTP/MQ */
    @Excel(name = "订阅模式: HTTP/MQ")
    private String MODE;

    /** HTTP URL 或 MQ topic */
    @Excel(name = "HTTP URL 或 MQ topic")
    private String ENDPOINT;

    /** 状态: ACTIVE/INACTIVE */
    @Excel(name = "状态: ACTIVE/INACTIVE")
    private String STATUS;

    @Excel(name = "来源")
    private String SOURCE;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("ID", getID())
            .append("NAME", getNAME())
            .append("TYPE", getTYPE())
            .append("MODE", getMODE())
            .append("ENDPOINT", getENDPOINT())
            .append("STATUS", getSTATUS())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
