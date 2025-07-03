/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.measure.fxopt.FxCalculationUtils.checkBlackVolatilities;
import static com.opengamma.strata.measure.fxopt.FxCalculationUtils.checkVannaVolgaVolatilities;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxVanillaOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.VannaVolgaFxVanillaOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOptionTrade;

/**
 * Multi-scenario measure calculations for FX vanilla option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxVanillaOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FxVanillaOptionMeasureCalculations DEFAULT = new FxVanillaOptionMeasureCalculations(
      BlackFxVanillaOptionTradePricer.DEFAULT,
      VannaVolgaFxVanillaOptionTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFxVanillaOptionTrade}.
   */
  private final BlackFxVanillaOptionTradePricer blackPricer;
  /**
   * Pricer for {@link ResolvedFxVanillaOptionTrade}.
   */
  private final VannaVolgaFxVanillaOptionTradePricer vannaVolgaPricer;

  /**
   * Creates an instance.
   * 
   * @param blackPricer  the pricer for {@link ResolvedFxVanillaOptionTrade} using Black
   * @param vannaVolgaPricer  the pricer for {@link ResolvedFxVanillaOptionTrade} using Vanna-Volga
   */
  FxVanillaOptionMeasureCalculations(
      BlackFxVanillaOptionTradePricer blackPricer,
      VannaVolgaFxVanillaOptionTradePricer vannaVolgaPricer) {
    this.blackPricer = ArgChecker.notNull(blackPricer, "blackPricer");
    this.vannaVolgaPricer = ArgChecker.notNull(vannaVolgaPricer, "vannaVolgaPricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // present value for one scenario
  MultiCurrencyAmount presentValue(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      return vannaVolgaPricer.presentValue(trade, ratesProvider, checkVannaVolgaVolatilities(volatilities));
    } else {
      return blackPricer.presentValue(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities, method);
    return paramSens.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities, method);
    return paramSens.multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    CurrencyParameterSensitivities paramSens = pointSensitivity(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private CurrencyParameterSensitivities pointSensitivity(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    PointSensitivities pointSens;
    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      pointSens = vannaVolgaPricer.presentValueSensitivityRatesStickyStrike(
          trade, ratesProvider, checkVannaVolgaVolatilities(volatilities));
    } else {
      pointSens = blackPricer.presentValueSensitivityRatesStickyStrike(
          trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
    return ratesProvider.parameterSensitivity(pointSens);
  }

  //-------------------------------------------------------------------------
  // calculates vega (present value volatility sensitivities) for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> vegaMarketQuoteBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> vegaMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // vega for one scenario
  CurrencyParameterSensitivities vegaMarketQuoteBucketed(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      BlackFxOptionSmileVolatilities blackSmileVols = checkVannaVolgaVolatilities(volatilities);
      PointSensitivities pointSens =
          vannaVolgaPricer.presentValueSensitivityModelParamsVolatility(trade, ratesProvider, blackSmileVols);
      return blackSmileVols.parameterSensitivity(pointSens);
    } else {
      BlackFxOptionVolatilities blackVols = checkBlackVolatilities(volatilities);
      PointSensitivities pointSens =
          blackPricer.presentValueSensitivityModelParamsVolatility(trade, ratesProvider, blackVols);
      return blackVols.parameterSensitivity(pointSens);
    }
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      return vannaVolgaPricer.currencyExposure(trade, ratesProvider, checkVannaVolgaVolatilities(volatilities));
    } else {
      return blackPricer.currencyExposure(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates delta for all scenarios
  DoubleScenarioArray delta(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> delta(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // delta for one scenario
  double delta(
      ResolvedFxVanillaOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxVanillaOptionMethod method) {

    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      throw new IllegalArgumentException(
          "FX vanilla option Vanna Volga pricer does not currently support delta calculation");
    } else {
      return blackPricer.delta(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFxVanillaOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxVanillaOptionMethod method) {

    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).getValuationDate(),
            method));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFxVanillaOptionTrade trade,
      LocalDate valuationDate,
      FxVanillaOptionMethod method) {

    if (method == FxVanillaOptionMethod.VANNA_VOLGA) {
      return vannaVolgaPricer.currentCash(trade, valuationDate);
    } else {
      return blackPricer.currentCash(trade, valuationDate);
    }
  }

}
