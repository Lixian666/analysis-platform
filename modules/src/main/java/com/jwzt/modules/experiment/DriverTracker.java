package com.jwzt.modules.experiment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.MovementAnalyzer;
import com.jwzt.modules.experiment.filter.LocationSmoother;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.geo.ShapefileWriter;


import java.util.*;
import java.util.stream.Collectors;

public class DriverTracker {
    OutlierFilter outlierFilter = new OutlierFilter();
    LocationSmoother smoother = new LocationSmoother();

    private Deque<LocationPoint> window = new ArrayDeque<>();
    private Deque<MovementAnalyzer.MovementState> states = new ArrayDeque<>();
    private int windowSize = FilterConfig.WINDOW_STATE_SIZE;

    private BoardingDetector detector = new BoardingDetector();

    public void onNewLocation(LocationPoint point) {
        window.addLast(point);
        if (window.size() > windowSize) {
            window.removeFirst();
        }
        // 通过windowSize个点判断当前运动状态
        MovementAnalyzer.MovementState state = MovementAnalyzer.analyzeState(new ArrayList<>(window));
        BoardingDetector.Event event = detector.updateState(new ArrayList<>(window), state);
//        states.addLast(state);
//        BoardingDetector.Event event = detector.updateState(new ArrayList<>(window), new ArrayList<>(states));
//        if (states.size() >= 5){
//            states.clear();
//        }

        if (state == MovementAnalyzer.MovementState.DRIVING) {
            System.out.println("🚗 当前正在驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (state == MovementAnalyzer.MovementState.LOW_DRIVING) {
            System.out.println("🚗🐢 当前正在低速驾驶，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (state == MovementAnalyzer.MovementState.WALKING) {
            System.out.println("🚶 当前在步行，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else if (state == MovementAnalyzer.MovementState.RUNNING) {
            System.out.println("🏃 当前在小跑，时间为：" + point.getAcceptTime() + "速度为：" + point.getSpeed() + "m/s");
        } else {
            System.out.println("⛔ 当前静止，时间为：" + point.getAcceptTime());
        }

        switch (event) {
            case ARRIVED_BOARDING:
                System.out.println("📥 检测到上车事件");
                break;
            case ARRIVED_DROPPING:
                System.out.println("📤 检测到下车事件");
                break;
        }
    }
    public void handleNewRawPoint(DriverTracker tracker, LocationPoint rawPoint) {
        int state = outlierFilter.isValid(rawPoint);
        if (!(state == 0)) {
            if (state == 1){
                System.out.println("⚠️  时间间隔异常定位点已剔除：" + rawPoint);
            } else if (state == 2) {
                System.out.println("⚠️  速度异常定位点已剔除：" + rawPoint);
            } else if (state == 3) {
                System.out.println("⚠️  定位异常定位点已剔除：" + rawPoint);
            } else {
                System.out.println("⚠️  异常定位点已剔除：" + rawPoint);
            }
            return;
        }

//        LocationPoint smoothed = smoother.smooth(rawPoint);
        tracker.onNewLocation(rawPoint);
    }

    public static void main(String[] args) {
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
        JSONObject jsonObject = JsonUtils.loadJson(file);
        JSONArray points = jsonObject.getJSONArray("data");
        List<LocationPoint> LocationPoints = new ArrayList<>();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < points.size(); i++){
            LocationPoint point = points.getObject(i, LocationPoint.class);
            coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude(), DateTimeUtils.convertToTimestamp(point.getAcceptTime())));
        }
//        // 写入shp文件 输出坐标点图层
//        String shpFilePath = "D:\\work\\output\\points.shp";
//        ShapefileWriter.writeCoordinatesToShapefile(coordinates, shpFilePath);
        for (int i = 0; i < points.size(); i++) {
            LocationPoint point = points.getObject(i, LocationPoint.class);
            point.setTimestamp(DateTimeUtils.convertToTimestamp(point.getAcceptTime()));
            LocationPoints.add(point);
        }
        DriverTracker tracker = new DriverTracker();
        Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                .collect(Collectors.groupingBy(LocationPoint::getCardId));
        for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
            List<LocationPoint> pointsByCardId = entry.getValue();
            System.out.println("Card ID: " + entry.getKey());
            List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
            for (LocationPoint point : newPoints) {
                tracker.handleNewRawPoint(tracker, point);
            }
        }
    }
}
