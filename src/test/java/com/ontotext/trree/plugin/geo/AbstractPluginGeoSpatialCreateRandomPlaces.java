package com.ontotext.trree.plugin.geo;

import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Krzysztof Sielski
 */
public abstract class AbstractPluginGeoSpatialCreateRandomPlaces extends SingleRepositoryFunctionalTest {

	private static final String q1 = "PREFIX omgeo: <http://www.ontotext.com/owlim/geo#>"
			+ "PREFIX cidoc: <http://erlangen-crm.org/current/>"
			+ "select * WHERE {"
			+ "     ?place omgeo:nearby('52.574472150718606' '17.008895874023438' '100') ."
			+ "} ";
	private static final String q2 = "PREFIX omgeo: <http://www.ontotext.com/owlim/geo#>"
			+ "PREFIX cidoc: <http://erlangen-crm.org/current/>"
			+ "select * WHERE {"
			+ "     ?place omgeo:nearby('52.574472150718606' '17.008895874023438' '100') ."
			+ "     ?place cidoc:P1_is_identified_by ?appellation. "
			+ "} ";
	private static final String q3 = "PREFIX omgeo: <http://www.ontotext.com/owlim/geo#>"
			+ "PREFIX cidoc: <http://erlangen-crm.org/current/>"
			+ "select * WHERE {"
			+ "     ?place omgeo:nearby('52.574472150718606' '17.008895874023438' '100') ."
			+ "     optional {?place cidoc:P1_is_identified_by ?appellation}. "
			+ "} ";

	@Test
	public void createRandomPlaces() throws Exception {
		executeQueries();
	}

	private void executeQueries() throws Exception {
		RepositoryConnection con = getRepository().getConnection();
		try {

			System.out.println("Executing query q1");
			executeQuery(q1, con);
			System.out.println("Executing query q2");
			executeQuery(q2, con);
			System.out.println("Executing query q3");
			executeQuery(q3, con);

		} finally {
			con.close();
		}
	}

	private void executeQuery(String query, RepositoryConnection con) {
		try {
			long time = System.currentTimeMillis();
			TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
			int resultCount = 0;
			while (result.hasNext()) {
				result.next();
				resultCount++;
			}
			time = System.currentTimeMillis() - time;
			System.out.printf("Result count: %d in %fs.\n", resultCount, time / 1000.0);
			assertTrue("Query time too big: " + time, time < 2500);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Before
	public void initRepository() throws Exception {
		RepositoryConnection con = getRepository().getConnection();
		try {
			con.begin();
			long id = 0;
			int RANDOM_PLACES = 10000;
			
			if (useRemoteRepositoryManager())
				RANDOM_PLACES=RANDOM_PLACES/10;

			Random r = new Random(0xdeadbeefl);
			IRI P_lat = vf.createIRI("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
			IRI P_long = vf.createIRI("http://www.w3.org/2003/01/geo/wgs84_pos#long");
			IRI P_is_ident_by = vf.createIRI("http://erlangen-crm.org/current/P1_is_identified_by");

			// add random places
			for (int i = 0; i < RANDOM_PLACES; i++) {
				IRI placeUri = vf.createIRI("random:place." + id++);
				con.add(placeUri, P_lat, vf.createLiteral(Float.toString(52f + r.nextFloat())));
				con.add(placeUri, P_long, vf.createLiteral(Float.toString(17f + r.nextFloat())));
				con.add(placeUri, P_is_ident_by, vf.createLiteral(Long.toString(id++)));
			}
			con.commit();
			con.begin();

			// add random objects with appellations
			for (int i = 0; i < RANDOM_PLACES; i++) {
				con.add(vf.createIRI("random:" + id++), P_is_ident_by, vf.createLiteral(Long.toString(id++)));
				if (i % 10000 == 0) {
					System.out.println("inserted statements:" + i);
					con.commit();
					con.begin();
				}
			}
			con.commit();
			con.begin();

			// create index
			String createGeoIndexQuery = "PREFIX ontogeo: <http://www.ontotext.com/owlim/geo#> ASK { _:b1 ontogeo:createIndex _:b2. }";
			System.out.println("Create geo index: " + con.prepareBooleanQuery(QueryLanguage.SPARQL, createGeoIndexQuery).
					evaluate());
			con.commit();
		} finally {
			con.close();
		}
	}

}
