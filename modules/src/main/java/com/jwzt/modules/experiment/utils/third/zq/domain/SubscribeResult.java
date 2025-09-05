package com.jwzt.modules.experiment.utils.third.zq.domain;

import lombok.Data;
import java.util.List;

@Data
public class SubscribeResult {
    private Data data;
    private int errorCode;
    private List<String> errorMsg;

    public static class Data {
        private List<ResultItem> resultList;
    }

    public static class ResultItem {
        private String buildId;
        private String subscribeId;
    }
}
