/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.AbstractCurveSpecification;
import com.opengamma.util.result.Result;

/**
 * Function capable of providing a curve specification from a definition.
 */
public interface CurveSpecificationFn {

  /**
   * Finds the matching curve specification from a definition.
   * 
   *
   * @param env the execution environment
   * @param curveDefinition  the curve definition, not null
   * @return the curve specification, a failure result if not found
   */
  Result<AbstractCurveSpecification> getCurveSpecification(Environment env, AbstractCurveDefinition curveDefinition);

}
