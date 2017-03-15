/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.DiscountingBondFutureTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Multi-scenario measure calculations for Bond Future trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class BondFutureMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final BondFutureMeasureCalculations DEFAULT = new BondFutureMeasureCalculations(
      DiscountingBondFutureTradePricer.DEFAULT);
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedBondFutureTrade}.
   */
  private final DiscountingBondFutureTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBondFutureTrade}
   */
  BondFutureMeasureCalculations(
      DiscountingBondFutureTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).discountingProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    // mark to model
    double settlementPrice = settlementPrice(trade, discountingProvider);
    return tradePricer.presentValue(trade, discountingProvider, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).discountingProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).discountingProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  DoubleScenarioArray parSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return DoubleScenarioArray.of(
        marketData.getScenarioCount(),
        i -> parSpread(trade, marketData.scenario(i).discountingProvider()));
  }

  // par spread for one scenario
  double parSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    double settlementPrice = settlementPrice(trade, discountingProvider);
    return tradePricer.parSpread(trade, discountingProvider, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates unit price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return DoubleScenarioArray.of(
        marketData.getScenarioCount(),
        i -> unitPrice(trade, marketData.scenario(i).discountingProvider()));
  }

  // unit price for one scenario
  double unitPrice(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    // mark to model
    return tradePricer.price(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).discountingProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    double settlementPrice = settlementPrice(trade, discountingProvider);
    return tradePricer.currencyExposure(trade, discountingProvider, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // gets the settlement price
  private double settlementPrice(ResolvedBondFutureTrade trade, LegalEntityDiscountingProvider discountingProvider) {
    StandardId standardId = trade.getProduct().getSecurityId().getStandardId();
    QuoteId id = QuoteId.of(standardId, FieldName.SETTLEMENT_PRICE);
    return discountingProvider.data(id) / 100;  // convert market quote to value needed
  }

}
