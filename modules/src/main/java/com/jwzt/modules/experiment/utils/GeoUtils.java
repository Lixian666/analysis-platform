package com.jwzt.modules.experiment.utils;

import com.jwzt.modules.experiment.domain.Coordinate;
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
import java.util.List;

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

}
