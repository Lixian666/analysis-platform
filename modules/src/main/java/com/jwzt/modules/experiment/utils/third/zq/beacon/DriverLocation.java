package com.jwzt.modules.experiment.utils.third.zq.beacon;

public class DriverLocation {
    private double lat;
    private double lon;
    private double errorMargin; // 平均误差(米)

    public DriverLocation(double lat, double lon, double errorMargin) {
        this.lat = lat;
        this.lon = lon;
        this.errorMargin = errorMargin;
    }

    @Override
    public String toString() {
        return String.format("Lat: %.6f, Lon: %.6f (误差: %.2fm)", lat, lon, errorMargin);
    }

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getErrorMargin() {
		return errorMargin;
	}

	public void setErrorMargin(double errorMargin) {
		this.errorMargin = errorMargin;
	}
    
    
}
