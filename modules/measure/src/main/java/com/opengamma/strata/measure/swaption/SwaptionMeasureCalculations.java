/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swaption.SabrSwaptionTradePricer;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionTradePricer;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;

/**
 * Multi-scenario measure calculations for Swaption trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class SwaptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final SwaptionMeasureCalculations DEFAULT = new SwaptionMeasureCalculations(
      VolatilitySwaptionTradePricer.DEFAULT,
      SabrSwaptionTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedSwaptionTrade}.
   */
  private final VolatilitySwaptionTradePricer tradePricer;
  /**
   * Pricer for {@link ResolvedSwaptionTrade}.
   */
  private final SabrSwaptionTradePricer sabrTradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedSwaptionTrade}
   * @param sabrTradePricer  the pricer for {@link ResolvedSwaptionTrade} SABR
   */
  SwaptionMeasureCalculations(
      VolatilitySwaptionTradePricer tradePricer,
      SabrSwaptionTradePricer sabrTradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
    this.sabrTradePricer = ArgChecker.notNull(sabrTradePricer, "sabrTradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.presentValue(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private PointSensitivities pointSensitivity(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    if (volatilities instanceof SabrSwaptionVolatilities) {
      return sabrTradePricer.presentValueSensitivityRatesStickyModel(
          trade, ratesProvider, (SabrSwaptionVolatilities) volatilities);
    }
    return tradePricer.presentValueSensitivityRatesStickyStrike(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates normal vega for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> vegaMarketQuoteBucketed(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> vegaMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  //  normal vega for one scenario
  CurrencyParameterSensitivities vegaMarketQuoteBucketed(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    if (!volatilities.getVolatilityType().equals(NORMAL_VOLATILITY)) {
      throw new IllegalArgumentException("Vega calculation requires normal volatilities");
    }
    PointSensitivities pointSensitivity = pointSensitivityVega(trade, ratesProvider, volatilities);
    return volatilities.parameterSensitivity(pointSensitivity);
  }

  //  normal vega point sensitivity
  private PointSensitivities pointSensitivityVega(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.presentValueSensitivityModelParamsVolatility(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    RateIndex index = trade.getProduct().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            swaptionMarketData.scenario(i).volatilities(index)));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return tradePricer.currencyExposure(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedSwaptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      SwaptionScenarioMarketData swaptionMarketData) {

    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).getValuationDate()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedSwaptionTrade trade,
      LocalDate valuationDate) {

    return tradePricer.currentCash(trade, valuationDate);
  }

}
