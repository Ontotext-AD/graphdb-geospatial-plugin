package com.ontotext.trree.plugin.geo;

import com.ontotext.test.utils.StandardUtils;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

public class TestOrderVarIntersectionOWLIM2872 extends AbstractOrderVarIntersectionOWLIM2872 {

	@Override
	protected RepositoryConfig createRepositoryConfiguration() {
		return StandardUtils.createOwlimSe("empty");
	}

}
