/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.security;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.Security;

/**
 * Multi-scenario measure calculations for simple security trades and positions.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class SecurityMeasureCalculations {

  // restricted constructor
  private SecurityMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyScenarioArray presentValue(
      Security security,
      double quantity,
      ScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(security, quantity, marketData.scenario(i)));
  }

  //-------------------------------------------------------------------------
  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      Security security,
      double quantity,
      MarketData marketData) {

    QuoteId id = QuoteId.of(security.getSecurityId().getStandardId());
    double price = marketData.getValue(id);
    return security.getInfo().getPriceInfo().calculateMonetaryAmount(quantity, price);
  }

}
