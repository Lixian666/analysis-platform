package com.jwzt.modules.experiment.utils.third.zq.beacon;

import java.util.*;

import com.jwzt.modules.experiment.domain.TakBeaconInfo;

/***
 * 补完所有信标距离
 * @author zpn-worker
 *
 */
public class BeaconRepair {

    public List<TakBeaconInfo> beaconList = new ArrayList<>();

    public class Point {
        double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    /**
     * 根据正确信标估算故障信标的真实距离
     * * @param correctBeacons  正确信标的经纬度列表
     * @param correctDistances 正确信标对应的测量距离(米)
     * @param faultyBeacon    故障信标的经纬度
     * @return 估算出的正确距离(米)
     */
    public double estimateCorrectDistance(List<TakBeaconInfo> correctBeacons, 
                                                 List<Double> correctDistances, 
                                                 TakBeaconInfo faultyBeacon) {
        if (correctBeacons.size() < 2) {
            throw new IllegalArgumentException("至少需要2个（推荐3个以上）正确信标来定位");
        }

        // 1. 将经纬度转换为平面坐标 (以第一个正确信标为原点)
        TakBeaconInfo origin = correctBeacons.get(0);
        List<Point> points = new ArrayList<>();
        for (TakBeaconInfo b : correctBeacons) {
            points.add(project(origin, b));
        }
        Point faultyPoint = project(origin, faultyBeacon);

        // 2. 估算当前用户的位置 (x, y)
        // 初始值设为所有信标的中心点
        Point userPos = calculateUserPosition(points, correctDistances);

        // 3. 计算用户位置到故障信标的距离
        return Math.sqrt(Math.pow(userPos.x - faultyPoint.x, 2) + Math.pow(userPos.y - faultyPoint.y, 2));
    }

    /**
     * 简易的多边定位算法（基于质心迭代优化）
     */
    private Point calculateUserPosition(List<Point> beacons, List<Double> dists) {
        double userX = 0, userY = 0;
        for (Point p : beacons) {
            userX += p.x;
            userY += p.y;
        }
        userX /= beacons.size();
        userY /= beacons.size();

        // 简单的迭代优化（梯度下降思想）
        for (int iter = 0; iter < 100; iter++) {
            double dx = 0, dy = 0;
            for (int i = 0; i < beacons.size(); i++) {
                Point b = beacons.get(i);
                double d_measured = dists.get(i);
                double d_current = Math.sqrt(Math.pow(userX - b.x, 2) + Math.pow(userY - b.y, 2));
                
                if (d_current < 0.1) continue; // 防止除零

                // 计算误差向量
                double ratio = (d_current - d_measured) / d_current;
                dx += ratio * (userX - b.x);
                dy += ratio * (userY - b.y);
            }
            // 更新步长
            userX -= dx / beacons.size() * 0.5;
            userY -= dy / beacons.size() * 0.5;
        }
        return new Point(userX, userY);
    }

    /**
     * 经纬度转平面坐标（复用之前的逻辑）
     */
    private Point project(TakBeaconInfo origin, TakBeaconInfo target) {
        double earthRadius = 6371000.0;
        double latRad = Math.toRadians(origin.getLatitude());
        double y = Math.toRadians(target.getLatitude() - origin.getLatitude()) * earthRadius;
        double x = Math.toRadians(target.getLongitude() - origin.getLongitude()) * earthRadius * Math.cos(latRad);
        return new Point(x, y);
    }
    
    public  Map<String, Double> repairBeaconDistance(Map<String, Double> deviceReport, double tolerance){
    	
    	BeaconFaultDetector beaconFaultDetector = new BeaconFaultDetector();
        beaconFaultDetector.setBaseLocal(beaconList);
        List<String> errorbeacon = beaconFaultDetector.detect(deviceReport, tolerance);
        for(String beacon : errorbeacon) {
        	deviceReport.remove(beacon);
        }
        
        List<TakBeaconInfo> noMessageBeanconList = new ArrayList();
        List<TakBeaconInfo> baseBeanconList = beaconFaultDetector.getBaseLocal();
        for(TakBeaconInfo beacon : baseBeanconList) {
        	if(deviceReport.get(beacon.getBeaconId()) == null) {
        		noMessageBeanconList.add(beacon);
        	}
        }
        
        
        List<TakBeaconInfo> rightBeacon = new ArrayList<TakBeaconInfo>();
        List<Double> rightBeaconDistance = new ArrayList<Double>(); 
        
        Map<String, Double> repairBeaconDistance = new HashMap<String, Double>();
        
        
        for(String beaconId :deviceReport.keySet()) {
        	
        	for(TakBeaconInfo baseBeancon : baseBeanconList) {
        		if(baseBeancon.getBeaconId().equals(beaconId)) {
        			rightBeacon.add(baseBeancon);
        			rightBeaconDistance.add(deviceReport.get(beaconId));
        		}
        	}
        	
        }
        
        for(TakBeaconInfo beacon : noMessageBeanconList) {
        	Double repairDistance = Math.round(estimateCorrectDistance(rightBeacon, rightBeaconDistance, beacon) * 100.0) / 100.0;
        	repairBeaconDistance.put(beacon.getBeaconId(), repairDistance);
        }
        
        repairBeaconDistance.putAll(deviceReport);
        
        return repairBeaconDistance;
        
        
    }
    	
    
 // --- 测试 ---
    public static void main(String[] args) {
    	
    	BeaconFaultDetector beaconFaultDetector = new BeaconFaultDetector();
        Map<String, Double> deviceReport = beaconFaultDetector.getErrorPront();

        BeaconRepair beaconRepair = new BeaconRepair();
        
        deviceReport = beaconRepair.repairBeaconDistance(deviceReport, 8.0);
        System.out.println(deviceReport);
        
        System.out.println("开始计算位置...");
        LocationSolver locationSolver = new LocationSolver();
        DriverLocation result = locationSolver.calculateUserLocation(deviceReport);
        
        System.out.println("计算结果: " + result);
    }
    
    
}