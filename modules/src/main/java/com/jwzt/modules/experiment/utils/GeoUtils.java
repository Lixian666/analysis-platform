package com.jwzt.modules.experiment.utils;

import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.geom.Point2D;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GeoUtils {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    // 地球半径，单位米
    private static final double EARTH_RADIUS = 6371000;

    /**
     * 计算两个经纬度之间的距离
     *
     * @param lon1 点1的经度
     * @param lat1 点1的纬度
     * @param lon2 点2的经度
     * @param lat2 点2的纬度
     * @return 距离，单位：米
     */
    public static double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        // 将角度转换为弧度
        double radLat1 = Math.toRadians(lat1);
        double radLon1 = Math.toRadians(lon1);
        double radLat2 = Math.toRadians(lat2);
        double radLon2 = Math.toRadians(lon2);

        // 计算差值
        double deltaLat = radLat2 - radLat1;
        double deltaLon = radLon2 - radLon1;

        // 使用 Haversine 公式计算距离
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static double distanceM(Coordinate coordinate1, Coordinate coordinate2) {
        GeodeticCalculator calc = new GeodeticCalculator();
        // 设置起点（经度，纬度）
        calc.setStartingGeographicPoint(coordinate1.getLongitude(), coordinate1.getLatitude());
        calc.setDestinationGeographicPoint(coordinate2.getLongitude(), coordinate2.getLatitude());
        // 获取地理距离（单位：米）
        double distance = calc.getOrthodromicDistance(); // 也叫大圆距离
        return distance;
    }

    /**
     * 计算方向变化角度（度）
     */
    public static double calculateDirectionChange(Coordinate p0, Coordinate p1, Coordinate p2, Coordinate p3, Coordinate p4) {
        // 前向向量 (p1->p2)
        double vec1x = p2.getLongitude() - p1.getLongitude();
        double vec1y = p2.getLatitude() - p1.getLatitude();

        // 后向向量 (p2->p3)
        double vec2x = p3.getLongitude() - p2.getLongitude();
        double vec2y = p3.getLatitude() - p2.getLatitude();

        // 计算点积
        double dot = vec1x * vec2x + vec1y * vec2y;
        double mag1 = Math.sqrt(vec1x * vec1x + vec1y * vec1y);
        double mag2 = Math.sqrt(vec2x * vec2x + vec2y * vec2y);

        // 避免除零
        if (mag1 < 1e-6 || mag2 < 1e-6) return 0.0;

        // 计算角度（度）
        double angle = Math.toDegrees(Math.acos(dot / (mag1 * mag2)));
        return angle;
    }

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        // 将角度转换为弧度
        double radLat1 = Math.toRadians(coordinate1.getLatitude());
        double radLon1 = Math.toRadians(coordinate1.getLongitude());
        double radLat2 = Math.toRadians(coordinate2.getLatitude());
        double radLon2 = Math.toRadians(coordinate2.getLongitude());

        // 计算差值
        double deltaLat = radLat2 - radLat1;
        double deltaLon = radLon2 - radLon1;

        // 使用 Haversine 公式计算距离
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }


    /**
     * 计算平均速度
     * @param coordinates
     * @return
     */
    public double calculateSpeeds(List<Coordinate> coordinates) {
        if(coordinates.size()<2) {
            System.out.println("数据不足无法计算速度");
            return 0d;
        }
        double speed = 0d;
        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate current = coordinates.get(i);
            Coordinate previous = coordinates.get(i - 1);
            double distance = calculateDistance(previous, current);
            long timeDifference = current.getTimestamp() - previous.getTimestamp(); // in milliseconds
            if (timeDifference == 0) { // Avoid division by zero error
                speed +=0.0;
            } else {
                speed += distance / (timeDifference / 1000.0);
            }
        }
        return speed/(coordinates.size()-1);
    }

    /**
      * 将自定义 Coordinate 列表转换为 JTS 的 Coordinate[]多边形
      * @param customCoordinates
      * @return
    */
    public static Polygon toPolygon(List<Coordinate> customCoordinates) {
        // 必须闭合：首尾相同
        if (!isClosed(customCoordinates)) {
            customCoordinates.add(new Coordinate(
                    customCoordinates.get(0).getLongitude(),
                    customCoordinates.get(0).getLatitude()
            ));
        }

        org.locationtech.jts.geom.Coordinate[] jtsCoords = customCoordinates.stream()
                .map(c -> new org.locationtech.jts.geom.Coordinate(c.getLongitude(), c.getLatitude()))
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);

        LinearRing shell = geometryFactory.createLinearRing(jtsCoords);
        return geometryFactory.createPolygon(shell);
    }

    /**
     *  判断点是否在多边形内
     * @param pointCoord
     * @param polygon
     * @return
     */
    public static boolean isPointInPolygon(Coordinate pointCoord, Polygon polygon) {
        Point point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(pointCoord.getLongitude(), pointCoord.getLatitude())
        );
        return polygon.covers(point); // covers比contains更宽松，包含边界
    }

    /**
      * 判断多边形是否闭合
      * @param coords
      * @return
      */
    public static boolean isClosed(List<Coordinate> coords) {
        if (coords == null || coords.size() < 3) return false;
        Coordinate first = coords.get(0);
        Coordinate last = coords.get(coords.size() - 1);
        return first.getLongitude() == last.getLongitude() && first.getLatitude() == last.getLatitude();
    }

    /**
     * 判断点是否在多边形内
     * @param coordinate
     * @param polygonList
     * @return
     */
    public boolean isInsidePolygon(Coordinate coordinate, List<Coordinate> polygonList) {

        Coordinate point = new Coordinate(coordinate.getLongitude(), coordinate.getLatitude());
        Polygon polygon = toPolygon(polygonList);
        boolean inside = isPointInPolygon(point, polygon);
        return inside;
    }

    /**
     * 将shp文件变成Geometry
     * @param shpFilePaths
     * @return
     */
    public static List<Geometry> loadGeometries(List<String> shpFilePaths) {
        List<Geometry> geometryList = new ArrayList<>();
        for (String path : shpFilePaths) {
            File file = new File(path);
            if (!file.exists()) continue;

            try {
                ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(file);
                if (store == null) continue;

                SimpleFeatureSource featureSource = store.getFeatureSource();
                try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                    while (features.hasNext()) {
                        SimpleFeature feature = features.next();
                        Object geomObj = feature.getDefaultGeometry();
                        if (geomObj instanceof Geometry) {
                            geometryList.add((Geometry) geomObj);
                        }
                    }
                }
                store.dispose();
            } catch (Exception e) {
                System.err.println("读取 shp 文件失败：" + path);
                e.printStackTrace();
            }
        }
        return geometryList;
    }

    /**
     * 判断点是否在多个Geometry中的任意一个多边形内
     * @param coordinate
     * @param geometries
     * @return
     */
    public static boolean isInsideGeometry(Coordinate coordinate, List<Geometry> geometries) {
        if (coordinate == null || geometries == null || geometries.isEmpty()) return false;
        Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude()));
        for (Geometry geom : geometries) {
            if (geom.covers(point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断点是否在多个shp文件中的任意一个多边形内
     * @param coordinate
     * @param shpFilePaths
     * @return
     */
    public static boolean isInsideShp(Coordinate coordinate, List<String> shpFilePaths) {
        if (coordinate == null) return false;

        // 将自定义的 Coordinate 转为 JTS 的 Point
        Point point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude())
        );

        for (String path : shpFilePaths) {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("文件不存在！");
                continue;
            }

            try {
                // 显式创建 ShapefileDataStore
                ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(file);
                if (store == null) {
                    System.out.println("无法创建 FileDataStore，请检查文件格式或依赖配置。");
                    continue;
                }

                SimpleFeatureSource featureSource = store.getFeatureSource();
                try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                    while (features.hasNext()) {
                        SimpleFeature feature = features.next();
                        Object geomObj = feature.getDefaultGeometry();
                        if (geomObj instanceof Geometry) {
                            Geometry geom = (Geometry) geomObj;
                            if (geom.covers(point)) {
                                return true;
                            }
                        }
                    }
                }
                store.dispose(); // 主动释放资源
            } catch (Exception e) {
                System.err.println("处理shp文件失败：" + path);
                e.printStackTrace(); // 打印详细异常信息
                return false;
            }
        }
        return false;
    }

    /**
     * 判断点是否在多个shp文件中的任意一个多边形内，支持缓冲区
     * @param coordinate 点坐标
     * @param shpFilePaths shp文件路径列表
     * @param bufferDistance 缓冲距离（米）
     * @return 是否在任意一个多边形（或缓冲区）内
     */
    public static boolean isInsideShp(Coordinate coordinate, List<String> shpFilePaths, double bufferDistance) {
        boolean enableBuffer = bufferDistance > 0;
        if (coordinate == null) return false;

        // 创建 Point 对象
        Point point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(coordinate.getLongitude(), coordinate.getLatitude())
        );

        // 将缓冲距离从米转换为经纬度（大致估算）
        double bufferDegree = enableBuffer ? metersToDegrees(bufferDistance) : 0;

        for (String path : shpFilePaths) {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("文件不存在：" + path);
                continue;
            }

            try {
                ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(file);
                if (store == null) {
                    System.out.println("无法创建 FileDataStore，请检查文件格式或依赖配置。");
                    continue;
                }

                SimpleFeatureSource featureSource = store.getFeatureSource();
                try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                    while (features.hasNext()) {
                        SimpleFeature feature = features.next();
                        Object geomObj = feature.getDefaultGeometry();
                        if (geomObj instanceof Geometry) {
                            Geometry geom = (Geometry) geomObj;

                            // 若开启缓冲则生成缓冲区
                            if (enableBuffer) {
                                geom = geom.buffer(bufferDegree);
                            }

                            if (geom.covers(point)) {
                                return true;
                            }
                        }
                    }
                }
                store.dispose(); // 主动释放资源
            } catch (Exception e) {
                System.err.println("处理shp文件失败：" + path);
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * 将米转为近似经纬度（1度约等于111km）
     * @param meters 米
     * @return 近似度数
     */
    private static double metersToDegrees(double meters) {
        return meters / 111000.0; // 近似换算
    }

    /**
     * 计算中心点（经纬度平均值）
     */
    public static LocationPoint calculateCenter(List<LocationPoint> points) {
        double sumLng = 0.0;
        double sumLat = 0.0;
        for (LocationPoint p : points) {
            sumLng += p.getLongitude();
            sumLat += p.getLatitude();
        }
        return new LocationPoint(
                points.get(0).getCardId(),
                sumLng / points.size(),
                sumLat / points.size());

    }

    /**
     * 处理一秒内多个点的情况（使用中位数）
     */
    public static List<LocationPoint> processMultiplePointsPerSecond(List<LocationPoint> points) {
        points.sort(Comparator.comparingLong(LocationPoint::getTimestamp));
        Map<Long, List<LocationPoint>> perSecond = points.stream()
                .collect(Collectors.groupingBy(p -> p.getTimestamp() / 1000));

        List<LocationPoint> result = new ArrayList<>();
        for (List<LocationPoint> secondPoints : perSecond.values()) {
            if (secondPoints.isEmpty()) continue;

            if (secondPoints.size() == 1) {
                result.add(secondPoints.get(0));
            } else {
                double medianLng = calculateMedian(
                        secondPoints.stream().mapToDouble(LocationPoint::getLongitude).toArray());
                double medianLat = calculateMedian(
                        secondPoints.stream().mapToDouble(LocationPoint::getLatitude).toArray());

                LocationPoint medianPoint = new LocationPoint(
                        secondPoints.get(0).getCardId(),
                        medianLng,
                        medianLat,
                        secondPoints.get(0).getAcceptTime(),
                        secondPoints.get(0).getTimestamp());
                result.add(medianPoint);
            }
        }

        result.sort(Comparator.comparingLong(LocationPoint::getTimestamp));
        return result;
    }

//    /**
//     * 处理一秒内多个点的情况（使用中位数）
//     */
//    private static List<Coordinate> processMultiplePointsPerSecond(List<Coordinate> points) {
//        points.sort(Comparator.comparingLong(Coordinate::getTimestamp));
//        Map<Long, List<Coordinate>> perSecond = points.stream()
//                .collect(Collectors.groupingBy(p -> p.getTimestamp() / 1000));
//
//        List<Coordinate> result = new ArrayList<>();
//        for (List<Coordinate> secondPoints : perSecond.values()) {
//            if (secondPoints.isEmpty()) continue;
//
//            if (secondPoints.size() == 1) {
//                result.add(secondPoints.get(0));
//            } else {
//                double medianLng = calculateMedian(
//                        secondPoints.stream().mapToDouble(Coordinate::getLongitude).toArray());
//                double medianLat = calculateMedian(
//                        secondPoints.stream().mapToDouble(Coordinate::getLatitude).toArray());
//
//                Coordinate medianPoint = new Coordinate(
//                        medianLng, medianLat, secondPoints.get(0).getTimestamp());
//                result.add(medianPoint);
//            }
//        }
//
//        result.sort(Comparator.comparingLong(Coordinate::getTimestamp));
//        return result;
//    }

    /**
     * 计算中位数
     */
    private static double calculateMedian(double[] values) {
        Arrays.sort(values);
        int mid = values.length / 2;
        return (values.length % 2 == 0) ?
                (values[mid - 1] + values[mid]) / 2.0 :
                values[mid];
    }


}
