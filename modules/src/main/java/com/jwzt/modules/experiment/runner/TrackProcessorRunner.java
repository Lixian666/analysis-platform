package com.jwzt.modules.experiment.runner;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.DriverTracker;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TrackProcessorRunner implements CommandLineRunner {

    @Autowired
    private DriverTracker driverTracker;

    private static String UUID;
    private static String cardId;

    @Override
    public void run(String... args) throws Exception {

    }
//        String data = FilePathConfig.YUZUI;
////        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
////        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
//        JSONObject jsonObject = JsonUtils.loadJson(file);
//        JSONArray points = jsonObject.getJSONArray("data");
//        List<LocationPoint> LocationPoints = DriverTracker.processWithAnchorData(points, data);
//        // 按卡号分组
//        if (data.equals("minhang")){
//            Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
//                    .collect(Collectors.groupingBy(LocationPoint::getCardId));
//            for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
//                // 取出一个卡号的所有点
//                List<LocationPoint> pointsByCardId = entry.getValue();
//                // 再次根据点位、是否时间一样、是否漂移清洗数据
//                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
//                if (FilterConfig.IS_OUTPUT_SHP){
//                    //清洗过运动或停留数据后生成shp文件
//                    DriverTracker.outputVectorFiles(newPoints,"D:\\work\\output\\finish_clean_points.shp");
//                }
//                DriverTracker tracker = new DriverTracker();
//                // 开始行为分析
////                for (LocationPoint point : newPoints) {
////                    tracker.handleNewRawPoint(tracker, point);
////                }
//            }
//        } else if (data.equals("yuzui")){
//            Map<String, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
//                    .collect(Collectors.groupingBy(LocationPoint::getCardUUID));
//            for (Map.Entry<String, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
//                // 取出一个卡号的所有点
//                List<LocationPoint> pointsByCardId = entry.getValue();
//                // 再次根据点位、是否时间一样、是否漂移清洗数据
//                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
//                if (FilterConfig.IS_OUTPUT_SHP){
//                    //清洗过运动或停留数据后生成shp文件
//                    DriverTracker.outputVectorFiles(newPoints,"D:\\work\\output\\yuzui\\data_clean_points.shp");
//                }
//                DriverTracker tracker = new DriverTracker();
//                cardId = entry.getKey();
//                // 开始行为分析
//                tracker.handleNewRawPoint(newPoints);
////                for (LocationPoint point : newPoints) {
////                    tracker.handleNewRawPoint(tracker, point);
////                }
//
//            }
//        }
////        String data = FilePathConfig.YUZUI;
////        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
////        JSONObject jsonObject = JsonUtils.loadJson(file);
////        JSONArray points = jsonObject.getJSONArray("data");
////        List<LocationPoint> locationPoints = DriverTracker.processWithAnchorData(points, data);
////
////        if (data.equals("yuzui")) {
////            Map<String, List<LocationPoint>> groupedByCardId = locationPoints.stream()
////                    .collect(Collectors.groupingBy(LocationPoint::getCardUUID));
////            for (Map.Entry<String, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
////                List<LocationPoint> pointsByCardId = entry.getValue();
////                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
////                driverTracker.handleNewRawPoint(driverTracker, newPoints);
////            }
////        }
//    }
}
