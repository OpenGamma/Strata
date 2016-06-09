/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.market.product.swaption.SwaptionVolatilities;
import com.opengamma.strata.measure.rate.RatesMarketData;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionTradePricer;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;

/**
 * Multi-scenario measure calculations for Swap trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class SwaptionMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final VolatilitySwaptionTradePricer PRICER = VolatilitySwaptionTradePricer.DEFAULT;

  // restricted constructor
  private SwaptionMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    return CurrencyValuesArray.of(
        ratesMarketData.getScenarioCount(),
        i -> calculatePresentValue(trade, ratesMarketData.scenario(i), swaptionMarketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedSwaptionTrade trade,
      RatesMarketData ratesMarketData,
      SwaptionMarketData swaptionMarketData) {

    RatesProvider provider = ratesMarketData.ratesProvider();
    SwaptionVolatilities volatilities = swaptionMarketData.volatilities(trade.getProduct().getIndex());
    return PRICER.presentValue(trade, provider, volatilities);
  }

}
