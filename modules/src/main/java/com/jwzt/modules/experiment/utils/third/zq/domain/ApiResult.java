package com.jwzt.modules.experiment.utils.third.zq.domain;

import lombok.Data;

import java.util.List;

@Data
public class ApiResult {
    private SubscribeResult.DataObj data;
    private int errorCode;
    private List<String> errorMsg;
}
