package com.ontotext.trree.plugin.geo;

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTreeWithCoords;
import com.ontotext.trree.sdk.*;
import com.ontotext.trree.sdk.Entities.Scope;
import gnu.trove.TLongHashSet;
import gnu.trove.TLongObjectProcedure;
import gnu.trove.TLongProcedure;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Damyan
 *
 */
public class GeoSpatialPlugin extends PluginBase implements PatternInterpreter, ListPatternInterpreter, UpdateInterpreter {
    static {
        FunctionLoader.loadFunctionsInPackage("com.ontotext.trree.plugin.geo");
    }

	private static final String STORAGE_FILE = "storage";
	private static final String TEMP_SUFFIX = ".temp";

	private static final Logger Logger = LoggerFactory.getLogger(GeoSpatialPlugin.class);

	private final ReadWriteLock indexGuard = new ReentrantReadWriteLock();
	private RTreeWithCoords index = null;

	private long idLat;
	private long idLong;
	private long idCreateIndex;
	private long idNearby;
	private long idWithin;


	// Strange class, isn't List<Integer> enough?
	private static class GeoStatBase {
		final LongArrayList subjects = new LongArrayList(16);

		public int size() {
			return subjects.size();
		}

		public long fromIndex(int index) {
			return subjects.get(index);
		}
	}

	private static class GeoStatWithin extends GeoStatBase implements TLongProcedure {
		@Override
		public boolean execute(long id) {
			subjects.add(id);
			return true;
		}
	}

	private static class GeoStatNearBy extends GeoStatBase implements TLongObjectProcedure<Rectangle> {
		private float aroundLat = 0f;
		private float aroundLon = 0f;
		private float distance = 0;

		public GeoStatNearBy(float lat, float lon, float dist) {
			aroundLat = lat;
			aroundLon = lon;
			distance = dist;
		}

		@Override
		public boolean execute(long id, Rectangle match) {
			if (Utils.angularDistance(aroundLat, aroundLon, match.maxX, match.maxY) < distance) {
				subjects.add(id);
			}
			return true;
		}
	}

	private static class GeoStatWithinPoly extends GeoStatBase implements TLongObjectProcedure<Rectangle> {
		private Polygon polygon;

		public GeoStatWithinPoly(Polygon poly) {
			polygon = poly;
		}

		@Override
		public boolean execute(long id, Rectangle match) {
			if (polygon.contains(match.maxX, match.maxY)) {
				subjects.add(id);
			}
			return true;
		}
	}

	private static void boundingCoordinates(Rectangle[] res, float degLat, float degLon, float distancekm) {
		float radLat = (float) Math.toRadians(degLat);
		float radLon = (float) Math.toRadians(degLon);
		if (distancekm < 0f) {
			throw new IllegalArgumentException("distancekm can not be negative");
		}

		// angular distance in radians on a great circle
		float radDist = (float) Utils.distanceKmToAngular(distancekm);

		float minLat = radLat - radDist;
		float maxLat = radLat + radDist;

		double minLon, maxLon;
		if (minLat > Utils.MIN_LAT_RADIANS && maxLat < Utils.MAX_LAT_RADIANS) {
			double deltaLon = Math.asin(Math.sin(radDist) / Math.cos(radLat));
			boolean lonOver180 = false;
			boolean lonUnderMinus180 = false;
			minLon = radLon - deltaLon;
			if (minLon < Utils.MIN_LON_RADIANS) {
				minLon += Utils.ONE_REVOLUTION_RADIANS;
				lonUnderMinus180 = true;
			}
			maxLon = radLon + deltaLon;
			if (maxLon > Utils.MAX_LON_RADIANS) {
				maxLon -= Utils.ONE_REVOLUTION_RADIANS;
				lonOver180 = true;
			}
			if (lonOver180 || lonUnderMinus180) {
				res[0] = new Rectangle((float) Math.toDegrees(minLat), 180, (float) Math.toDegrees(maxLat),
						(float) Math.toDegrees(minLon));
				res[1] = new Rectangle((float) Math.toDegrees(minLat), (float) Math.toDegrees(maxLon),
						(float) Math.toDegrees(maxLat), -180);
			} else {
				res[0] = new Rectangle((float) Math.toDegrees(minLat), (float) Math.toDegrees(minLon),
						(float) Math.toDegrees(maxLat), (float) Math.toDegrees(maxLon));
				res[1] = null;
			}
		} else {
			// a pole is within the distance
			minLat = (float) Math.max(minLat, Utils.MIN_LAT_RADIANS);
			maxLat = (float) Math.min(maxLat, Utils.MAX_LAT_RADIANS);
			minLon = Utils.MIN_LON_RADIANS;
			maxLon = Utils.MAX_LON_RADIANS;
			res[0] = new Rectangle((float) Math.toDegrees(minLat), (float) Math.toDegrees(minLon),
					(float) Math.toDegrees(maxLat), (float) Math.toDegrees(maxLon));
			res[1] = null;
		}

	}

	@Override
	public String getName() {
		return "geospatial";
	}

	@Override
	public void initialize(InitReason initReason, PluginConnection pluginConnection) {
		// load index from disk (if present)
		if (getStorageFile().exists()) {
			try {
				restoreIndex();
			} catch (IOException e) {
				Logger.error("Failed to restore geospatial index", e);
			}
		}

		idLat = pluginConnection.getEntities().resolve(GeoSpatial.LAT);
		idLong = pluginConnection.getEntities().resolve(GeoSpatial.LONG);
		idCreateIndex = pluginConnection.getEntities().put(GeoSpatial.CREATE_INDEX, Scope.SYSTEM);
		idNearby = pluginConnection.getEntities().put(GeoSpatial.NEARBY, Scope.SYSTEM);
		idWithin = pluginConnection.getEntities().put(GeoSpatial.WITHIN, Scope.SYSTEM);

		loadPredicates(pluginConnection);

        final FunctionRegistry functionRegistry = FunctionRegistry.getInstance();
        final ServiceLoader<Function> sl = ServiceLoader.load(Function.class, FunctionLoader.class.getClassLoader());
        sl.reload();


        Logger.debug("Plugin:" + getName() + " initialized");
	}

	private void loadPredicates(PluginConnection pluginConnection) {
		idLat = pluginConnection.getEntities().put(GeoSpatial.LAT, Scope.DEFAULT);
		idLong = pluginConnection.getEntities().put(GeoSpatial.LONG, Scope.DEFAULT);
	}

	private long getLatitudeId() {
		return idLat;
	}

	private long getLongtitudeId() {
		return idLong;
	}

	@Override
	public void shutdown(ShutdownReason shutdownReason) {
	}

	public File getStorageFile() {
		return new File(getDataDir() + File.separator + STORAGE_FILE);
	}

	public File getTempStorageFile() {
		return new File(getStorageFile() + TEMP_SUFFIX);
	}

	@Override
	public StatementIterator interpret(long subject, long predicate, long object, long context,
                                       PluginConnection pluginConnection, RequestContext requestContext) {
		if (com.ontotext.trree.sdk.Utils.match(predicate, idCreateIndex)) {
			Boolean result = createIndex(pluginConnection.getStatements(), pluginConnection.getEntities());
			return result ? StatementIterator.TRUE() : StatementIterator.FALSE();
		}
		return null;
	}

	@Override
	public StatementIterator interpret(long subject, long predicate, long[] objects, long context,
                                       PluginConnection pluginConnection, RequestContext requestContext) {
		// see if this is a predicate we support
		if (com.ontotext.trree.sdk.Utils.match(predicate, idNearby)) {
			return handleNearBy(subject, predicate, objects, context, pluginConnection.getStatements(), pluginConnection.getEntities());
		}
		if (com.ontotext.trree.sdk.Utils.match(predicate, idWithin)) {
			return handleWithin(subject, predicate, objects, context, pluginConnection.getStatements(), pluginConnection.getEntities());
		}

		return null;
	}

	private boolean createIndex(Statements statements, Entities entities) {
		indexGuard.writeLock().lock();
		try {

			if (getLongtitudeId() != 0 && getLatitudeId() != 0) {
				TLongHashSet indexed = new TLongHashSet();
				setFingerprint(0);
				index = new RTreeWithCoords();
				Properties prop = new Properties();
				prop.put("MaxNodeEntries", "10");
				prop.put("MinNodeEntries", "5");
				index.init(prop);
				// initialize
				int count = 0;
				Rectangle r = new Rectangle();
				StatementIterator iter = statements.get(0, getLatitudeId(), 0, 0);
				try {
					while (iter.next()) {
						long entry = iter.subject;
						long latCoord = iter.object;
						long longCoord = -1;
						StatementIterator iterLong = statements.get(entry, getLongtitudeId(), 0, 0);
						try {
							if (iterLong.next()) {
								longCoord = iterLong.object;
							}
							if (iterLong.next()) {
								Logger.warn("multiple latitudes found for node " + entry);
							}
						} finally {
							iterLong.close();
						}
						if (longCoord != -1) {
							// have valid coordinates for an entry
							try {
								Literal latLiteral = (Literal) entities.get(latCoord);
								Literal longLiteral = (Literal) entities.get(longCoord);
								float latDouble = Float.parseFloat(latLiteral.getLabel());
								float longDouble = Float.parseFloat(longLiteral.getLabel());
								r.minX = r.maxX = latDouble;
								r.minY = r.maxY = longDouble;

								if (!indexed.add(entry)) {
									Logger.warn("node " + entry + " already indexed");
								} else {
									index.add(r, entry);
									// update fingerprint
									long fp = getFingerprint();
									fp ^= Double.doubleToLongBits(latDouble);
									fp ^= Double.doubleToLongBits(longDouble);
									fp ^= entry;
									setFingerprint(fp);
									count++;
									if (count % 10000 == 0) {
										Logger.debug(count + " entries indexed so far (" + latDouble + ","
												+ longDouble + ", entry=" + entities.get(entry) + ")");
									}
								}
							} catch (NumberFormatException nfe) {
								// bad double value, skipping this entry
							} catch (ClassCastException cce) {
								// objects not literals, skipping this entry
							}
						} // if
					} // while
				} finally {
					iter.close();
				}
				Logger.debug(count + " entries indexed in total");
				Logger.debug("Persisting index...");
				if (false == index.checkConsistency()) {
					Logger.debug("RTree index inconsistent");
				}
				persistIndex();
				Logger.debug("Index persisted");
				return true;
			}
		} catch (IOException e) {
			Logger.error("Failed persisting the geospatial index to disk", e);
		} finally {
			indexGuard.writeLock().unlock();
		}
		return false;
	}

	public void persistIndex() throws IOException {
		getDataDir().mkdirs();

		// store index into a temporary file
		File storageFile = getStorageFile();
		File tempStorageFile = getTempStorageFile();
		tempStorageFile.delete();

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
				tempStorageFile)));
		index.save(out);
		out.close();

		// move temporary storage file onto the real one
		storageFile.delete();
		tempStorageFile.renameTo(storageFile);
	}

	public void restoreIndex() throws IOException {
		Logger.debug("Restoring geospatial index from disk");

		index = new RTreeWithCoords();
		Properties prop = new Properties();
		prop.put("MaxNodeEntries", "10");
		prop.put("MinNodeEntries", "5");
		index.init(prop);

		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(getStorageFile())));
			index.load(in);
		} catch (IOException e) {
			index = null;
			Logger.error("Failed restoring geospatial index", e);
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
		}

		Logger.debug("Geospatial index restored from disk");
	}

	/**
	 * handle the SPARQL List syntax patterns like: 1) ?subject owlimgeo:nearby(?lat ?lon ?limit) // * 2)
	 * ?subject owlimgeo:nearby(?location ?limit) where ?location should be connected with
	 * <http://www.w3.org/2003/01/geo/wgs84_pos#lat> and <http://www.w3.org/2003/01/geo/wgs84_pos#long> to the
	 * actual coordinates in decimal decrees as per geo specification that could be found at
	 * http://www.w3.org/2003/01/geo/
	 *
	 * @param subject
	 *            - could be bound or unbound - the location that is within the range defined by ?limit
	 * @param predicate
	 *            - owlim:near
	 * @param objects
	 * @param context
	 * @param statements
	 * @param entities
	 * @return
	 */
	private StatementIterator handleNearBy(long subject, long predicate, long[] objects, long context,
                                           Statements statements, Entities entities) {
		return createIterator(subject, predicate, objects, context, statements, entities, false);
	}

	/**
	 * handle the SPARQL List syntax patterns like: 1) ?subject owlimgeo:within(?lat1 ?lon1 ?lat2 ?lon2) // *
	 * 2) ?subject owlimgeo:within(?location1 ?location2) where ?location should be connected with
	 * <http://www.w3.org/2003/01/geo/wgs84_pos#lat> and <http://www.w3.org/2003/01/geo/wgs84_pos#long> to the
	 * actual coordinates in decimal decrees as per geo specification that could be found at
	 * http://www.w3.org/2003/01/geo/
	 *
	 * @param subject
	 *            - could be bound or unbound - the location that is within the range defined by ?limit
	 * @param predicate
	 *            - owlim:near
	 * @param objects
	 * @param context
	 * @return
	 */
	private StatementIterator handleWithin(long subject, long predicate, long[] objects, long context,
                                           Statements statements, Entities entities) {
		return createIterator(subject, predicate, objects, context, statements, entities, true);
	}

	@SuppressWarnings("unchecked")
	private StatementIterator createIterator(final long subject, final long predicate, final long[] objects,
                                             final long context, final Statements statements, final Entities entities,
                                             final boolean isWithinFlag) {

		indexGuard.readLock().lock();

		boolean haveValidStat = false;
		final GeoStatBase stat;

		Polygon poly = null;
		boolean badPoly = false;

		try {
			if (index == null) {
				return StatementIterator.EMPTY;
			}
			Rectangle result[] = new Rectangle[2];
			if (!isWithinFlag) {
				float latV = getVarAsDouble(entities, objects[0]);
				float longV = getVarAsDouble(entities, objects[1]);
				if (Double.isInfinite(latV) || Double.isNaN(latV) || Double.isInfinite(longV) || Double.isNaN(longV)) {
					return StatementIterator.EMPTY;
				}
				float radians = (float) Utils.distanceKmToAngular(getVarAsDouble(entities, objects[2]));
				float distancekm = getVarAsDouble(entities, objects[2]);

				boundingCoordinates(result, latV, longV, distancekm);
				stat = new GeoStatNearBy(latV, longV, radians);
				haveValidStat = true;
			} else {
				if (objects.length == 4) {
					// within rect
					float latMin = getVarAsDouble(entities, objects[0]);
					float longMin = getVarAsDouble(entities, objects[1]);
					float latMax = getVarAsDouble(entities, objects[2]);
					float longMax = getVarAsDouble(entities, objects[3]);

					// @todo: allow searches for bounding box passing 180th meridian
					result[0] = new Rectangle(latMin, longMin, latMax, longMax);
					result[1] = null;
					stat = new GeoStatWithin();
					haveValidStat = true;
				} else {
					if (objects.length % 2 != 0) {
						Logger.error("odd number of coordinate arguments passed to geo:within");
						return StatementIterator.EMPTY;
					}
					if (poly == null) {
						poly = new Polygon(objects.length / 2);
						for (int pt = 0; pt < objects.length; pt += 2) {
							float lat = getVarAsDouble(entities, objects[pt]);
							float lon = getVarAsDouble(entities, objects[pt + 1]);
							if (Float.isNaN(lat) || Float.isNaN(lon)) {
								badPoly = true;
								break;
							}
							poly.add(lat, lon);
						}
					}
					if (poly == null || badPoly || poly.isReady() == false) {
						return StatementIterator.EMPTY;
					}

					// handle the case when a simple polygon lookup is required
					if (subject != 0) {
						long entry = subject;
						StatementIterator iter = statements.get(entry, getLatitudeId(), 0, context);
						try {
							if (iter.next()) {
								float latLoc = getIdAsFloat(entities, iter.object);
								if (Float.isNaN(latLoc)) {
									return StatementIterator.EMPTY;
								}
								StatementIterator iterLong = statements.get(entry, getLongtitudeId(), 0, context);
								try {
									if (iterLong.next()) {
										float lonLoc = getIdAsFloat(entities, iterLong.object);
										if (Float.isNaN(latLoc)) {
											return StatementIterator.EMPTY;
										}
										if (poly.contains(latLoc, lonLoc)) {
											return StatementIterator.create(subject, -1, -1, -1);
										}
									}
								} finally {
									iterLong.close();
								}
							}
						} finally {
							iter.close();
						}
						return StatementIterator.EMPTY;
					}

					// @todo: allow searches for bounding box passing 180th meridian
					result[0] = new Rectangle(poly.getMinLat(), poly.getMinLong(), poly.getMaxLat(),
							poly.getMaxLong());
					result[1] = null;
					stat = new GeoStatWithinPoly(poly);
					haveValidStat = true;
				}
			}
			for (Rectangle element : result) {
				if (element == null) {
					continue;
				}
				if (stat instanceof TLongProcedure) {
					index.intersects(element, (TLongProcedure) stat);
				} else {
					index.intersects(element, (TLongObjectProcedure<Rectangle>) stat);
				}
			}
		} finally {
			indexGuard.readLock().unlock();
		}

		if (!haveValidStat) {
			return StatementIterator.EMPTY;
		}

		if (subject != 0) {
			if (!stat.subjects.contains(subject)) {
				return StatementIterator.EMPTY;
			}
			return StatementIterator.create(subject, -1, -1, -1);
		}

		return new StatementIterator(subject, predicate, objects[0], context) {

			int current = -1;

			@Override
			public boolean next() {
				if (++current >= stat.size()) {
					return false;
				}
				this.subject = entities.getClass(stat.fromIndex(current));
				return true;
			}
			@Override
			public void close() {
				current = stat.size();
			}
		};
	}

	private float getIdAsFloat(Entities entities, long id) {
		Value val = entities.get(id);
		if (val == null) {
			return Float.NaN;
		}
		if (!(val instanceof Literal)) {
			return Float.NaN;
		}
		try {
			return Float.parseFloat(((Literal) val).getLabel());
		} catch (NumberFormatException nfe) {
			return Float.NaN;
		}
	}

	private float getVarAsDouble(Entities entities, long id) {
		String value = com.ontotext.trree.sdk.Utils.getString(entities, id);
		if (value == null) {
			return Float.NaN;
		}
		double factor = 1.0;
		if (Utils.isKilometres(value)) {
			value = value.substring(0, value.length() - Utils.KM_SUFFIX.length());
		} else if (Utils.isMiles(value)) {
			value = value.substring(0, value.length() - Utils.MILE_SUFFIX.length());
			factor = Utils.KM_TO_MILES_RATIO;
		}
		try {
			return (float) (Double.parseDouble(value) * factor);
		} catch (NumberFormatException nfe) {
			return Float.NaN;
		}
	}

	@Override
	public double estimate(long subject, long predicate, long[] objects, long context,
                           PluginConnection pluginConnection, RequestContext requestContext) {

		for (long object : objects) {
			if (Entities.UNBOUND == object) {
				return Double.POSITIVE_INFINITY;
			}
		}

		if (subject == Entities.UNBOUND) {
			return 1.5;
		}

		return 1;
	}

	@Override
	public double estimate(long subject, long predicate, long object, long context, PluginConnection pluginConnection,
						   RequestContext requestContext) {
		return 1;
	}

	@Override
	public long[] getPredicatesToListenFor() {
		return new long[] { idCreateIndex };
	}

	@Override
	public boolean interpretUpdate(long subject, long predicate, long object, long context, boolean isAddition,
			boolean isExplicit, PluginConnection pluginConnection) {
		createIndex(pluginConnection.getStatements(), pluginConnection.getEntities());
		return true;
	}
}
