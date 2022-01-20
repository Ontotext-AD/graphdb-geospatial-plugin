package com.ontotext.trree.plugin.geo;

import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public abstract class AbstractPluginGeoSpatial extends SingleRepositoryFunctionalTest {
	// Copied from plugin. Test should not use implementation but only SPARQL.
	private static final double EARTH_AVERAGE_RADIUS_KM = 6371.009;

	private static boolean useUpdate;
	private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private static final String GEO_LAT = GEO_NS + "lat";
	private static final String GEO_LONG = GEO_NS + "long";
	static IRI PointClass = SimpleValueFactory.getInstance().createIRI(GEO_NS + "Point");
	static IRI propLat = SimpleValueFactory.getInstance().createIRI(GEO_LAT);
	static IRI propLon = SimpleValueFactory.getInstance().createIRI(GEO_LONG);
	static IRI gn_name = SimpleValueFactory.getInstance().createIRI("http://www.geonames.org/ontology#name");
	static IRI gn_featureCode = SimpleValueFactory.getInstance().createIRI("http://www.geonames.org/ontology#featureCode");
	static IRI gn_Airport = SimpleValueFactory.getInstance().createIRI("http://www.geonames.org/ontology#S.AIRP");
	static IRI gn_medical = SimpleValueFactory.getInstance().createIRI("http://www.geonames.org/ontology#S.CTRM");

	@Parameters(name = "useUpdate = {0}")
	public static List<Object[]> getParameters() {
		return Arrays.<Object[]> asList(new Object[] { true }, new Object[] { false });
	}

	public AbstractPluginGeoSpatial(boolean useUpdate) {
		AbstractPluginGeoSpatial.useUpdate = useUpdate;
	}

	private boolean createIndex() throws RDF4JException {
		String query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
				+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
				+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
				+ " PREFIX gn: <http://www.geonames.org/ontology#>\n\n"
				+ (useUpdate ? "INSERT DATA" : "ASK")
				+ " {_:b1 <http://www.ontotext.com/owlim/geo#createIndex> _:b2 }";

		RepositoryConnection connection = getRepository().getConnection();
		try {
			boolean reindexed = false;
			if (useUpdate) {
				connection.begin();
				Update update = connection.prepareUpdate(QueryLanguage.SPARQL, query);
				update.execute();
				connection.commit();
			} else {
				BooleanQuery q = connection.prepareBooleanQuery(QueryLanguage.SPARQL, query);
				boolean result = q.evaluate();
				
				assertTrue(result);
			}

			return reindexed;
		} finally {
			connection.close();
		}
	}

	@Before
	public void setUp() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			// add data grid
			int count = 1;
			float baseLon = 50f;
			float baseLat = -5f;
			connection.begin();
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					count++;
					addEntry(connection, "" + count, "obj" + count, baseLat + kmToDegrees(i * 1.0f), baseLon
							+ kmToDegrees(j * 1.0f), gn_Airport);
				}
			}
			connection.commit();
			connection.close();
	
			try {
				boolean reindexed = createIndex();
				System.out.println("createIndex " + ((reindexed) ? "success" : "failure/no datata"));
	
				getRepository().shutDown();
				getRepository().init();
				connection = getRepository().getConnection();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally { 
			connection.close();
		}
	}

	private float kmToDegrees(float distance) {
		return (float) Math.toDegrees(distance / EARTH_AVERAGE_RADIUS_KM);
	}

	private void addEntry(RepositoryConnection connection, String obj, String name, float lat, float lon, IRI kind)
			throws RepositoryException {
			IRI entry = vf.createIRI("http://test.org#" + obj);
			connection.add(entry, RDF.TYPE, PointClass);
			connection.add(entry, gn_name, vf.createLiteral(name));
			connection.add(entry, propLat, vf.createLiteral("" + lat));
			connection.add(entry, propLon, vf.createLiteral("" + lon));
			connection.add(entry, gn_featureCode, kind);
	}

	@Test
	public void testNearByUsingExtBindings() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			String query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ " PREFIX gn: <http://www.geonames.org/ontology#>\n" + "  \n" + " SELECT *\n"
					+ " WHERE  {   \n" + " ?base gn:name ?locationName .\n" + " ?base geo:lat ?latBase .\n"
					+ " ?base geo:long ?lonBase .\n"
					+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(?latBase ?lonBase ?distance) .  \n"
					+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
					+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";
			try {
				TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				q.setBinding("locationName", vf.createLiteral("obj13"));
				q.setBinding("distance", vf.createLiteral("1mi"));
				int count = 0;
				TupleQueryResult result = q.evaluate();
				while (result.hasNext()) {
					BindingSet set = result.next();
					List<String> list = result.getBindingNames();
					for (String name : list) {
						Binding b = set.getBinding(name);
						assertTrue(b != null);
						assertTrue(b.getName() != null);
						assertTrue(b.getValue() != null);
					}
					count++;
				}
				result.close();
				assertTrue("q1 should be 9 but count=" + count, count == 9);

				q.setBinding("locationName", vf.createLiteral("obj2"));
				q.setBinding("distance", vf.createLiteral("1"));
				count = 0;
				result = q.evaluate();
				while (result.hasNext()) {
					BindingSet set = result.next();
					List<String> list = result.getBindingNames();
					for (String name : list) {
						Binding b = set.getBinding(name);
						assertTrue(b != null);
						assertTrue(b.getName() != null);
						assertTrue(b.getValue() != null);
					}
					count++;
				}
				assertTrue("q2 should be 3 but count=" + count, count == 3);
			} catch (MalformedQueryException mfe) {
				mfe.printStackTrace();
				fail();
			} catch (QueryEvaluationException mfe) {
				mfe.printStackTrace();
				fail();
			}
		} finally {
			connection.close();
		}
	}

	@Test
	public void testNearByUsingConstants() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			String query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ " PREFIX gn: <http://www.geonames.org/ontology#>\n" + "  \n" + " SELECT *\n"
					+ " WHERE  {   \n" + " ?base gn:name \"obj13\" .\n" + " ?base geo:lat ?latBase .\n"
					+ " ?base geo:long ?lonBase .\n"
					+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(?latBase ?lonBase \"1mi\") .  \n"
					+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
					+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";
			try {
				TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				int count = 0;
				TupleQueryResult result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("Q1 should be 9 but count=" + count, count == 9);

				query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
						+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
						+ " PREFIX gn: <http://www.geonames.org/ontology#>\n" + "  \n" + " SELECT *\n"
						+ " WHERE  {   \n" + " ?base gn:name \"obj2\" .\n" + " ?base geo:lat ?latBase .\n"
						+ " ?base geo:long ?lonBase .\n"
						+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(?latBase ?lonBase 1) .  \n"
						+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
						+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";

				q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				count = 0;
				result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("Q2 should be 3 but count=" + count, count == 3);

				query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
						+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
						+ " PREFIX gn: <http://www.geonames.org/ontology#>\n" + "  \n" + " SELECT *\n"
						+ " WHERE  {   \n"
						+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(-5.0 50.0 1) .  \n"
						+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
						+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";
				q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				count = 0;
				result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("Q3 should be 3 but count=" + count, count == 3);

				query = "\n"
						+ " PREFIX co: <http://www.geonames.org/countries/#>\n"
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
						+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
						+ " PREFIX gn: <http://www.geonames.org/ontology#>\n"
						+ "  \n"
						+ " SELECT *\n"
						+ " WHERE  {   \n"
						+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(-4.991007 50.008995 \"1mi\") .  \n"
						+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
						+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";
				count = 0;
				q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("Q4 should be 9 but count=" + count, count == 9);

			} catch (MalformedQueryException mfe) {
				mfe.printStackTrace();
				fail();
			} catch (QueryEvaluationException mfe) {
				mfe.printStackTrace();
				fail();
			}
		} finally {
			connection.close();
		}
	}

	@Test
	public void testWithinUsingExtBindings() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			String query = "\n"
					+ " PREFIX co: <http://www.geonames.org/countries/#>\n"
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ " PREFIX gn: <http://www.geonames.org/ontology#>\n"
					+ "  \n"
					+ " SELECT *\n"
					+ " WHERE  {   \n"
					+ " ?base gn:name ?locationName .\n"
					+ " ?base geo:lat ?latBase .\n"
					+ " ?base geo:long ?lonBase .\n"
					+ " ?base2 gn:name ?locationName2 .\n"
					+ " ?base2 geo:lat ?latBase2 .\n"
					+ " ?base2 geo:long ?lonBase2 .\n"
					+ "   ?link <http://www.ontotext.com/owlim/geo#within>(?latBase ?lonBase ?latBase2 ?lonBase2) .  \n"
					+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
					+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " }\n";
			try {
				TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				q.setBinding("locationName", vf.createLiteral("obj13"));
				q.setBinding("locationName2", vf.createLiteral("obj13"));
				int count = 0;
				TupleQueryResult result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("q1 should be 1 but count=" + count, count == 1);

				q.setBinding("locationName", vf.createLiteral("obj2"));
				q.setBinding("locationName2", vf.createLiteral("obj3"));
				count = 0;
				result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("q2 should be 2 but count=" + count, count == 2);

				q.setBinding("locationName", vf.createLiteral("obj2"));
				q.setBinding("locationName2", vf.createLiteral("obj13"));
				count = 0;
				result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
						}
						count++;
					}
				} finally {
					result.close();
				}
				assertTrue("q3 should be 4 but count=" + count, count == 4);
			} catch (MalformedQueryException mfe) {
				mfe.printStackTrace();
				fail();
			} catch (QueryEvaluationException mfe) {
				mfe.printStackTrace();
				fail();
			}
		} finally {
			connection.close();
		}
	}

	@Test
	public void testDistance() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			String query = "\n" + " PREFIX co: <http://www.geonames.org/countries/#>\n"
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ " PREFIX gn: <http://www.geonames.org/ontology#>\n"
					+ " PREFIX fn: <http://www.ontotext.com/owlim/geo#>\n" + "  \n"
					+ " SELECT ?link ?name ?lat ?lon \n" + " WHERE  {   \n"
					+ "   ?link <http://www.ontotext.com/owlim/geo#nearby>(-4.991007 50.008995 1) .  \n"
					+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
					+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n"
					+ "   filter( fn:distance(-4.991007,50.008995,?lat,?lon) < 1.0)\n"
					+ " } order by fn:distance(-4.991007,50.008995,?lat,?lon)\n";
			try {
				TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				int count = 0;
				TupleQueryResult result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
							System.out.print(b.getName() + "=" + b.getValue().toString() + "\t");
						}
						System.out.println();
						count++;
					}
				} finally {
					result.close();
				}
			} catch (MalformedQueryException mfe) {
				mfe.printStackTrace();
				fail();
			} catch (QueryEvaluationException mfe) {
				mfe.printStackTrace();
				fail();
			}
		} finally {
			connection.close();
		}
	}

	@Test
	public void testWithinPolygon() throws RepositoryException {
		RepositoryConnection connection = getRepository().getConnection();
		try {
			String query = "\n"
					+ " PREFIX co: <http://www.geonames.org/countries/#>\n"
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n"
					+ " PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ " PREFIX gn: <http://www.geonames.org/ontology#>\n"
					+ " PREFIX fn: <http://www.ontotext.com/owlim/geo#>\n"
					+ "  \n"
					+ " SELECT ?link ?name ?lat ?lon \n"
					+ " WHERE  {   \n"
					+ " ?base gn:name \"obj33\" .\n"
					+ " ?base geo:lat ?latBase .\n"
					+ " ?base geo:long ?lonBase .\n"
					+ " ?base2 gn:name \"obj25\" .\n"
					+ " ?base2 geo:lat ?latBase2 .\n"
					+ " ?base2 geo:long ?lonBase2 .\n"
					+ " ?base3 gn:name \"obj56\" .\n"
					+ " ?base3 geo:lat ?latBase3 .\n"
					+ " ?base3 geo:long ?lonBase3 .\n"
					+ "   ?link <http://www.ontotext.com/owlim/geo#within>(?latBase ?lonBase ?latBase2 ?lonBase2 ?latBase3 ?lonBase3) .  \n"
					+ "    ?link gn:name ?name .  \n" + "    ?link gn:featureCode gn:S.AIRP .\n"
					+ "    ?link geo:lat ?lat .\n" + "    ?link geo:long ?lon\n" + " } \n";
			HashSet<String> results = new HashSet<String>();
			results.add("obj25");
			results.add("obj35");
			results.add("obj34");
			results.add("obj45");
			try {
				TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
				int count = 0;
				TupleQueryResult result = q.evaluate();
				try {
					while (result.hasNext()) {
						BindingSet set = result.next();
						List<String> list = result.getBindingNames();
						for (String name : list) {
							Binding b = set.getBinding(name);
							assertTrue(b != null);
							assertTrue(b.getName() != null);
							assertTrue(b.getValue() != null);
							System.out.print(b.getName() + "=" + b.getValue().toString() + "\t");
							if ("name".equals(b.getName())) {
								String v = ((Literal) b.getValue()).getLabel();
								assertTrue("unexpected result" + b.getValue(), results.remove(v));
							}
						}
						System.out.println();
						count++;
					}
					assertTrue("missing results ", results.isEmpty());
				} finally {
					result.close();
				}
				// check for explicit lookups when obj25, obj35, obj34, obj45 are used

				for (int i = 1; i <= 100; i++) {
					if (i == 25 || i == 35 || i == 34 || i == 45) {
						q.setBinding("name", vf.createLiteral("obj" + i));
						result = q.evaluate();
						try {
							assertTrue("shoud have a result", result.hasNext());
							result.next();
							assertFalse("shoud not have more than 1 result", result.hasNext());
						} finally {
							result.close();
						}
					} else {
						connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
						q.setBinding("name", vf.createLiteral("obj" + i));
						result = q.evaluate();
						try {
							assertFalse("shoud not have match", result.hasNext());
						} finally {
							result.close();
						}
					}
				}

			} catch (MalformedQueryException mfe) {
				mfe.printStackTrace();
				fail();
			} catch (QueryEvaluationException mfe) {
				mfe.printStackTrace();
				fail();
			}
		} finally {
			connection.close();
		}
	}

}
