package com.jwzt.modules.experiment.utils.third.zq.beacon;

import java.util.*;
import com.jwzt.modules.experiment.domain.TakBeaconInfo;

public class BeaconFaultDetector {
    
	// 地球半径（米）
    private final double EARTH_RADIUS = 6371000.0;

    /**
     * 核心方法：查找异常信标
     * @param beacons 所有的信标列表
     * @param distances 设备上报的距离 Map (Key: ID, Value: 距离-米)
     * @param tolerance 容忍的误差阈值 (如 2.0 米)
     */
    public List<String> detectFaultyBeacons(List<TakBeaconInfo> beacons, Map<String, Double> distances, double tolerance) {
        if (distances.size() < 4) {
            System.out.println("警告：信标数量过少，无法进行冲突对比分析。");
            return new ArrayList<>();
        }

        // 记录每个信标的冲突次数
        Map<String, Integer> conflictScore = new HashMap<>();
        for (TakBeaconInfo b : beacons) conflictScore.put(b.getBeaconId(), 0);

        // 两两对比
        for (int i = 0; i < beacons.size(); i++) {
            for (int j = i + 1; j < beacons.size(); j++) {
            	TakBeaconInfo b1 = beacons.get(i);
            	TakBeaconInfo b2 = beacons.get(j);

                Double d1 = distances.get(b1.getBeaconId());
                Double d2 = distances.get(b2.getBeaconId());

                if (d1 == null || d2 == null) continue;

                // 计算两个经纬度点之间的精确物理距离（米）
                double actualDist = calculateFlatDistance(b1, b2);

                // 几何一致性检验：利用三角不等式
                // 1. d1 + d2 >= actualDist (距离和不能小于信标间距)
                // 2. |d1 - d2| <= actualDist (距离差不能超过信标间距)
                boolean isViolated = (d1 + d2 < actualDist - tolerance) || 
                                     (Math.abs(d1 - d2) > actualDist + tolerance);

                if (isViolated) {
                    conflictScore.put(b1.getBeaconId(), conflictScore.get(b1.getBeaconId()) + 1);
                    conflictScore.put(b2.getBeaconId(), conflictScore.get(b2.getBeaconId()) + 1);
                }
            }
        }

        
        
        // 筛选出冲突次数最多的信标
        return findMaxConflictIds(conflictScore);
    }

    /**
     	* 高精度距离计算：
     	* 针对小范围（10km内），先投影到平面再算欧氏距离，比直接算球面距离更精准
     */
    private double calculateFlatDistance(TakBeaconInfo b1, TakBeaconInfo b2) {
        double latMid = Math.toRadians((b1.getLatitude() + b2.getLatitude()) / 2.0);
        
        // 纬度每度对应的米数
        double dy = Math.toRadians(b1.getLatitude() - b2.getLatitude()) * EARTH_RADIUS;
        // 经度每度对应的米数（随纬度变化）
        double dx = Math.toRadians(b1.getLongitude() - b2.getLongitude()) * EARTH_RADIUS * Math.cos(latMid);

        return Math.sqrt(dx * dx + dy * dy);
    }

    private List<String> findMaxConflictIds(Map<String, Integer> scores) {
        List<String> suspects = new ArrayList<>();
        int max = 0;
        for (int s : scores.values()) if (s > max) max = s;

        // 如果最高冲突次数为0，说明全部正常
        if (max == 0) return suspects;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() == max) {
                suspects.add(entry.getKey());
            }
        }
        return suspects;
    }

    // --- 测试 ---
    public static void main(String[] args) {
    	
    	BeaconFaultDetector beaconFaultDetector = new BeaconFaultDetector();
        Map<String, Double> deviceReport = beaconFaultDetector.getRightPront();

        List<String> results = beaconFaultDetector.detect(deviceReport, 2.0);
    }
    
    /**
     * 检测信标距离是否合法（只检测信标距离过短的问题）
     * @param distances  所有信标的距离至少要给5个信标的距离进行比较
     * @param tolerance	 允许误差单位 （米）
     * @return
     */
    public List<String> detect(Map<String, Double> distances, double tolerance) {
    	List<TakBeaconInfo> beaconList = getBaseLocal();
    	List<String> results = detectFaultyBeacons(beaconList, distances, tolerance);
    	return results;
    	
    }
    
    /*
     * 
     	初始化每个信标经纬度
     */
    public List<TakBeaconInfo> getBaseLocal(){

            List<TakBeaconInfo> beaconList = new ArrayList<>();

            beaconList.add(new TakBeaconInfo("1918FD013C29", 109.5832668,24.4052388));
            beaconList.add(new TakBeaconInfo("1918FD013865", 109.5831927,24.4053638));
            beaconList.add(new TakBeaconInfo("1918FD01394A", 109.5831898,24.4053731));
            beaconList.add(new TakBeaconInfo("1918FD013B67", 109.5831640,24.4054317));
            beaconList.add(new TakBeaconInfo("1918FD0119B6", 109.5831350,24.4055047));
            beaconList.add(new TakBeaconInfo("1918FD013B17", 109.5831061,24.4053319));
            beaconList.add(new TakBeaconInfo("1918FD013C52", 109.5830666,24.4054322));
            beaconList.add(new TakBeaconInfo("1918FD013C35", 109.5830601,24.4054473));
            
            beaconList.add(new TakBeaconInfo("1918FD01379F", 109.5828288,24.4054109));
            beaconList.add(new TakBeaconInfo("1918FD01385B", 109.5828288,24.4054109));
            beaconList.add(new TakBeaconInfo("1918FD013B82", 109.5828681,24.4053337));
            beaconList.add(new TakBeaconInfo("1918FD0137D2", 109.5828801,24.4053035));
            beaconList.add(new TakBeaconInfo("1918FD01397D", 109.5829014,24.4052217));
            beaconList.add(new TakBeaconInfo("1918FD013937", 109.5829014,24.4052217));
            
            return beaconList;
    }
    
    /*
     * 测试用异常数据
     */
    public Map<String, Double> getErrorPront() {
        Map<String, Double> deviceReport = new HashMap<>();
        deviceReport.put("1918FD013C29", 0.99);
        //deviceReport.put("1918FD013B17", 3.190);
        deviceReport.put("1918FD013B17", 1.190);
        deviceReport.put("1918FD013A47", 4.060);
        deviceReport.put("1918FD01394A", 12.470); 
        deviceReport.put("1918FD013865", 12.610); 
        
        deviceReport.put("1918FD013C52", 13.760); 
        deviceReport.put("1918FD013B67", 14.970); 
        deviceReport.put("1918FD013C35", 15.540); 
        deviceReport.put("1918FD01397D", 22.390); 
        //deviceReport.put("1918FD01397D", 20.390);
        deviceReport.put("1918FD0137D2", 22.890); 
        return deviceReport;
    }
    
    /*
     * 测试用正常数据
     */
    public Map<String, Double> getRightPront() {
        Map<String, Double> deviceReport = new HashMap<>();
        deviceReport.put("1918FD013C35", 13.38);
        deviceReport.put("1918FD01394A", 20.34);
        deviceReport.put("1918FD0119B6", 3.93); 
        deviceReport.put("1918FD0119BA", 2.79); 
        deviceReport.put("1918FD013B67", 13.5); 

        return deviceReport;
    }
}