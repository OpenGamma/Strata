/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.bond.DiscountingCapitalIndexedBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;

/**
 * Multi-scenario measure calculations for capital indexed bond trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class CapitalIndexedBondMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final CapitalIndexedBondMeasureCalculations DEFAULT = new CapitalIndexedBondMeasureCalculations(
      DiscountingCapitalIndexedBondTradePricer.DEFAULT);
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedCapitalIndexedBondTrade}.
   */
  private final DiscountingCapitalIndexedBondTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedCapitalIndexedBondTrade}
   */
  CapitalIndexedBondMeasureCalculations(
      DiscountingCapitalIndexedBondTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesScenarioMarketData ratesMarketData,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return CurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.presentValue(trade, ratesProvider, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedCapitalIndexedBondTrade trade,
      RatesScenarioMarketData ratesMarketData,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedCapitalIndexedBondTrade trade,
      RatesScenarioMarketData ratesMarketData,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return ScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesScenarioMarketData ratesMarketData,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.currencyExposure(trade, ratesProvider, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesScenarioMarketData ratesMarketData,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return CurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currentCash(trade, ratesProvider);
  }

}
