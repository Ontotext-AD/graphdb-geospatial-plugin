package com.ontotext.trree.plugin.geo;

public class Polygon {
	private float pointsLat[], pointsLon[];
	private int sz = 0;
	// private boolean bAutoClose = false;
	private boolean bReady = false;
	private float minlat = 0, maxlat = 0, minlon = 0, maxlon = 0;

	public Polygon(int numpoints) {
		sz = 0;
		pointsLat = new float[numpoints + 1];
		pointsLon = new float[numpoints + 1];
	}

	public void add(float lat, float lon) {
		if (bReady) {
			throw new RuntimeException("no more point should be added");
		}
		if (sz == 0) {
			pointsLat[sz] = minlat = maxlat = lat;
			pointsLon[sz] = minlon = maxlon = lon;
		} else {
			pointsLat[sz] = lat;
			pointsLon[sz] = lon;
			if (minlat > lat) {
				minlat = lat;
			}
			if (maxlat < lat) {
				maxlat = lat;
			}
			if (minlon > lon) {
				minlon = lon;
			}
			if (maxlon < lon) {
				maxlon = lon;
			}
		}
		sz++;
		// last one was asserted
		if (sz == pointsLat.length - 1) {
			if (pointsLat[0] != lat && pointsLon[0] != lon) {
				pointsLat[sz] = pointsLat[0];
				pointsLon[sz] = pointsLon[0];
				// bAutoClose = true;
				sz++;
			}
			bReady = true;
		}
	}

	public boolean contains(float lat, float lon) {
		if (lat < minlat || lat > maxlat || lon < minlon || lon > maxlon) {
			return false;
		}
		int crossings = 0;
		for (int i = 0; i < sz - 1; i++) {
			double slope = (pointsLat[i + 1] - pointsLat[i]) / (pointsLon[i + 1] - pointsLon[i]);
			boolean cond1 = (pointsLon[i] <= lon) && (lon < pointsLon[i + 1]);
			boolean cond2 = (pointsLon[i + 1] <= lon) && (lon < pointsLon[i]);
			boolean cond3 = lat < slope * (lon - pointsLon[i]) + pointsLat[i];
			if ((cond1 || cond2) && cond3) {
				crossings++;
			}
		}
		return (crossings % 2 != 0);
	}

	public float getMinLat() {
		return minlat;
	}

	public float getMaxLat() {
		return maxlat;
	}

	public float getMinLong() {
		return minlon;
	}

	public float getMaxLong() {
		return maxlon;
	}

	public boolean isReady() {
		return bReady;
	}
}
