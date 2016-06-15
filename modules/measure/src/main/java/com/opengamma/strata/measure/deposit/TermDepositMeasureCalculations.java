/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.deposit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketData;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;

/**
 * Multi-scenario measure calculations for Term Deposit trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
class TermDepositMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingTermDepositProductPricer PRICER = DiscountingTermDepositProductPricer.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private TermDepositMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedTermDepositTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedTermDeposit product = trade.getProduct();
    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedTermDeposit product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    return PRICER.presentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedTermDepositTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedTermDeposit product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      ResolvedTermDeposit product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> bucketedPv01(
      ResolvedTermDepositTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedTermDeposit product = trade.getProduct();
    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedTermDeposit product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(
      ResolvedTermDepositTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedTermDeposit product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParRate(product, marketData.scenario(i)));
  }

  // par rate for one scenario
  private static double calculateParRate(
      ResolvedTermDeposit product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    return PRICER.parRate(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedTermDepositTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedTermDeposit product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParSpread(product, marketData.scenario(i)));
  }

  // par spread for one scenario
  private static double calculateParSpread(
      ResolvedTermDeposit product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    return PRICER.parSpread(product, provider);
  }

}
