package com.jwzt.modules.experiment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.*;
import com.jwzt.modules.experiment.filter.LocationSmoother;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.geo.ShapefileWriter;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;


import java.util.*;
import java.util.stream.Collectors;

public class DriverTracker {
    OutlierFilter outlierFilter = new OutlierFilter();
    LocationSmoother smoother = new LocationSmoother();

    private Deque<LocationPoint> window = new ArrayDeque<>();
    private List<LocationPoint> recordPoints = new ArrayList<>();
    private Deque<MovementAnalyzer.MovementState> states = new ArrayDeque<>();
    private int windowSize = FilterConfig.WINDOW_STATE_SIZE;
    private int recordPointsSize = FilterConfig.RECORD_POINTS_SIZE;

    private BoardingDetector detector = new BoardingDetector();

    public void onNewLocation(LocationPoint point) {
        window.addLast(point);
        if (window.size() > windowSize) {
            window.removeFirst();
        }
        // 通过windowSize个点判断当前运动状态
        MovementAnalyzer.MovementState state = MovementAnalyzer.analyzeState(new ArrayList<>(window));
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

        point.setState(state);
        recordPoints.add(point);
        if (recordPoints.size() > recordPointsSize){
            recordPoints.remove(0);
        }
        BoardingDetector.Event event = detector.updateState(recordPoints);
//        BoardingDetector.Event event = detector.updateState(new ArrayList<>(window), state);
//        states.addLast(state);
//        BoardingDetector.Event event = detector.updateState(new ArrayList<>(window), new ArrayList<>(states));
//        if (states.size() >= 5){
//            states.clear();
//        }

        switch (event) {
            case ARRIVED_BOARDING:
                System.out.println("📥 检测到到达上车事件");
                break;
            case ARRIVED_DROPPING:
                System.out.println("📤 检测到到达下车事件");
                break;
            case SEND_BOARDING:
                System.out.println("📥 检测到发运上车事件");
                break;
            case SEND_DROPPING:
                System.out.println("📤 检测到发运下车事件");
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


    private static void outputVectorFiles(List<LocationPoint> LocationPoints, String shpFilePath) {
        // 生成原始shp文件
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < LocationPoints.size(); i++){
            LocationPoint point = LocationPoints.get(i);
            coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude(), DateTimeUtils.convertToTimestamp(point.getAcceptTime())));
        }
        // 写入shp文件 输出坐标点图层
        ShapefileWriter.writeCoordinatesToShapefile(coordinates, shpFilePath);
    }

    public static List<LocationPoint> processWithAnchorData(JSONArray points, String data) {
        List<LocationPoint> LocationPoints = new ArrayList<>();
        if (data.equals("minhang")){
            for (int i = 0; i < points.size(); i++) {
                LocationPoint point = points.getObject(i, LocationPoint.class);
                point.setTimestamp(DateTimeUtils.convertToTimestamp(point.getAcceptTime()));
                LocationPoints.add(point);
            }
        } else if (data.equals("yuzui")){
            for (int i = 0; i < points.size(); i++) {
                LocationPoint1 point = points.getObject(i, LocationPoint1.class);
                LocationPoint locationPoint = new LocationPoint();
                locationPoint.setCardUUID(point.getRecordThirdId());
                locationPoint.setLongitude(point.getThrough());
                locationPoint.setLatitude(point.getWeft());
                String dateTimeStr = point.getRecordTime();
                int lastColonIndex = dateTimeStr.lastIndexOf(':');
                if (lastColonIndex != -1) {
                    dateTimeStr = dateTimeStr.substring(0, lastColonIndex) + "." + dateTimeStr.substring(lastColonIndex + 1);
                }
                locationPoint.setAcceptTime(dateTimeStr);
                locationPoint.setTimestamp(point.getRecordTimeLength());
                LocationPoints.add(locationPoint);
            }
        }

        if (FilterConfig.IS_OUTPUT_SHP){
            // 生成原始shp文件
            outputVectorFiles(LocationPoints,"D:\\work\\output\\yuzui\\origin_points.shp");
            // 生成同时间间点清洗数据shp文件
            List<LocationPoint> shpPoints = GeoUtils.processMultiplePointsPerSecond(LocationPoints);
            outputVectorFiles(shpPoints,"D:\\work\\output\\time_clean_points.shp");
        }

        // 按卡号分组
        if (data.equals("minhang")){
            Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardId));
            for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (FilterConfig.IS_OUTPUT_SHP){
                    //清洗过运动或停留数据后生成shp文件
                    outputVectorFiles(newPoints,"D:\\work\\output\\finish_clean_points.shp");
                }
                return newPoints;
            }
        } else if (data.equals("yuzui")){
            Map<String, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardUUID));
            for (Map.Entry<String, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (FilterConfig.IS_OUTPUT_SHP){
                    //清洗过运动或停留数据后生成shp文件
                    outputVectorFiles(newPoints,"D:\\work\\output\\yuzui\\data_clean_points.shp");
                }
                return newPoints;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String data = FilePathConfig.YUZUI;
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
        JSONObject jsonObject = JsonUtils.loadJson(file);
        JSONArray points = jsonObject.getJSONArray("data");
        List<LocationPoint> newPoints = processWithAnchorData(points, data);
        if (newPoints == null){
            return;
        }
        DriverTracker tracker = new DriverTracker();
        // 开始行为分析
        for (LocationPoint point : newPoints) {
            tracker.handleNewRawPoint(tracker, point);
        }
    }
}
