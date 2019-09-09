package com.ontotext.trree.plugin.geo;

import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

public class TestPluginGeoSpatial extends AbstractPluginGeoSpatial {
	public TestPluginGeoSpatial(boolean useUpdate) {
		super(useUpdate);
	}
	
	@Override
	protected RepositoryConfig createRepositoryConfiguration() {
		return StandardUtils.createOwlimSe("rdfs");
	}
}
