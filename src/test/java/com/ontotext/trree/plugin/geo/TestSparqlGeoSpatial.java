package com.ontotext.trree.plugin.geo;

import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

public class TestSparqlGeoSpatial extends AbstractSparqlGeoSpatial {

	@Override
	protected RepositoryConfig createRepositoryConfiguration() {
		return StandardUtils.createOwlimSe("owl-horst", false);
	}

}
