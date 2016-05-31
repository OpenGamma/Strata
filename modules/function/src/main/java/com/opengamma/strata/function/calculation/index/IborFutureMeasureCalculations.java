/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioResult;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.function.calculation.RatesMarketData;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Multi-scenario measure calculations for Ibor Future trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class IborFutureMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingIborFutureTradePricer PRICER = DiscountingIborFutureTradePricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private IborFutureMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedIborFutureTrade trade,
      RatesScenarioMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        index -> calculateParSpread(trade, marketData.scenario(index)));
  }

  // par spread for one scenario
  private static double calculateParSpread(
      ResolvedIborFutureTrade trade,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    double settlementPrice = settlementPrice(trade, marketData);
    return PRICER.parSpread(trade, provider, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedIborFutureTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedIborFutureTrade trade,
      RatesMarketData marketData) {

    // mark to model
    RatesProvider provider = marketData.ratesProvider();
    double settlementPrice = settlementPrice(trade, marketData);
    return PRICER.presentValue(trade, provider, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedIborFutureTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(trade, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      ResolvedIborFutureTrade trade,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, provider);
    return provider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurrencyParameterSensitivities> bucketedPv01(
      ResolvedIborFutureTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(trade, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedIborFutureTrade trade,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, provider);
    return provider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // gets the settlement price
  private static double settlementPrice(ResolvedIborFutureTrade trade, RatesMarketData marketData) {
    StandardId standardId = trade.getProduct().getSecurityId().getStandardId();
    QuoteId id = QuoteId.of(standardId, FieldName.SETTLEMENT_PRICE);
    return marketData.getMarketData().getValue(id) / 100;  // convert market quote to value needed
  }

}
