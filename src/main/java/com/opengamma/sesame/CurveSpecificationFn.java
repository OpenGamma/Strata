/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.util.result.Result;

public interface CurveSpecificationFn {

  Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition);

  Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition, ZonedDateTime valuationTime);
}
