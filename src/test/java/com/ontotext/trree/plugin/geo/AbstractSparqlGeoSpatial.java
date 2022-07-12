package com.ontotext.trree.plugin.geo;

import com.ontotext.graphdb.Config;
import com.ontotext.test.TemporaryLocalFolder;
import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import com.ontotext.test.utils.Utils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * OWLIM-SE
 */
public abstract class AbstractSparqlGeoSpatial extends SingleRepositoryFunctionalTest {
	// Copied from Plugin. The test shouldn't use the plugin implementation
	private static class GeoSpatial {
		public static final String NAMESPACE = "http://www.ontotext.com/owlim/geo#";
		public static final String GEO_NAMESPACE = "http://www.w3.org/2003/01/geo/wgs84_pos#";

		public static final IRI LAT;
		public static final IRI LONG;

		static {
			ValueFactory factory = SimpleValueFactory.getInstance();

			LAT = factory.createIRI(GEO_NAMESPACE, "lat");
			LONG = factory.createIRI(GEO_NAMESPACE, "long");
		}
	}

	@ClassRule
	public static TemporaryLocalFolder tmpFolder = new TemporaryLocalFolder();

	@BeforeClass
	public static void setWorkDir() {
		System.setProperty("graphdb.home.work", String.valueOf(tmpFolder.getRoot()));
		Config.reset();
	}

	@AfterClass
	public static void resetWorkDir() {
		System.clearProperty("graphdb.home.work");
		Config.reset();
	}

	@Test
	public void sameAsPropertyDuringQueriesBug() throws Exception {
		// in order to reproduce it, just change the behaviour of Var.clone() to use v.setBinding() instead
		// of members copy (except the EqSet) // skip Swiftowlim since it doesnt use our query evaluation
		// engine

		final boolean PRINT = false;

		RepositoryConnection connection = null;
		TupleQueryResult result = null;

		try {
			connection = getRepository().getConnection();
			connection.begin();

			ValueFactory valueFactory = getRepository().getValueFactory();

			IRI s1 = valueFactory.createIRI("s:1");
			IRI type = valueFactory.createIRI("http://neuinfo.org/1167");
			connection.add(s1, RDFS.SUBCLASSOF, type);

			Literal someLabel = valueFactory.createLiteral("Some label");
			connection.add(s1, RDFS.LABEL, someLabel);

			IRI prop1 = valueFactory.createIRI("http://ex.org/ex#prop1");
			IRI prop2 = valueFactory.createIRI("http://ex.org/ex#prop2");
			IRI geo = valueFactory.createIRI("http://ex.org/ex#geo");
			IRI geo2 = valueFactory.createIRI("http://ex.org/ex#geo2");
			connection.add(s1, prop1, geo);
			connection.add(s1, prop2, valueFactory.createLiteral("value2"));
			connection.add(geo, GeoSpatial.LAT, valueFactory.createLiteral("10"));
			connection.add(geo, GeoSpatial.LONG, valueFactory.createLiteral("11"));
			connection.add(geo2, GeoSpatial.LAT, valueFactory.createLiteral("20"));
			connection.add(geo2, GeoSpatial.LONG, valueFactory.createLiteral("21"));
			connection.add(prop1, OWL.SAMEAS, prop2);

			connection.commit();

			Update indexCreated = connection.prepareUpdate(QueryLanguage.SPARQL,
					"insert data {_:a1 <http://www.ontotext.com/owlim/geo#createIndex> _:a2 . }");
			indexCreated.execute();
			//assertTrue("index not created", indexCreated.execute());

			String sparqlQueryProp1 = "PREFIX rdfs: <" + RDFS.NAMESPACE + "> " + "PREFIX geo: <"
					+ GeoSpatial.GEO_NAMESPACE + "> " + "PREFIX otgeo: <" + GeoSpatial.NAMESPACE + "> "
					+ "PREFIX ns: <http://ex.org/ex#> " + "SELECT ?s ?geo " + "WHERE { "
					+ " ?s rdfs:subClassOf <http://neuinfo.org/1167>. " + " ?s <" + prop1.toString()
					+ "> ?geo . \n" + " ?geo otgeo:nearby (10.0 11.0 10) . " + "} ";

			String sparqlQueryProp2 = "PREFIX rdfs: <" + RDFS.NAMESPACE + "> " + "PREFIX geo: <"
					+ GeoSpatial.GEO_NAMESPACE + "> " + "PREFIX otgeo: <" + GeoSpatial.NAMESPACE + "> "
					+ "PREFIX ns: <http://ex.org/ex#> " + "SELECT ?s ?geo " + "WHERE { "
					+ " ?s rdfs:subClassOf <http://neuinfo.org/1167>. " + " ?s <" + prop2.toString()
					+ "> ?geo . \n" + " ?geo otgeo:nearby (10.0 11.0 10) . " + "} ";
			TupleQuery tq = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryProp1);
			// try with prop1
			int count = 0;
			result = tq.evaluate();
			while (result.hasNext()) {
				count++;
				BindingSet set = result.next();
				System.out.println(set.toString());
			}
			// Should get one result, with prop1 and ZERO results means that some var has InvalidBinding
			// flag set
			assertTrue("Should have 1 results, with prop1 but found " + count, count == 1);

			// now try the prop2
			tq = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryProp2);
			count = 0;
			result = tq.evaluate();
			while (result.hasNext()) {
				count++;
				BindingSet set = result.next();
				if(PRINT)
					System.out.println(set);
			}
			// Should get one result, with prop2 and ZERO results means that some var has InvalidBinding
			// flag set
			assertTrue("Should have 1 results, with prop2 but found " + count, count == 1);
		} finally {
			Utils.close(result);
			Utils.close(connection);
		}
	}
}
