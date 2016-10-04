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
import com.opengamma.strata.pricer.bond.DiscountingFixedCouponBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Multi-scenario measure calculations for fixed coupon bond trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FixedCouponBondMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FixedCouponBondMeasureCalculations DEFAULT = new FixedCouponBondMeasureCalculations(
      DiscountingFixedCouponBondTradePricer.DEFAULT);
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFixedCouponBondTrade}.
   */
  private final DiscountingFixedCouponBondTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedFixedCouponBondTrade}
   */
  FixedCouponBondMeasureCalculations(
      DiscountingFixedCouponBondTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).discountingProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.presentValue(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).discountingProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).discountingProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).discountingProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.currencyExposure(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currentCash(trade, marketData.scenario(i).discountingProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.currentCash(trade, discountingProvider.getValuationDate());
  }

}
