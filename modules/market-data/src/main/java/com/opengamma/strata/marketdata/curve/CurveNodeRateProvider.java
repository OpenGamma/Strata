/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.curve;

import com.opengamma.strata.marketdata.CalculationRequirements;

/**
 * A provider which returns a rate used when building the instrument for a curve node.
 */
public interface CurveNodeRateProvider {

  /**
   * Returns the market data required to provide a rate.
   *
   * @return the market data required to provide a rate
   */
  public abstract CalculationRequirements requirements();

  /**
   * Returns the rate.
   *
   * @param marketData  the market data required to provide the rate
   * @return the rate
   */
  public abstract double rate(CurveNodeMarketData marketData);
}
