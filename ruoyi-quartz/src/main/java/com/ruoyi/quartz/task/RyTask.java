package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.DriverTracker;
import com.jwzt.modules.experiment.RealTimeDriverTracker;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.BoardingDetector;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.third.manage.CenterWorkHttpUtils;
import com.jwzt.modules.experiment.utils.third.manage.domain.ReqVehicleCode;
import com.jwzt.modules.experiment.utils.third.zq.FusionData;
import com.jwzt.modules.experiment.utils.third.zq.ZQOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import javax.annotation.Resource;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private DriverTracker tracker;

    @Autowired
    private ZQOpenApi zqOpenApi;

    @Autowired
    private CenterWorkHttpUtils centerWorkHttpUtils;

    @Autowired
    private RealTimeDriverTracker realTimeDriverTracker;

    private BoardingDetector detector = new BoardingDetector();

    OutlierFilter outlierFilter = new OutlierFilter();

    private Deque<LocationPoint> recordWindow = new ArrayDeque<>(FilterConfig.RECORD_POINTS_SIZE);
    private List<LocationPoint>  recordPoints = new ArrayList<>();

    /**
     * 实时任务
     */
    public void realDriverTrackerZQ() throws ParseException {

        String data = baseConfig.LOCATION_CARD_TYPE;
        String date = "未获取到日期";

//        JSONObject jsonObject = ZQOpenApi.getListOfCards();
//        JSONArray points = jsonObject.getJSONArray("data");
//        if (points != null && !points.isEmpty()) {
//
//        }
        String cardId = "1918B3000BA3";
        String buildId = baseConfig.getJoysuch().getBuildingId();
        String startTimeStr = "2025-08-12 15:50:00";
        String endTimeStr = "2025-08-12 17:00:00";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);


        // 循环从 start 到 end，每次加 10 秒
        LocalDateTime current = startTime;
        while (!current.isAfter(endTime)) {
            // 时间 +10 秒
            LocalDateTime startCurrent = current;
            LocalDateTime endCurrent = current.plusSeconds(10);
            current = endCurrent;
            String startStr = DateTimeUtils.localDateTime2String(startCurrent);
            String endStr = DateTimeUtils.localDateTime2String(endCurrent);
            JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startStr, endStr));
            JSONArray points = jsonObject.getJSONArray("data");
            List<LocationPoint> LocationPoints = new ArrayList<>();
            for (int i = 0; i < points.size(); i++){
                JSONObject js = (JSONObject) points.get(i);
                JSONArray plist = js.getJSONArray("points");
                for (int j = 0; j < plist.size(); j++){
                    LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                    if (date.equals("未获取到日期")){
                        date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                    }
                    LocationPoint point1 = new LocationPoint(
                            cardId,
                            point.getLongitude(),
                            point.getLatitude(),
                            DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                            Long.parseLong(point.getTime()));
                    LocationPoints.add(point1);
                }
            }
            if (LocationPoints.size() > 0){
                realTimeDriverTracker.ingest(LocationPoints);
            }
//            List<LocationPoint> normalPoints = new ArrayList<>();
//            for (LocationPoint rawPoint : LocationPoints){
//                int state = outlierFilter.isValid(rawPoint);
//                if (!(state == 0)) {
//                    if (state == 1){
//                        System.out.println("⚠️  时间间隔异常定位点已剔除：" + rawPoint);
//                    }
////                    else if (state == 2) {
////                    System.out.println("⚠️  速度异常定位点已剔除：" + rawPoint);
////                }
//                    else if (state == 3) {
//                        System.out.println("⚠️  定位异常定位点已剔除：" + rawPoint);
//                    } else {
//                        System.out.println("⚠️  异常定位点已剔除：" + rawPoint);
//                    }
//                }else {
//                    // 正常点
//                    normalPoints.add(rawPoint);
//                }
//            }
//            for (LocationPoint point : normalPoints){
//                recordPoints.add(point);
//                if (recordPoints.size() >= FilterConfig.RECORD_POINTS_SIZE){
//                    List<LocationPoint> newPoints = new OutlierFilter().fixTheData(recordPoints);
//                    if (newPoints.size() < FilterConfig.RECORD_POINTS_SIZE){
//                        recordWindow = new ArrayDeque<>(newPoints);
//                        continue;
//                    }
//                    tracker.realHandleNewRawPoint(newPoints);
//                }
//            }
//            for (LocationPoint point : normalPoints){
//                recordWindow.addLast(point);
//                while (recordWindow.size() > FilterConfig.RECORD_POINTS_SIZE) recordWindow.removeFirst();
//                if (recordWindow.size() == FilterConfig.RECORD_POINTS_SIZE){
//                    List<LocationPoint> window = new ArrayList<>(recordWindow);
//                    List<LocationPoint> newPoints = new OutlierFilter().fixTheData(window);
//                    if (newPoints.size() < FilterConfig.RECORD_POINTS_SIZE){
//                        recordWindow = new ArrayDeque<>(newPoints);
//                        continue;
//                    }
//                    tracker.realHandleNewRawPoint(newPoints);
//                }
//            }
            String shpFilePath = baseConfig.getOutputShpPath() + "/" + date + "/" + data + "/";
            DriverTracker.cardId = "1918B3000BA3";
            DriverTracker.shpFilePath = shpFilePath;
        }
    }

    /**
     * 实时任务测试（历史数据模拟）
     */
    public void realDriverTrackerZQTest(){

        String data = baseConfig.LOCATION_CARD_TYPE;
        String date = "未获取到日期";
        String buildId = baseConfig.getJoysuch().getBuildingId();
//        String cardId = "1918B3000BA3";
//        String startTimeStr = "2025-08-06 18:20:00";
//        String endTimeStr = "2025-08-06 21:00:00";
//        String cardId = "1918B3000BA8";
//        String startTimeStr = "2025-09-24 18:00:00";
//        String endTimeStr = "2025-09-24 19:40:00";
        String cardId = "1918B3000A79";
//        String startTimeStr = "2025-09-28 17:00:00";
//        String endTimeStr = "2025-09-28 19:00:00";
        String startTimeStr = "2025-10-15 13:04:00";
        String endTimeStr = "2025-10-15 13:06:00";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
//        List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(baseConfig.getSwCenter().getTenantId(), startTimeStr + " 000", endTimeStr + " 000");
        JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
        JSONObject tagJsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)), DateTimeUtils.localDateTime2String(endTime.plusSeconds(2))));
        JSONArray points = jsonObject.getJSONArray("data");
        JSONArray tagData = tagJsonObject.getJSONArray("data");
        List<LocationPoint> LocationPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++){
            JSONObject js = (JSONObject) points.get(i);
            JSONArray plist = js.getJSONArray("points");
            for (int j = 0; j < plist.size(); j++){
                LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                if (date.equals("未获取到日期")){
                    date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                }
                LocationPoint point1 = new LocationPoint(
                        cardId,
                        point.getLongitude(),
                        point.getLatitude(),
                        DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                        Long.parseLong(point.getTime()));
                LocationPoints.add(point1);
            }
        }
        LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints,tagData);
        if (LocationPoints.size() > 0){
//            int batchSize = 10;
//            for (int i = 0; i < LocationPoints.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, LocationPoints.size());
//                List<LocationPoint> batch = LocationPoints.subList(i, end);
//                realTimeDriverTracker.ingest(batch);
//            }
            realTimeDriverTracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.CAR);
        }
    }

    /**
     * 实时任务测试（历史数据模拟）
     */
    public void realDriverTrackerZQTruckTest(){

        String data = baseConfig.LOCATION_CARD_TYPE;
        String date = "未获取到日期";
        String buildId = baseConfig.getJoysuch().getBuildingId();
        String cardId = "1918B3000561";
        String startTimeStr = "2025-10-16 18:25:00";
        String endTimeStr = "2025-10-16 19:40:00";
        LocalDateTime startTime = DateTimeUtils.str2DateTime(startTimeStr);
        LocalDateTime endTime = DateTimeUtils.str2DateTime(endTimeStr);
//        List<ReqVehicleCode> reqVehicleCodes = centerWorkHttpUtils.getRfidList(baseConfig.getSwCenter().getTenantId(), startTimeStr + " 000", endTimeStr + " 000");
        JSONObject jsonObject = JSONObject.parseObject(zqOpenApi.getListOfPoints(cardId, buildId, startTimeStr, endTimeStr));
        JSONObject tagJsonObject = JSONObject.parseObject(zqOpenApi.getTagStateHistoryOfTagID(buildId, cardId, DateTimeUtils.localDateTime2String(startTime.minusSeconds(2)), DateTimeUtils.localDateTime2String(endTime.plusSeconds(2))));
        JSONArray points = jsonObject.getJSONArray("data");
        JSONArray tagData = tagJsonObject.getJSONArray("data");
        List<LocationPoint> LocationPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++){
            JSONObject js = (JSONObject) points.get(i);
            JSONArray plist = js.getJSONArray("points");
            for (int j = 0; j < plist.size(); j++){
                LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
                if (date.equals("未获取到日期")){
                    date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
                }
                LocationPoint point1 = new LocationPoint(
                        cardId,
                        point.getLongitude(),
                        point.getLatitude(),
                        DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
                        Long.parseLong(point.getTime()));
                LocationPoints.add(point1);
            }
        }
        LocationPoints = FusionData.processesFusionLocationDataAndTagData(LocationPoints,tagData);
        if (LocationPoints.size() > 0){
//            int batchSize = 10;
//            for (int i = 0; i < LocationPoints.size(); i += batchSize) {
//                int end = Math.min(i + batchSize, LocationPoints.size());
//                List<LocationPoint> batch = LocationPoints.subList(i, end);
//                realTimeDriverTracker.ingest(batch);
//            }
            realTimeDriverTracker.replayHistorical(LocationPoints, RealTimeDriverTracker.VehicleType.TRUCK);
        }
    }

    public void test() {
        System.out.println(baseConfig.isStayVerify());
    }


    public void driverTrackerZQ()
    {
        test();
//        String data = BaseConfig.LOCATION_CARD_TYPE;
//        String date = "未获取到日期";
//
////        JSONObject jsonObject = ZQOpenApi.getListOfCards();
////        JSONArray points = jsonObject.getJSONArray("data");
////        if (points != null && !points.isEmpty()) {
////
////        }
//        String cardId = "1918B3000BA3";
//        String buildId = "209885";
//        String startTime = "2025-08-06 16:00:00";
//        String endTime = "2025-08-06 21:00:00";
//        JSONObject jsonObject = JSONObject.parseObject(getListOfPoints(cardId, buildId, startTime, endTime));
//        JSONArray points = jsonObject.getJSONArray("data");
//        List<LocationPoint> LocationPoints = new ArrayList<>();
//        for (int i = 0; i < points.size(); i++){
//            JSONObject js = (JSONObject) points.get(i);
//            JSONArray plist = js.getJSONArray("points");
//            for (int j = 0; j < plist.size(); j++){
//                LocationPoint2 point = plist.getObject(j, LocationPoint2.class);
//                if (date.equals("未获取到日期")){
//                    date = DateTimeUtils.timestampToDateStr(Long.parseLong(point.getTime()));
//                }
//                LocationPoint point1 = new LocationPoint(
//                        cardId,
//                        point.getLongitude(),
//                        point.getLatitude(),
//                        DateTimeUtils.timestampToDateTimeStr(Long.parseLong(point.getTime())),
//                        Long.parseLong(point.getTime()));
//                LocationPoints.add(point1);
//            }
//        }
//        String shpFilePath = outputShpPath + "/" + date + "/" + data + "/";
//        DriverTracker.cardId = "1918B3000BA3";
//        DriverTracker.shpFilePath = shpFilePath;
//        // 生成原始点位数据和时间序列清洗过的数据shp文件
//        DriverTracker.processWithAnchorDataZQ(LocationPoints, data);
//        // 再次根据点位、是否时间一样、是否漂移清洗数据
//        List<LocationPoint> newPoints = new OutlierFilter().fixTheData(LocationPoints);
//        if (baseConfig.isOutputShp()){
//            //清洗过运动或停留数据后生成shp文件
//            DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
//        }
//        // 开始行为分析
//        tracker.handleNewRawPoint(newPoints);

    }

    public void driverTracker()
    {
        String data = null;
        String date = "未获取到日期";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\51718.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\63856.txt";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250705.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\250710.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250710定位卡63856RTK.json";1
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250724.json";
//        String file = "C:\\Users\\Admin\\Desktop\\定位卡数据\\鱼嘴\\20250729.json";
        String file = "D:\\PlatformData\\定位卡数据\\鱼嘴\\20250729.json";
        JSONObject jsonObject = JsonUtils.loadJson(file);
        JSONArray points = jsonObject.getJSONArray("data");
        if (points != null && !points.isEmpty()) {
            JSONObject firstObj = points.getJSONObject(0);
            if (firstObj.containsKey("trajectoryId")) {
                // 存在 trajectoryId
                data = BaseConfig.OTHER;
            } else {
                // 不存在 trajectoryId
                data = BaseConfig.RTK;
            }
            if (firstObj.containsKey("recordTimeLength")){
                // 存在 acceptTime
                date = DateTimeUtils.timestampToDateStr(Long.parseLong(firstObj.getString("recordTimeLength")));
            } else if (firstObj.containsKey("timestamp")) {
                date = DateTimeUtils.timestampToDateStr(Long.parseLong(firstObj.getString("timestamp")));
            }
        }
        if (data == null){
            return;
        }
        String shpFilePath = baseConfig.getOutputShpPath() + "/" + date + "/" + data + "/";
        DriverTracker.shpFilePath = shpFilePath;
        List<LocationPoint> LocationPoints = new DriverTracker().processWithAnchorData(points, data);
        // 按卡号分组
        if (data.equals("rtk")){
            Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardId));
            for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (baseConfig.isOutputShp()){
                    //清洗过运动或停留数据后生成shp文件
                    DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
                }
                DriverTracker.cardId = String.valueOf(entry.getKey());
                // 开始行为分析
                tracker.handleNewRawPoint(newPoints);
//                DriverTracker tracker = new DriverTracker();
                // 开始行为分析
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }
            }
        } else if (data.equals("other")){
            Map<String, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardUUID));
            for (Map.Entry<String, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (baseConfig.isOutputShp()){
                    //清洗过运动或停留数据后生成shp文件
                    DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
                }
                DriverTracker.cardId = entry.getKey();
                // 开始行为分析
                tracker.handleNewRawPoint(newPoints);
//                for (LocationPoint point : newPoints) {
//                    tracker.handleNewRawPoint(tracker, point);
//                }

            }
        }
    }
}
