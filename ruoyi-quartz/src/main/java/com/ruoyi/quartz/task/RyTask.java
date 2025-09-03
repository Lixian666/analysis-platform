package com.ruoyi.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jwzt.modules.experiment.DriverTracker;
import com.jwzt.modules.experiment.config.BaseConfg;
import com.jwzt.modules.experiment.config.FilePathConfig;
import com.jwzt.modules.experiment.config.FilterConfig;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.domain.LocationPoint2;
import com.jwzt.modules.experiment.filter.OutlierFilter;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.JsonUtils;
import com.jwzt.modules.experiment.utils.third.ZQOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jwzt.modules.experiment.config.BaseConfg.OUTPUT_SHP_PATH;
import static com.jwzt.modules.experiment.utils.third.ZQOpenApi.getListOfPoints;

/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    @Autowired
    private DriverTracker tracker;
    public void driverTrackerZQ()
    {
        String data = BaseConfg.LOCATION_CARD_TYPE;
        String date = "未获取到日期";

//        JSONObject jsonObject = ZQOpenApi.getListOfCards();
//        JSONArray points = jsonObject.getJSONArray("data");
//        if (points != null && !points.isEmpty()) {
//
//        }
        String cardId = "1918B3000BA3";
        String buildId = "209885";
        String startTime = "2025-08-06 16:00:00";
        String endTime = "2025-08-06 21:00:00";
        JSONObject jsonObject = JSONObject.parseObject(getListOfPoints(cardId, buildId, startTime, endTime));
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
        String shpFilePath = OUTPUT_SHP_PATH + "/" + date + "/" + data + "/";
        DriverTracker.cardId = "1918B3000BA3";
        DriverTracker.shpFilePath = shpFilePath;
        // 生成原始点位数据和时间序列清洗过的数据shp文件
        DriverTracker.processWithAnchorDataZQ(LocationPoints, data);
        // 再次根据点位、是否时间一样、是否漂移清洗数据
        List<LocationPoint> newPoints = new OutlierFilter().fixTheData(LocationPoints);
        if (BaseConfg.IS_OUTPUT_SHP){
            //清洗过运动或停留数据后生成shp文件
            DriverTracker.outputVectorFiles(newPoints,shpFilePath + "data_clean_points.shp");
        }
        // 开始行为分析
        tracker.handleNewRawPoint(newPoints);

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
                data = BaseConfg.OTHER;
            } else {
                // 不存在 trajectoryId
                data = BaseConfg.RTK;
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
        String shpFilePath = OUTPUT_SHP_PATH + "/" + date + "/" + data + "/";
        DriverTracker.shpFilePath = shpFilePath;
        List<LocationPoint> LocationPoints = DriverTracker.processWithAnchorData(points, data);
        // 按卡号分组
        if (data.equals("rtk")){
            Map<Integer, List<LocationPoint>> groupedByCardId = LocationPoints.stream()
                    .collect(Collectors.groupingBy(LocationPoint::getCardId));
            for (Map.Entry<Integer, List<LocationPoint>> entry : groupedByCardId.entrySet()) {
                // 取出一个卡号的所有点
                List<LocationPoint> pointsByCardId = entry.getValue();
                // 再次根据点位、是否时间一样、是否漂移清洗数据
                List<LocationPoint> newPoints = new OutlierFilter().fixTheData(pointsByCardId);
                if (BaseConfg.IS_OUTPUT_SHP){
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
                if (BaseConfg.IS_OUTPUT_SHP){
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
