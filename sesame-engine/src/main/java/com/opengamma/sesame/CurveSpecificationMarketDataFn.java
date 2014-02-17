/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.Result;

/**
 * Function capable of getting the market data required for a curve specification.
 */
public interface CurveSpecificationMarketDataFn {

  /**
   * Requests the data for a curve specification.
   * 
   * @param curveSpecification  the curve specification, not null
   * @return the market data values, a failure result if not found
   */
  Result<MarketDataValues> requestData(CurveSpecification curveSpecification);

}
