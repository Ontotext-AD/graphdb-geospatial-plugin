package com.ontotext.trree.plugin.geo;

import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Helper for loading Sesame functions on the fly.
 */
public class FunctionLoader {
    private static Logger LOGGER = LoggerFactory.getLogger(FunctionLoader.class);

    public static void loadFunctionsInPackage(String pkg) {
        final FunctionRegistry functionRegistry = FunctionRegistry.getInstance();
        final ServiceLoader<Function> sl = ServiceLoader.load(Function.class, FunctionLoader.class.getClassLoader());
        sl.reload();

        int count = 0;
        for (Function f : sl) {
            if (f.getClass().getPackage().getName().startsWith(pkg)) {
                functionRegistry.add(f);
                LOGGER.debug("Registering function {} from package {}.", f.getURI(), pkg);
                count++;
            }
        }

        LOGGER.info("Registered {} functions from package {}.", count, pkg);
    }
}
