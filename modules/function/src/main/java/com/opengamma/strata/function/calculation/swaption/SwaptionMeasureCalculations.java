/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.market.view.SwaptionVolatilities;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionCashParYieldProductPricer;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionPhysicalProductPricer;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.SettlementType;

/**
 * Multi-scenario measure calculations for Swap trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class SwaptionMeasureCalculations {

  /**
   * The pricer to use for physical swaptions.
   */
  private static final VolatilitySwaptionPhysicalProductPricer PHYSICAL = VolatilitySwaptionPhysicalProductPricer.DEFAULT;
  /**
   * The pricer to use for cash par-yield swaptions.
   */
  private static final VolatilitySwaptionCashParYieldProductPricer CASH_PAR_YIELD =
      VolatilitySwaptionCashParYieldProductPricer.DEFAULT;

  // restricted constructor
  private SwaptionMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedSwaptionTrade trade,
      CalculationMarketData marketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    ResolvedSwaption product = trade.getProduct();
    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i), swaptionMarketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedSwaption product,
      MarketData marketData,
      SwaptionMarketData swaptionMarketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    SwaptionVolatilities volatilities = swaptionMarketData.volatilities(product.getIndex());
    if (product.getSwaptionSettlement().getSettlementType() == SettlementType.PHYSICAL) {
      return PHYSICAL.presentValue(product, provider, volatilities);
    } else {
      return CASH_PAR_YIELD.presentValue(product, provider, volatilities);
    }
  }

}
