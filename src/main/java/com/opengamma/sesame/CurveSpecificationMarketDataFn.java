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
 * Gets the market data required for a curve specification.
 */
public interface CurveSpecificationMarketDataFn {

  Result<MarketDataValues> requestData(CurveSpecification curveSpecification);
}
