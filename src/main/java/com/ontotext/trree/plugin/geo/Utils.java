package com.ontotext.trree.plugin.geo;

/**
 * Common constants, calculations and conversions.
 */
public class Utils {

	public static final double MIN_LAT_RADIANS = -Math.PI / 2; // -90 degrees
	public static final double MAX_LAT_RADIANS = Math.PI / 2; // +90 degrees
	public static final double MIN_LON_RADIANS = -Math.PI; // -180 degrees
	public static final double MAX_LON_RADIANS = Math.PI; // +180 degrees

	public static final double ONE_REVOLUTION_RADIANS = Math.PI * 2; // 360 degrees

	public static final double EARTH_AVERAGE_RADIUS_KM = 6371.009;

	public static final double KM_TO_MILES_RATIO = 1.609344;

	public static final String KM_SUFFIX = "km";

	public static final String MILE_SUFFIX = "mi";

	public static final double milesToKilometres(double distanceKm) {
		return distanceKm * KM_TO_MILES_RATIO;
	}

	/**
	 * Convert great circle distance in kilometres into angular distance in radians.
	 * 
	 * @param distanceKm
	 *            distance in km
	 * @return The angular distance
	 */
	public static double distanceKmToAngular(double distanceKm) {
		return distanceKm / Utils.EARTH_AVERAGE_RADIUS_KM;
	}

	public static boolean isMiles(String distance) {
		return distance.toLowerCase().endsWith(MILE_SUFFIX);
	}

	public static boolean isKilometres(String distance) {
		return distance.toLowerCase().endsWith(KM_SUFFIX);
	}

	/**
	 * Compute the angular distance (radians) between two points specified in spherical polar coordinates
	 * (degrees).
	 * 
	 * @param lat1
	 *            The latitude of the first point in degrees.
	 * @param lon1
	 *            The longitude of the first point in degrees.
	 * @param lat2
	 *            The latitude of the second point in degrees.
	 * @param lon2
	 *            The longitude of the second point in degrees.
	 * @return The angular distance in radians
	 */
	public static double angularDistance(float lat1, float lon1, float lat2, float lon2) {
		double dlon = Math.toRadians(lon2 - lon1);
		double dlat = Math.toRadians(lat2 - lat1);
		double a = Math.sin(dlat / 2);
		a *= a;
		double b = Math.sin(dlon / 2);
		b *= b;
		a = a + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * b;
		return 2 * Math.asin(Math.min(1, Math.sqrt(a)));
	}

	/**
	 * Indicates if the point p_lat, p_long is within the 'rectangle' specified by the meridians r_west,
	 * r_east and the parallels r_south and r_north. r_west must be more westerly relative to r_east, although
	 * it can have a greater longitude, i.e. when spanning +/-180 Rectangles with north < south will not
	 * contain any point.
	 * 
	 * @param r_south
	 *            Most southerly extent of rectangle (degrees)
	 * @param r_west
	 *            Most westerly extent of rectangle (degrees)
	 * @param r_north
	 *            Most northerly extent of rectangle (degrees)
	 * @param r_east
	 *            Most easterly extent of rectangle (degrees)
	 * @param p_lat
	 * @param p_long
	 * @return true if the point lies within the 'rectangle'
	 */
	public static boolean within(double r_south, double r_west, double r_north, double r_east, double p_lat,
			double p_long) {
		return intersects(r_south, r_west, r_north, r_east, p_lat, p_long, p_lat, p_long);
	}

	/**
	 * Indicates if the two 'rectangles' intersect, i.e. contain 1 or more distinct points that are in both
	 * 'rectangles'.
	 * 
	 * @param r1_south
	 *            Most southerly extent of rectangle 1 (degrees)
	 * @param r1_west
	 *            Most westerly extent of rectangle 1 (degrees)
	 * @param r1_north
	 *            Most northerly extent of rectangle 1 (degrees)
	 * @param r1_east
	 *            Most easterly extent of rectangle 1 (degrees)
	 * @param r2_south
	 *            Most southerly extent of rectangle 2 (degrees)
	 * @param r2_west
	 *            Most westerly extent of rectangle 2 (degrees)
	 * @param r2_north
	 *            Most northerly extent of rectangle 2 (degrees)
	 * @param r2_east
	 *            Most easterly extent of rectangle 2 (degrees)
	 * @return true if the 'rectangles' intersect
	 */
	public static boolean intersects(double r1_south, double r1_west, double r1_north, double r1_east,
			double r2_south, double r2_west, double r2_north, double r2_east) {

		if (r1_east < r1_west) {
			// r1 spans +/-180 meridian
			// So break this in to two rectangles on each side of +/-180 meridian
			// and recursively call this function twice
			return intersects(r1_south, r1_west, r1_north, +180, r2_south, r2_west, r2_north, r2_east)
					|| intersects(r1_south, -180, r1_north, r1_east, r2_south, r2_west, r2_north, r2_east);
		}
		if (r2_east < r2_west) {
			// r1 spans +/-180 meridian
			// So rather than do the splitting like for r1, just swap r1 for r2 and call again
			return intersects(r2_south, r2_west, r2_north, r2_east, r1_south, r1_west, r1_north, r1_east);
		}
		if (r1_north < r2_south) {
			return false;
		}
		if (r2_north < r1_south) {
			return false;
		}
		if (r1_east < r2_west) {
			return false;
		}
		if (r2_east < r1_west) {
			return false;
		}
		return true;
	}
}
