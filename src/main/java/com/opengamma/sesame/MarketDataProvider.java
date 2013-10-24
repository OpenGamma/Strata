/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.google.common.collect.ImmutableSet;
import com.opengamma.financial.security.FinancialSecurity;

public class MarketDataProvider implements MarketDataProviderFunction {

  @Override
  public MarketDataFunctionResult retrieveMarketData(MarketDataContext marketDataContext, FinancialSecurity security, String requiredData) {

    MarketDataRequirement requirement = new MarketDataRequirement() {
      // Some implementation for how we specify the requirement
    };
    return marketDataContext.retrieveMarketData(ImmutableSet.of(requirement));
  }
}
