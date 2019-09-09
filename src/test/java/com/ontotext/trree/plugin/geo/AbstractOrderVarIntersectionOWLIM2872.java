package com.ontotext.trree.plugin.geo;

import com.ontotext.test.functional.base.SingleRepositoryFunctionalTest;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public abstract class AbstractOrderVarIntersectionOWLIM2872 extends SingleRepositoryFunctionalTest {

	@Test
	public void londonGeoSpatial() throws Exception {
		RepositoryConnection conn = getRepository().getConnection();
		try {
			conn.prepareUpdate(QueryLanguage.SPARQL, ""
					+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
					+ "PREFIX dbr: <http://dbpedia.org/resource/>\n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX dbp-ont: <http://dbpedia.org/ontology/>\n"
					+ ""
					+ "\n"
					+ "INSERT DATA {\n"
					+ "  dbr:London wgs:lat \"51.507222222222225\"^^xsd:float , \"51.50853\"^^xsd:float .\n"
					+ "  dbr:London wgs:long \"-0.1275\"^^xsd:float , \"-0.12574\"^^xsd:float .\n"
					+ "\n"
					+ "  dbr:Gatwick_Airport a dbp-ont:Airport .\n"
					+ "  dbr:Gatwick_Airport wgs:lat \"51.14805555555556\"^^xsd:float , \"51.15609\"^^xsd:float .\n"
					+ "  dbr:Gatwick_Airport wgs:long \"-0.19027777777777777\"^^xsd:float , \"-0.17818\"^^xsd:float .\n"
					+ "  dbr:Gatwick_Airport rdf:rank \"0.01\"^^xsd:float .\n"
					+ "\n"
					+ "  dbr:Heathrow_Airport a dbp-ont:Airport .\n"
					+ "  dbr:Heathrow_Airport wgs:lat \"51.4775\"^^xsd:float .\n"
					+ "  dbr:Heathrow_Airport wgs:long \"-0.4613888888888889\"^^xsd:float .\n"
					+ "  dbr:Heathrow_Airport rdf:rank \"0.01\"^^xsd:float .\n"
					+ "\n"
					+ "  dbr:Andrewsfield_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:Andrewsfield_Aerodrome wgs:lat \"51.895\"^^xsd:float .\n"
					+ "  dbr:Andrewsfield_Aerodrome wgs:long \"0.44916666666666666\"^^xsd:float .\n"
					+ "  dbr:Andrewsfield_Aerodrome rdf:rank \"0.00\"^^xsd:float .\n"
					+ "\n"
					+ "  dbr:Lasham_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:Lashenden_Headcorn_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:Little_Gransden_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:London_Air_Park a dbp-ont:Airport .\n"
					+ "  dbr:London_Biggin_Hill_Airport a dbp-ont:Airport .\n"
					+ "  dbr:London_Gliding_Club a dbp-ont:Airport .\n"
					+ "  dbr:London_Heliport a dbp-ont:Airport .\n"
					+ "  dbr:Lullingstone_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:MDPGA_Wethersfield a dbp-ont:Airport .\n"
					+ "  dbr:Marden_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:North_Weald_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:Old_Warden_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:Panshanger_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:Penshurst_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Ashford a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Boreham a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Bourn a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Bradwell_Bay a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Castle_Camps a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Chailey a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Chipping_Ongar a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Deanland a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Detling a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Eastchurch a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Fairlop a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Gosfield a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Great_Dunmow a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Great_Sampford a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Hampstead_Norris a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Henley_on_Thames a dbp-ont:Airport .\n"
					+ "  dbr:RAF_High_Halden a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Hornchurch a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Kenley a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Oakley a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Sawbridgeworth a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Tempsford a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Twinwood_Farm a dbp-ont:Airport .\n"
					+ "  dbr:RAF_West_Malling a dbp-ont:Airport .\n"
					+ "  dbr:RAF_Westcott a dbp-ont:Airport .\n"
					+ "  dbr:RNAS_Kingsnorth a dbp-ont:Airport .\n"
					+ "  dbr:Redhill_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:Rochester_Airport_England a dbp-ont:Airport .\n"
					+ "  dbr:Shoreham_Airport a dbp-ont:Airport .\n"
					+ "  dbr:Stag_Lane_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:Stapleford_Aerodrome a dbp-ont:Airport .\n"
					+ "  dbr:White_Waltham_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:Wisley_Airfield a dbp-ont:Airport .\n"
					+ "  dbr:Wycombe_Air_Park a dbp-ont:Airport .\n"
					+ ""
					+ "}").execute();

			conn.prepareUpdate(QueryLanguage.SPARQL, ""
					+ "INSERT DATA {\n"
					+ "  _:a <http://www.ontotext.com/owlim/geo#createIndex> _:b ."
					+ "}").execute();

			String queryWithOntoExplain = ""
					+ "PREFIX wgs: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
					"\n" +
					"PREFIX omgeo: <http://www.ontotext.com/owlim/geo#>\n" +
					"\n" +
					"PREFIX dbp-ont: <http://dbpedia.org/ontology/>\n" +
					"\n" +
					"PREFIX om: <http://www.ontotext.com/owlim/>\n" +
					"\n" +
					"PREFIX dbr: <http://dbpedia.org/resource/>\n" +
					"\n" +
					"PREFIX onto: <http://www.ontotext.com/>\n" +
					"\n" +
					" \n" +
					"\n" +
					"SELECT distinct ?airport ?RR ?lat1 ?long1\n" +
					"\n" +
					"FROM onto:explain\n" +
					"\n" +
					"WHERE {\n" +
					"\n" +
					"	dbr:London wgs:lat ?lat ; wgs:long ?long . \n" +
					"\n" +
					"        ?airport omgeo:nearby(?lat ?long \"50mi\");\n" +
					"\n" +
					"           a dbp-ont:Airport .\n" +
					"\n" +
					"} ORDER BY DESC(?RR)";
			String srch = "FROM onto:explain";
			int expl = queryWithOntoExplain.indexOf(srch);
			String query = queryWithOntoExplain.substring(0, expl) + queryWithOntoExplain.substring(expl + srch.length() + 2);

			TupleQueryResult iter = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();

			int results = 0;
			while (iter.hasNext()) {
				System.out.println(iter.next());
				results++;
			}
			System.out.println(results + " result(s)");

			String explainPlan = ((Literal) conn.prepareTupleQuery(QueryLanguage.SPARQL, queryWithOntoExplain).evaluate().next().getBinding("plan").getValue()).getLabel();

			assertTrue("No results! Most lilkely the GeoSpatialPlugin triple pattern is placed between <lat> and <long>:\n" + explainPlan, results > 0);

		}
		finally {
			conn.close();
		}
	}

}
