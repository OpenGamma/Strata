/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.measure.fxopt.FxCalculationUtils.checkBlackVolatilities;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.DiscountingFxCollarTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;

/**
 * Multi-scenario measure calculations for FX collar trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
public class FxCollarMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FxCollarMeasureCalculations DEFAULT = new FxCollarMeasureCalculations(
      DiscountingFxCollarTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;
  /**
   * Pricer for {@link ResolvedFxCollarTrade}.
   */
  private final DiscountingFxCollarTradePricer tradePricer;

  /**
   * Creates an instance.
   *
   * @param tradePricer  the pricer for {@link ResolvedFxCollarTrade}
   */
  FxCollarMeasureCalculations(
      DiscountingFxCollarTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // present value for one scenario
  MultiCurrencyAmount presentValue(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return tradePricer.presentValue(trade, ratesProvider, checkBlackVolatilities(volatilities));
  }

  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities);
    return paramSens.total().multipliedBy(ONE_BASIS_POINT);
  }

  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities);
    return paramSens.multipliedBy(ONE_BASIS_POINT);
  }

  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  private CurrencyParameterSensitivities pointSensitivity(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    PointSensitivities pointSens = tradePricer.presentValueSensitivityRatesStickyStrike(
        trade, ratesProvider, checkBlackVolatilities(volatilities));
    return ratesProvider.parameterSensitivity(pointSens);
  }

  // calculates vega (present value volatility sensitivities) for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> vegaMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> vegaMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // vega for one scenario
  CurrencyParameterSensitivities vegaMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    BlackFxOptionVolatilities blackVols = checkBlackVolatilities(volatilities);
    PointSensitivities pointSens =
        tradePricer.presentValueSensitivityModelParamsVolatility(trade, ratesProvider, blackVols);
    return blackVols.parameterSensitivity(pointSens);
  }

  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    CurrencyPair currencyPair = trade.getProduct().getOption1().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair)));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return tradePricer.currencyExposure(trade, ratesProvider, checkBlackVolatilities(volatilities));
  }

  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).getValuationDate()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFxCollarTrade trade,
      LocalDate valuationDate) {

    return tradePricer.currentCash(trade, valuationDate);
  }
}
