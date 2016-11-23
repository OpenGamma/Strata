/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

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
import com.opengamma.strata.pricer.fxopt.BlackFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;

/**
 * Multi-scenario measure calculations for FX single barrier option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxSingleBarrierOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FxSingleBarrierOptionMeasureCalculations DEFAULT = new FxSingleBarrierOptionMeasureCalculations(
      BlackFxSingleBarrierOptionTradePricer.DEFAULT,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOptionTrade}.
   */
  private final BlackFxSingleBarrierOptionTradePricer blackPricer;
  /**
   * Pricer for {@link ResolvedFxSingleBarrierOptionTrade}.
   */
  private final ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer trinomialTreePricer;

  /**
   * Creates an instance.
   * 
   * @param blackPricer  the pricer for {@link ResolvedFxSingleBarrierOptionTrade}
   * @param trinomialTreePricer  the pricer for {@link ResolvedFxSingleBarrierOptionTrade} SABR
   */
  FxSingleBarrierOptionMeasureCalculations(
      BlackFxSingleBarrierOptionTradePricer blackPricer,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer trinomialTreePricer) {
    this.blackPricer = ArgChecker.notNull(blackPricer, "blackPricer");
    this.trinomialTreePricer = ArgChecker.notNull(trinomialTreePricer, "trinomialTreePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return trinomialTreePricer.presentValue(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      return blackPricer.presentValue(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return paramSens.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return paramSens.multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private CurrencyParameterSensitivities parameterSensitivities(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return trinomialTreePricer.presentValueSensitivityRates(
          trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      PointSensitivities pointSens = blackPricer.presentValueSensitivityRatesStickyStrike(
          trade, ratesProvider, checkBlackVolatilities(volatilities));
      return ratesProvider.parameterSensitivity(pointSens);
    }
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

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
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return trinomialTreePricer.currencyExposure(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      return blackPricer.currencyExposure(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).getValuationDate(),
            method));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFxSingleBarrierOptionTrade trade,
      LocalDate valuationDate,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return trinomialTreePricer.currentCash(trade, valuationDate);
    } else {
      return blackPricer.currentCash(trade, valuationDate);
    }
  }

  //-------------------------------------------------------------------------
  // ensures that the volatilities are correct
  private BlackFxOptionVolatilities checkBlackVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX single barrier option Black pricing requires BlackFxOptionVolatilities");
  }

  // ensures that the volatilities are correct
  private BlackFxOptionVolatilities checkTrinomialTreeVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX single barrier option Trinomial Tree pricing requires BlackFxOptionVolatilities");
  }

}
