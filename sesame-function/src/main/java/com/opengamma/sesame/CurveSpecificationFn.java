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

/**
 * Function capable of providing a curve specification from a definition.
 */
public interface CurveSpecificationFn {

  /**
   * Finds the matching curve specification from a definition.
   * 
   * @param curveDefinition  the curve definition, not null
   * @return the curve specification, a failure result if not found
   */
  Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition);

  /**
   * Finds the matching curve specification from a definition.
   * 
   * @param curveDefinition  the curve definition, not null
   * @param valuationTime  the valuation time, not null
   * @return the curve specification, a failure result if not found
   */
  Result<CurveSpecification> getCurveSpecification(CurveDefinition curveDefinition, ZonedDateTime valuationTime);

}
