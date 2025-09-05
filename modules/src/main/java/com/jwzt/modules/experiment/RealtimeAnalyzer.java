package com.jwzt.modules.experiment;

import com.jwzt.modules.experiment.domain.LocationPoint;

import java.util.List;

public interface RealtimeAnalyzer {
    // 单点增量：接收一个新点（或迟到点）
    void accept(LocationPoint point);

    // 批量回放：可直接复用现有逻辑
    default void acceptAll(List<LocationPoint> points) {
        points.forEach(this::accept);
    }

    // 获取当前 card 的聚合状态/快照（调试或监控）
//    AnalyzerSnapshot snapshot();
}
