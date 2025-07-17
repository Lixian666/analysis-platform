package com.jwzt.modules.experiment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.util.Date;

@Data
public class TakBehaviorRecordDetailVo {

    /** 货场ID */
    @Excel(name = "货场ID")
    private String yardId;

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

    /** ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private String cardId;

    /** 轨迹编号 */
    @Excel(name = "轨迹编号")
    private String trackId;

    /** 点时间 */
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
}
