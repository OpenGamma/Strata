/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.google.common.collect.ImmutableSet;
import com.opengamma.financial.security.FinancialSecurity;

public class StandardMarketDataProvider implements MarketDataProvider {

  private final MarketDataContext _marketDataContext;

  public StandardMarketDataProvider(MarketDataContext marketDataContext) {
    _marketDataContext = marketDataContext;
  }

  @Override
  public MarketDataFunctionResult retrieveMarketData(FinancialSecurity security, String requiredData) {

    MarketDataRequirement requirement = new MarketDataRequirement() {
      // Some implementation for how we specify the requirement
    };
    return _marketDataContext.retrieveMarketData(ImmutableSet.of(requirement));
  }
}
