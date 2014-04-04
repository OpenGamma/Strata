/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;

/**
 * Function capable of getting the market data required for a curve specification.
 */
public interface CurveSpecificationMarketDataFn {

  /**
   * Requests the data for a curve specification.
   *
   * @param env the execution environment
   * @param curveSpecification  the curve specification, not null
   * @return the market data values, a failure result if not found
   */
  Result<Map<ExternalIdBundle, Double>> requestData(Environment env, CurveSpecification curveSpecification);
}
