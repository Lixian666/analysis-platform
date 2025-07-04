package com.jwzt.modules.experiment.map;

import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import com.jwzt.modules.experiment.utils.GeoUtils;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 区域检查器
 */
public class ZoneChecker {

    /**
     * 是否在货场驾驶区域
     * @param p
     * @return
     */
    public static boolean isInDrivingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Arrays.asList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp",
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp",
                "D:\\work\\data\\shp\\minhang\\货运线\\huoyunxin.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    /**
     * 是否在货场货运线区域
     * @param p
     * @return
     */
    public static boolean isInHuoyunxinZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\货运线\\huoyunxin.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths, 1.0);
    }

    /**
     * 是否在货场停车区域
     * @param p
     * @return
     */
    public static boolean isInParkingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    /**
     * 是否在货场道路区域
     * @param p
     * @return
     */
    public boolean isInRoadZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }
}
