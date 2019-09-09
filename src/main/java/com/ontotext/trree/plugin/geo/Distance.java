package com.ontotext.trree.plugin.geo;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class Distance implements Function {
	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {

		if (args.length != 4) {
			throw new ValueExprEvaluationException(
					"ontogeo:distance requires exactly 4 arguments(lat1, lon1, lat2 lon2), got "
							+ args.length);
		}
		try {
			if (!(args[0] instanceof Literal)) {
				throw new ValueExprEvaluationException(
						"Invalid first argument for ontogeo:distance function: " + args[0]);
			}
			float lat1 = Float.parseFloat(((Literal) args[0]).getLabel());
			if (!(args[1] instanceof Literal)) {
				throw new ValueExprEvaluationException(
						"Invalid second argument for ontogeo:distance function: " + args[1]);
			}
			float lon1 = Float.parseFloat(((Literal) args[1]).getLabel());
			if (!(args[2] instanceof Literal)) {
				throw new ValueExprEvaluationException(
						"Invalid third argument for ontogeo:distance function: " + args[2]);
			}
			float lat2 = Float.parseFloat(((Literal) args[2]).getLabel());
			if (!(args[3] instanceof Literal)) {
				throw new ValueExprEvaluationException(
						"Invalid fourth argument for ontogeo:distance function: " + args[3]);
			}
			float lon2 = Float.parseFloat(((Literal) args[3]).getLabel());
			String retValue = Double.toString(Utils.angularDistance(lat1, lon1, lat2, lon2) * 6371f);
			return valueFactory.createLiteral(retValue, XMLSchema.FLOAT);
		} catch (NumberFormatException nfe) {
			return valueFactory.createLiteral(Double.toString(Double.POSITIVE_INFINITY), XMLSchema.FLOAT);
		}
	}

	@Override
	public String getURI() {
		return GeoSpatial.DISTANCE.stringValue();
	}

}
