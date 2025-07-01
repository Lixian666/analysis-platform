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

    public static boolean isInDrivingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Arrays.asList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp",
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp",
                "D:\\work\\data\\shp\\minhang\\货运线\\huoyunxin.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    public static boolean isInParkingZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\20250624库区\\kuqu.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }

    public boolean isInRoadZone(LocationPoint p) {
        Coordinate newCoordinate = new Coordinate(p.getLongitude(), p.getLatitude());
        List<String> shpPaths = Collections.singletonList(
                "D:\\work\\data\\shp\\minhang\\行车道路\\xingcdaolu.shp"
        );
        return GeoUtils.isInsideShp(newCoordinate, shpPaths);
    }
}
