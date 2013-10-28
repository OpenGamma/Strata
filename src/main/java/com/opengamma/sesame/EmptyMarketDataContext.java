/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * A MarketDataContext that contains no market data which can be used on
 * an initial request. As it has no market data, it simplifies the checks
 * to be done when market data is requested.
 */
public class EmptyMarketDataContext implements MarketDataContext {

  private final MarketDataResultGenerator _resultGenerator;

  public EmptyMarketDataContext(MarketDataResultGenerator resultGenerator) {
    _resultGenerator = resultGenerator;
  }

  @Override
  public MarketDataFunctionResult retrieveMarketData(Set<MarketDataRequirement> requiredMarketData) {

    // We can just flag everything as missing
    return _resultGenerator.marketDataResultBuilder()
        .missingData(requiredMarketData)
        .build();
  }

  @Override
  public MarketDataFunctionResult retrieveMarketData(MarketDataRequirement requiredMarketData) {
    return retrieveMarketData(ImmutableSet.of(requiredMarketData));
  }
}
