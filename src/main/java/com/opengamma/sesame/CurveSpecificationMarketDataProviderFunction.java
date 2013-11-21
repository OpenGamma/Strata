/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.sesame.marketdata.MarketDataFunctionResult;

/**
 * Gets the market data required for a curve specification.
 */
public interface CurveSpecificationMarketDataProviderFunction {

  MarketDataFunctionResult requestData(CurveSpecification curveSpecification);
}
