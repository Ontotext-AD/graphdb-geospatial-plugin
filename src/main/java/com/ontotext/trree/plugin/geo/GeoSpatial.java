package com.ontotext.trree.plugin.geo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class GeoSpatial {
	public static final String NAMESPACE = "http://www.ontotext.com/owlim/geo#";

	public static final IRI NEARBY;
	public static final IRI WITHIN;
	public static final IRI DISTANCE;
	public static final IRI CREATE_INDEX;

	public static final String GEO_NAMESPACE = "http://www.w3.org/2003/01/geo/wgs84_pos#";

	public static final IRI LAT;
	public static final IRI LONG;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		NEARBY = factory.createIRI(NAMESPACE, "nearby");
		WITHIN = factory.createIRI(NAMESPACE, "within");
		DISTANCE = factory.createIRI(NAMESPACE, "distance");
		CREATE_INDEX = factory.createIRI(NAMESPACE, "createIndex");

		LAT = factory.createIRI(GEO_NAMESPACE, "lat");
		LONG = factory.createIRI(GEO_NAMESPACE, "long");
	}
}
