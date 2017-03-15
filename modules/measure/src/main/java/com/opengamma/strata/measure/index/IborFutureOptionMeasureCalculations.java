/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
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
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.NormalIborFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.index.NormalIborFutureOptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Multi-scenario measure calculations for Ibor Future Option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class IborFutureOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final IborFutureOptionMeasureCalculations DEFAULT = new IborFutureOptionMeasureCalculations(
      NormalIborFutureOptionMarginedTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedIborFutureOptionTrade}.
   */
  private final NormalIborFutureOptionMarginedTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedIborFutureOptionTrade}
   */
  IborFutureOptionMeasureCalculations(
      NormalIborFutureOptionMarginedTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    // mark to model
    double settlementPrice = settlementPrice(trade, ratesProvider);
    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    return tradePricer.presentValue(trade, ratesProvider, normalVols, settlementPrice);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01CalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivityRates(trade, ratesProvider, normalVols);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates unit price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    IborIndex index = trade.getProduct().getUnderlyingFuture().getIndex();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> unitPrice(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(index)));
  }

  // unit price for one scenario
  double unitPrice(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    // mark to model
    NormalIborFutureOptionVolatilities normalVols = checkNormalVols(volatilities);
    return tradePricer.price(trade, ratesProvider, normalVols);
  }

  //-------------------------------------------------------------------------
  // gets the settlement price
  private double settlementPrice(ResolvedIborFutureOptionTrade trade, RatesProvider ratesProvider) {
    StandardId standardId = trade.getProduct().getSecurityId().getStandardId();
    QuoteId id = QuoteId.of(standardId, FieldName.SETTLEMENT_PRICE);
    return ratesProvider.data(id);
  }

  // ensure volatilities are Normal
  private NormalIborFutureOptionVolatilities checkNormalVols(IborFutureOptionVolatilities volatilities) {
    if (volatilities instanceof NormalIborFutureOptionVolatilities) {
      return (NormalIborFutureOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException(Messages.format(
        "Ibor future option only supports Normal volatilities, but was '{}'", volatilities.getVolatilityType()));
  }

}
