/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
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
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.index.NormalOvernightFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.index.NormalOvernightFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOptionTrade;

public class OvernightFutureOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final OvernightFutureOptionMeasureCalculations DEFAULT = new OvernightFutureOptionMeasureCalculations(
      NormalOvernightFutureOptionMarginedTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedOvernightFutureOptionTrade}.
   */
  private final NormalOvernightFutureOptionMarginedTradePricer tradePricer;

  /**
   * Creates an instance.
   *
   * @param tradePricer the pricer for {@link ResolvedOvernightFutureOptionTrade}
   */
  OvernightFutureOptionMeasureCalculations(
      NormalOvernightFutureOptionMarginedTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    // mark to model
    double settlementPrice = settlementPrice(trade, ratesProvider);
    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    return tradePricer.presentValue(trade, ratesProvider, normalVols, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01CalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed vega for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> vegaMarketQuoteBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> vegaMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed vega for one scenario
  CurrencyParameterSensitivities vegaMarketQuoteBucketed(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity =
        tradePricer.presentValueSensitivityModelParamsVolatility(trade, ratesProvider, normalVols).build();
    return volatilities.parameterSensitivity(pointSensitivity);
  }

  //-------------------------------------------------------------------------
  // calculates unit price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedOvernightFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      OvernightFutureOptionScenarioMarketData optionMarketData) {

    OvernightIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> unitPrice(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // unit price for one scenario
  double unitPrice(
      ResolvedOvernightFutureOptionTrade trade,
      RatesProvider ratesProvider,
      OvernightFutureOptionVolatilities volatilities) {

    // mark to model
    NormalOvernightFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    return tradePricer.price(trade, ratesProvider, normalVols);
  }

  //-------------------------------------------------------------------------
  // gets the settlement price
  private double settlementPrice(ResolvedOvernightFutureOptionTrade trade, RatesProvider ratesProvider) {
    StandardId standardId = trade.getProduct().getSecurityId().getStandardId();
    QuoteId id = QuoteId.of(standardId, FieldName.SETTLEMENT_PRICE);
    return ratesProvider.data(id);
  }

  // ensure volatilities are Normal
  private NormalOvernightFutureOptionVolatilities checkNormalVols(OvernightFutureOptionVolatilities volatilities) {
    if (volatilities instanceof NormalOvernightFutureOptionVolatilities) {
      return (NormalOvernightFutureOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException(Messages.format(
        "Overnight future option only supports Normal volatilities, but was '{}'",
        volatilities.getVolatilityType()));
  }

}
