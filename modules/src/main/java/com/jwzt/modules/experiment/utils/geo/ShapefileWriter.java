package com.jwzt.modules.experiment.utils.geo;

import com.jwzt.modules.experiment.domain.Coordinate;
import com.jwzt.modules.experiment.domain.LocationPoint;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

import static com.jwzt.modules.experiment.utils.FileUtils.ensureFilePathExists;

public class ShapefileWriter {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public static void writeCoordinatesToShapefile(List<Coordinate> points, String shpPath) {
        try {
            // 1. 定义Feature类型（字段结构）
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName("LocationPoint");
            typeBuilder.setCRS(DefaultGeographicCRS.WGS84); // WGS84经纬度坐标
            typeBuilder.add("the_geom", Point.class);
            typeBuilder.add("id", Integer.class); // 可以添加更多属性
            typeBuilder.add("time", String.class); // 时间字段
            typeBuilder.add("timestamp", Long.class); // 时间字段
            final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

            // 2. 创建 FeatureCollection
            DefaultFeatureCollection collection = new DefaultFeatureCollection();

            for (int i = 0; i < points.size(); i++) {
                Coordinate c = points.get(i);
                Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(c.getLongitude(), c.getLatitude()));
                Date timestamp = new Date(c.getTimestamp());
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                featureBuilder.add(point);
                featureBuilder.add(i); //
                String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
                featureBuilder.add(timeString); // 加入时间字符串
                featureBuilder.add(c.getTimestamp()); // 时间
                SimpleFeature feature = featureBuilder.buildFeature(null);
                collection.add(feature);
            }

            // 3. 创建 Shapefile
            File newFile = new File(shpPath);
            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

            DataStore dataStore = dataStoreFactory.createNewDataStore(
                    Collections.singletonMap("url", newFile.toURI().toURL())
            );
            dataStore.createSchema(TYPE);

            // 4. 写入数据
            Transaction transaction = new DefaultTransaction("create");
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(typeName);

            featureStore.setTransaction(transaction);
            featureStore.addFeatures(collection);
            transaction.commit();
            transaction.close();
            dataStore.dispose();

            System.out.println("写入shp成功: " + shpPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** shp 输出 */
    public static void outputVectorFiles(List<LocationPoint> points, String shpFilePath) {
        List<Coordinate> coordinates = new ArrayList<>(points.size());
        for (LocationPoint p : points) {
            coordinates.add(new Coordinate(p.getLongitude(), p.getLatitude(), p.getTimestamp()));
        }
        ensureFilePathExists(shpFilePath);
        ShapefileWriter.writeCoordinatesToShapefile(coordinates, shpFilePath);
    }
}
