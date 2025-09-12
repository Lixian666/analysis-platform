package com.jwzt.modules.experiment.utils.third.zq.domain;

import lombok.Data;
import java.util.List;

@Data
public class SubscribeResult {
    private DataObj data;
    private int errorCode;
    private List<String> errorMsg;

    @Data
    public static class DataObj  {
        private List<ResultItem> resultList;
    }

    @Data
    public static class ResultItem {
        private String buildId;
        private String subscribeId;
    }
}
