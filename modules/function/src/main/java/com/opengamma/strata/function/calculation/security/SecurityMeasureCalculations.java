/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.security;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityTrade;

/**
 * Multi-scenario measure calculations for simple security trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
class SecurityMeasureCalculations {

  // restricted constructor
  private SecurityMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      SecurityTrade trade,
      Security security,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, security, marketData.scenario(i)));
  }

  //-------------------------------------------------------------------------
  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      SecurityTrade trade,
      Security security,
      MarketData marketData) {

    QuoteKey key = QuoteKey.of(security.getSecurityId().getStandardId());
    double price = marketData.getValue(key);
    return security.getInfo().getPriceInfo().calculateMonetaryValue(trade.getQuantity(), price);
  }

}
