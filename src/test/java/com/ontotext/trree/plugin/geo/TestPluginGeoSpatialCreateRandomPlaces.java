package com.ontotext.trree.plugin.geo;

import com.ontotext.test.utils.OwlimSeRepositoryDescription;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

public class TestPluginGeoSpatialCreateRandomPlaces extends AbstractPluginGeoSpatialCreateRandomPlaces {
	@Override
	protected RepositoryConfig createRepositoryConfiguration() {
		OwlimSeRepositoryDescription descr = new OwlimSeRepositoryDescription();
		descr.getOwlimSailConfig().setRuleset("empty");
		descr.getOwlimSailConfig().setQueryTimeout(60);
		return descr.getRepositoryConfig();
	}
}
