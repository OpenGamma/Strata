/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toCurrencyValuesArray;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;
import com.opengamma.strata.market.view.SwaptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionCashParYieldProductPricer;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionPhysicalProductPricer;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;
import com.opengamma.strata.product.swaption.SwaptionTrade;

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
      SwaptionTrade trade,
      ExpandedSwaption product,
      CalculationMarketData marketData,
      SwaptionVolatilitiesKey volKey) {

    List<CurrencyAmount> result = new ArrayList<>();
    for (int i = 0; i < marketData.getScenarioCount(); i++) {
      RatesProvider provider = ratesProvider(marketData, i);
      SwaptionVolatilities volatilities = provider.data(volKey);
      if (product.getSwaptionSettlement().getSettlementType() == SettlementType.PHYSICAL) {
        result.add(PHYSICAL.presentValue(product, provider, volatilities));
      } else {
        result.add(CASH_PAR_YIELD.presentValue(product, provider, volatilities));
      }
    }
    return result.stream().collect(toCurrencyValuesArray());
  }

  // creates a RatesProvider
  private static RatesProvider ratesProvider(CalculationMarketData marketData, int index) {
    return new MarketDataRatesProvider(new SingleCalculationMarketData(marketData, index));
  }

}
