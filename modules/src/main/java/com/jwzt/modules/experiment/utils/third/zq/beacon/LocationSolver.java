package com.jwzt.modules.experiment.utils.third.zq.beacon;

import java.util.*;

import com.jwzt.modules.experiment.domain.TakBeaconInfo;

public class LocationSolver {

  

    // 内部使用的平面坐标点
    private static class PointXY {
        double x, y;
        public PointXY(double x, double y) { this.x = x; this.y = y; }
    }

    private static final double EARTH_RADIUS = 6371000.0; // 米

    /**
     * 计算用户当前位置
     * @param distances Map<信标ID, 距离(米)>
     * @return 计算出的经纬度结果
     */
    public static DriverLocation calculateUserLocation(Map<String, Double> distances) {
        // 过滤掉没有距离数据的信标
    	BeaconFaultDetector beaconFaultDetector = new BeaconFaultDetector();
    	List<TakBeaconInfo> beacons = beaconFaultDetector.getBaseLocal();
    	
        List<TakBeaconInfo> validBeacons = new ArrayList<>();
        List<Double> validDistances = new ArrayList<>();

        for (TakBeaconInfo b : beacons) {
            if (distances.containsKey(b.getBeaconId())) {
                validBeacons.add(b);
                validDistances.add(distances.get(b.getBeaconId()));
            }
        }

        if (validBeacons.size() < 3) {
            throw new IllegalArgumentException("至少需要3个信标才能进行准确的二维定位");
        }

        // 1. 选取参考点（原点），通常选第一个信标
        TakBeaconInfo origin = validBeacons.get(0);

        // 2. 将所有信标投影到平面坐标系
        List<PointXY> projectedBeacons = new ArrayList<>();
        for (TakBeaconInfo b : validBeacons) {
            projectedBeacons.add(latLonToMeterXY(origin, b));
        }

        // 3. 使用迭代最小二乘法计算用户在平面上的位置 (x, y)
        PointXY userPosXY = solvePositionLeastSquares(projectedBeacons, validDistances);

        // 4. 将计算出的 (x, y) 反算回经纬度
        return meterXYToLatLon(origin, userPosXY, projectedBeacons, validDistances);
    }

    /**
     * 核心算法：通过梯度下降/迭代逼近用户位置
     */
    private static PointXY solvePositionLeastSquares(List<PointXY> points, List<Double> radii) {
        // 初始猜测位置：所有信标的质心（平均值）
        double x = 0, y = 0;
        for (PointXY p : points) {
            x += p.x;
            y += p.y;
        }
        x /= points.size();
        y /= points.size();

        // 迭代参数
        double learningRate = 0.1; // 学习率
        int iterations = 100;      // 迭代次数

        for (int k = 0; k < iterations; k++) {
            double dx_sum = 0;
            double dy_sum = 0;

            for (int i = 0; i < points.size(); i++) {
                PointXY p = points.get(i);
                double targetR = radii.get(i);

                // 计算当前猜测点到该信标的距离
                double dx = x - p.x;
                double dy = y - p.y;
                double currentDist = Math.sqrt(dx * dx + dy * dy);

                if (currentDist == 0) continue; // 防止除零

                // 误差 = 测量距离 - 当前计算距离
                // 我们希望移动点，使得 distance 接近 targetR
                double error = targetR - currentDist;

                // 向量计算：移动方向向量
                // (dx / currentDist) 是 cos, (dy / currentDist) 是 sin
                // 我们沿着连接线的方向移动，去修正误差
                dx_sum += (dx / currentDist) * error;
                dy_sum += (dy / currentDist) * error;
            }

            // 更新位置：取所有信标建议移动量的平均值
            // 如果误差是正的（距离不够），我们需要远离信标？
            // 实际上这里的逻辑是：如果 currentDist < targetR，我们要把点推远。
            // 简单的向量加减：NewPos = OldPos - Gradient
            // 这里我们用简化的力导向模型：如果太近就推开，太远就拉近
            
            x -= (dx_sum / points.size()) * learningRate * -1.0; 
            y -= (dy_sum / points.size()) * learningRate * -1.0;
            
            // 随着迭代，减小学习率以微调
            learningRate *= 0.98; 
        }

        return new PointXY(x, y);
    }

    /**
     * 投影：经纬度 -> 平面 XY (米)
     */
    private static PointXY latLonToMeterXY(TakBeaconInfo origin, TakBeaconInfo target) {
        double latRad = Math.toRadians(origin.getLatitude());
        
        // Y轴：纬度差对应的米数
        double dy = Math.toRadians(target.getLatitude() - origin.getLatitude()) * EARTH_RADIUS;
        
        // X轴：经度差对应的米数 (需修正纬度造成的收缩)
        double dx = Math.toRadians(target.getLongitude() - origin.getLongitude()) * EARTH_RADIUS * Math.cos(latRad);
        
        return new PointXY(dx, dy);
    }

    /**
     * 反投影：平面 XY (米) -> 经纬度
     * 同时计算残差（误差评估）
     */
    private static DriverLocation meterXYToLatLon(TakBeaconInfo origin, PointXY pos, List<PointXY> points, List<Double> radii) {
        double latRad = Math.toRadians(origin.getLatitude());

        // 反解纬度
        double latDiffRad = pos.y / EARTH_RADIUS;
        double newLat = origin.getLatitude() + Math.toDegrees(latDiffRad);

        // 反解经度
        double lonDiffRad = pos.x / (EARTH_RADIUS * Math.cos(latRad));
        double newLon = origin.getLongitude() + Math.toDegrees(lonDiffRad);

        // 计算平均误差（残差）
        double totalError = 0;
        for (int i = 0; i < points.size(); i++) {
            double dist = Math.sqrt(Math.pow(pos.x - points.get(i).x, 2) + Math.pow(pos.y - points.get(i).y, 2));
            totalError += Math.abs(dist - radii.get(i));
        }

        return new DriverLocation(newLat, newLon, totalError / points.size());
    }

    // --- 测试 Main 方法 ---
    public static void main(String[] args) {
        List<TakBeaconInfo> beacons = new ArrayList<>();
        // 模拟场景：以 (30, 120) 为原点的 100米范围
        // 1度经度约等于 111km -> 0.00001度 约等于 1.1米
        // 我们用更精确的计算生成测试数据
        
        TakBeaconInfo b1 = new TakBeaconInfo("B1", 30.00000, 120.00000); // (0, 0)
        TakBeaconInfo b2 = new TakBeaconInfo("B2", 30.00090, 120.00000); // (~100m, 0) 北
        TakBeaconInfo b3 = new TakBeaconInfo("B3", 30.00000, 120.00104); // (0, ~100m) 东
        
        beacons.add(b1);
        beacons.add(b2);
        beacons.add(b3);

        // 假设用户在正中间 (50m, 50m) 的位置
        // 真实经纬度大约是 (30.00045, 120.00052)
        
        Map<String, Double> measuredDistances = new HashMap<>();
        // 勾股定理计算理论距离: sqrt(50^2 + 50^2) ≈ 70.71
        measuredDistances.put("B1", 70.71); 
        measuredDistances.put("B2", 70.71); 
        measuredDistances.put("B3", 70.71);

        System.out.println("开始计算位置...");
        DriverLocation result = calculateUserLocation(measuredDistances);
        
        System.out.println("计算结果: " + result);
        System.out.println("推测用户位于这三个信标的中心区域。");
    }
}