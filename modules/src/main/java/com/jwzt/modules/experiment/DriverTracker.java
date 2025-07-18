package com.jwzt.modules.experiment;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.*;
import com.jwzt.modules.experiment.filter.LocationSmoother;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordsService;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.GeoUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.geo.ShapefileWriter;
import com.jwzt.modules.experiment.vo.EventState;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class DriverTracker {
    OutlierFilter outlierFilter = new OutlierFilter();
    LocationSmoother smoother = new LocationSmoother();

    @Autowired
    private ITakBehaviorRecordsService iTakBehaviorRecordsService;
    @Autowired
    private ITakBehaviorRecordDetailService iTakBehaviorRecordDetailService;


    private Deque<LocationPoint> window = new ArrayDeque<>();
    private List<LocationPoint> recordPoints = new ArrayList<>();
    private Deque<MovementAnalyzer.MovementState> states = new ArrayDeque<>();
    private int windowSize = FilterConfig.WINDOW_STATE_SIZE;
    private int recordPointsSize = FilterConfig.RECORD_POINTS_SIZE;

    private Long boardingTime = null;
    private Long droppingTime = null;

    private static String UUID;
    public static String cardId;

    private int boarding_idx = 0;
    private int dropping_idx = 0;

    private BoardingDetector detector = new BoardingDetector();


    public void onNewLocation(List<LocationPoint> points) {
        for (int i = 0; i <= points.size() - recordPointsSize; i++) {
            List<LocationPoint> recordPoints = points.subList(i, i + recordPointsSize);
            EventState eventState = detector.updateState(recordPoints);
            switch (eventState.getEvent()) {
                case ARRIVED_BOARDING:
                    if (boardingTime == null){
                        boarding_idx = i + (recordPointsSize / 2);
                        boardingTime = eventState.getTimestamp();
                        UUID = IdUtils.fastSimpleUUID();
                    }
                    System.out.println("📥 检测到到达上车事件");
                    break;
                case ARRIVED_DROPPING:
                    if (boardingTime != null){
                        List<TakBehaviorRecordDetail> takBehaviorRecordDetailList = new ArrayList<TakBehaviorRecordDetail>();
                        List<LocationPoint> subPoints = points.subList(boarding_idx,  i + (recordPointsSize / 2));
                        for (LocationPoint point : subPoints){
                            TakBehaviorRecordDetail takBehaviorRecordDetail = new TakBehaviorRecordDetail();
                            takBehaviorRecordDetail.setTrackId(UUID);
                            takBehaviorRecordDetail.setRecordTime(new Date(point.getTimestamp()));
                            takBehaviorRecordDetail.setTimestampMs(point.getTimestamp());
                            takBehaviorRecordDetail.setLongitude(point.getLongitude());
                            takBehaviorRecordDetail.setLatitude(point.getLatitude());
                            takBehaviorRecordDetailList.add(takBehaviorRecordDetail);
                        }
                        TakBehaviorRecords takBehaviorRecords = new TakBehaviorRecords();
                        takBehaviorRecords.setCardId(cardId);
                        takBehaviorRecords.setYardId("YUZUI");
                        takBehaviorRecords.setTrackId(UUID);
                        takBehaviorRecords.setStartTime(new Date(boardingTime));
                        takBehaviorRecords.setEndTime(new Date(eventState.getTimestamp()));
                        takBehaviorRecords.setPointCount((long) subPoints.size());
                        takBehaviorRecords.setType(0L);
                        takBehaviorRecords.setDuration(DateTimeUtils.calculateTimeDifference(boardingTime, eventState.getTimestamp()));
                        takBehaviorRecords.setState("完成");
                        takBehaviorRecords.setTakBehaviorRecordDetailList(takBehaviorRecordDetailList);
                        iTakBehaviorRecordsService.insertTakBehaviorRecords(takBehaviorRecords);
                        iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetailAll(takBehaviorRecordDetailList);
//                        for (TakBehaviorRecordDetail takBehaviorRecordDetail : takBehaviorRecordDetailList){
//                            iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetail(takBehaviorRecordDetail);
//                        }
                    }
                    boardingTime = null;
                    System.out.println("📤 检测到到达下车事件");
                    break;
                case SEND_BOARDING:
                    if (boardingTime == null){
                        boarding_idx = i + (recordPointsSize / 2);
                        boardingTime = eventState.getTimestamp();
                        UUID = IdUtils.fastSimpleUUID();
                    }
                    System.out.println("📥 检测到发运上车事件");
                    break;
                case SEND_DROPPING:
                    if (boardingTime != null){
                        List<TakBehaviorRecordDetail> takBehaviorRecordDetailList = new ArrayList<TakBehaviorRecordDetail>();
                        List<LocationPoint> subPoints = points.subList(boarding_idx,  i + (recordPointsSize / 2));
                        for (LocationPoint point : subPoints){
                            TakBehaviorRecordDetail takBehaviorRecordDetail = new TakBehaviorRecordDetail();
                            takBehaviorRecordDetail.setCardId(cardId);
                            takBehaviorRecordDetail.setTrackId(UUID);
                            takBehaviorRecordDetail.setRecordTime(new Date(point.getTimestamp()));
                            takBehaviorRecordDetail.setTimestampMs(point.getTimestamp());
                            takBehaviorRecordDetail.setLongitude(point.getLongitude());
                            takBehaviorRecordDetail.setLatitude(point.getLatitude());
                            takBehaviorRecordDetailList.add(takBehaviorRecordDetail);
                        }
                        TakBehaviorRecords takBehaviorRecords = new TakBehaviorRecords();
                        takBehaviorRecords.setCardId(cardId);
                        takBehaviorRecords.setYardId("YUZUI");
                        takBehaviorRecords.setTrackId(UUID);
                        takBehaviorRecords.setStartTime(new Date(boardingTime));
                        takBehaviorRecords.setEndTime(new Date(eventState.getTimestamp()));
                        takBehaviorRecords.setPointCount((long) subPoints.size());
                        takBehaviorRecords.setType(1L);
                        takBehaviorRecords.setDuration(DateTimeUtils.calculateTimeDifference(boardingTime, eventState.getTimestamp()));
                        takBehaviorRecords.setState("完成");
                        takBehaviorRecords.setTakBehaviorRecordDetailList(takBehaviorRecordDetailList);
                        iTakBehaviorRecordsService.insertTakBehaviorRecords(takBehaviorRecords);
                        iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetailAll(takBehaviorRecordDetailList);
//                        for (TakBehaviorRecordDetail takBehaviorRecordDetail : takBehaviorRecordDetailList){
//                            iTakBehaviorRecordDetailService.insertTakBehaviorRecordDetail(takBehaviorRecordDetail);
//                        }
                    }
                    boardingTime = null;
                    System.out.println("📤 检测到发运下车事件");
                    break;
            }
        }
        System.out.println("📤 检测完成");
    }
    public void handleNewRawPoint(List<LocationPoint> points) {
        iTakBehaviorRecordsService.deleteByCreationTime("2025-07-11 16:00:00");
        iTakBehaviorRecordDetailService.deleteByCreationTime("2025-07-11 16:00:00");
        List<LocationPoint> normalPoints = new ArrayList<>();
        for (LocationPoint rawPoint : points){
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
            }else {
                // 正常点
                normalPoints.add(rawPoint);
            }
        }
        // 添加运动状态
        List<LocationPoint> newPoints = outlierFilter.stateAnalysis(normalPoints);
        if (FilterConfig.IS_OUTPUT_SHP){
            // 生成空间异常、时间间隔异常、速度异常过滤后的shp文件
            outputVectorFiles(newPoints,"D:\\work\\output\\yuzui\\final_points.shp");
        }
        // 行为分析
        this.onNewLocation(newPoints);
    }


    public static void outputVectorFiles(List<LocationPoint> LocationPoints, String shpFilePath) {
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
        if (data.equals("rtk")){
            for (int i = 0; i < points.size(); i++) {
                LocationPoint point = points.getObject(i, LocationPoint.class);
                point.setTimestamp(DateTimeUtils.convertToTimestamp(point.getAcceptTime()));
                LocationPoints.add(point);
            }
        } else if (data.equals("other")){
            for (int i = 0; i < points.size(); i++) {
                try {
                    if (i==10777){
                        System.out.println(i);
                    }
                    LocationPoint1 point = points.getObject(i, LocationPoint1.class);
                    LocationPoint locationPoint = new LocationPoint();
                    locationPoint.setCardUUID(point.getRecordThirdId());
                    locationPoint.setLongitude(point.getThrough());
                    locationPoint.setLatitude(point.getWeft());
                    String dateTimeStr = fixLeadingZero(point.getRecordTime());
                    int lastColonIndex = dateTimeStr.lastIndexOf(':');
                    if (lastColonIndex != -1) {
                        dateTimeStr = dateTimeStr.substring(0, lastColonIndex) + "." + dateTimeStr.substring(lastColonIndex + 1);
                    }
                    locationPoint.setAcceptTime(dateTimeStr);
                    locationPoint.setTimestamp(point.getRecordTimeLength());
                    LocationPoints.add(locationPoint);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (FilterConfig.IS_OUTPUT_SHP){
            // 生成原始shp文件
            outputVectorFiles(LocationPoints,"D:\\work\\output\\yuzui\\origin_points.shp");
            // 生成同时间间点清洗数据shp文件
            List<LocationPoint> shpPoints = GeoUtils.processMultiplePointsPerSecond(LocationPoints);
            outputVectorFiles(shpPoints,"D:\\work\\output\\yuzui\\time_clean_points.shp");
        }
        return LocationPoints;

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
//                    outputVectorFiles(newPoints,"D:\\work\\output\\finish_clean_points.shp");
//                }
//                return newPoints;
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
//                    outputVectorFiles(newPoints,"D:\\work\\output\\yuzui\\data_clean_points.shp");
//                }
//                return newPoints;
//            }
//        }
//        return null;
    }

    public static String fixLeadingZero(String timeStr) {
        if (timeStr != null && timeStr.matches("^0\\d{4}-.*")) {
            return timeStr.replaceFirst("^0", "");
        }
        return timeStr;
    }

    public static void main(String[] args) {
        String data = FilePathConfig.YUZUI;
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
        JSONObject jsonObject = JsonUtils.loadJson(file);
        JSONArray points = jsonObject.getJSONArray("data");
        List<LocationPoint> LocationPoints = processWithAnchorData(points, data);
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
                    outputVectorFiles(newPoints,"D:\\work\\output\\yuzui\\finish_clean_points.shp");
                }
                DriverTracker tracker = new DriverTracker();
                // 开始行为分析
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }
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
                DriverTracker tracker = new DriverTracker();
                cardId = entry.getKey();
                // 开始行为分析
                tracker.handleNewRawPoint(newPoints);
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }

            }
        }
    }
}
