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
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.BlackBondFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.bond.BlackBondFutureVolatilities;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;

/**
 * Multi-scenario measure calculations for Bond Future Option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class BondFutureOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final BondFutureOptionMeasureCalculations DEFAULT = new BondFutureOptionMeasureCalculations(
      BlackBondFutureOptionMarginedTradePricer.DEFAULT);
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedBondFutureOptionTrade}.
   */
  private final BlackBondFutureOptionMarginedTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBondFutureOptionTrade}
   */
  BondFutureOptionMeasureCalculations(
      BlackBondFutureOptionMarginedTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SecurityId securityId = trade.getProduct().getUnderlyingFuture().getSecurityId();
    return CurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            legalEntityMarketData.scenario(i).discountingProvider(),
            optionMarketData.scenario(i).volatilities(securityId)));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    // mark to model
    double settlementPrice = settlementPrice(trade, discountingProvider);
    BlackBondFutureVolatilities normalVols = checkBlackVols(volatilities);
    return tradePricer.presentValue(trade, discountingProvider, normalVols, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SecurityId securityId = trade.getProduct().getUnderlyingFuture().getSecurityId();
    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedSum(
            trade,
            legalEntityMarketData.scenario(i).discountingProvider(),
            optionMarketData.scenario(i).volatilities(securityId)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    BlackBondFutureVolatilities normalVols = checkBlackVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, discountingProvider, normalVols);
    return discountingProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SecurityId securityId = trade.getProduct().getUnderlyingFuture().getSecurityId();
    return ScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(
            trade,
            legalEntityMarketData.scenario(i).discountingProvider(),
            optionMarketData.scenario(i).volatilities(securityId)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    BlackBondFutureVolatilities normalVols = checkBlackVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, discountingProvider, normalVols);
    return discountingProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates unit price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SecurityId securityId = trade.getProduct().getUnderlyingFuture().getSecurityId();
    return DoubleScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> unitPrice(
            trade,
            legalEntityMarketData.scenario(i).discountingProvider(),
            optionMarketData.scenario(i).volatilities(securityId)));
  }

  // unit price for one scenario
  double unitPrice(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    // mark to model
    BlackBondFutureVolatilities normalVols = checkBlackVols(volatilities);
    return tradePricer.price(trade, discountingProvider, normalVols);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SecurityId securityId = trade.getProduct().getUnderlyingFuture().getSecurityId();
    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            legalEntityMarketData.scenario(i).discountingProvider(),
            optionMarketData.scenario(i).volatilities(securityId)));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    double settlementPrice = settlementPrice(trade, discountingProvider);
    return tradePricer.currencyExposure(trade, discountingProvider, volatilities, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // gets the settlement price
  private double settlementPrice(ResolvedBondFutureOptionTrade trade, LegalEntityDiscountingProvider discountingProvider) {
    StandardId standardId = trade.getProduct().getSecurityId().getStandardId();
    QuoteId id = QuoteId.of(standardId, FieldName.SETTLEMENT_PRICE);
    return discountingProvider.data(id);
  }

  // ensure volatilities are Black
  private BlackBondFutureVolatilities checkBlackVols(BondFutureVolatilities volatilities) {
    if (volatilities instanceof BlackBondFutureVolatilities) {
      return (BlackBondFutureVolatilities) volatilities;
    }
    throw new IllegalArgumentException(Messages.format(
        "Bond future option only supports Black volatilities, but was '{}'", volatilities.getVolatilityType()));
  }

}
