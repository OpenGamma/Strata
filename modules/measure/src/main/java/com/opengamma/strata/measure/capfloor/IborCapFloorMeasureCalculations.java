/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.capfloor;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.capfloor.VolatilityIborCapFloorTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorTrade;

/**
 * Multi-scenario measure calculations for Ibor cap/floor trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class IborCapFloorMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final IborCapFloorMeasureCalculations DEFAULT = new IborCapFloorMeasureCalculations(
      VolatilityIborCapFloorTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedIborCapFloorTrade}.
   */
  private final VolatilityIborCapFloorTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedIborCapFloorTrade}
   */
  IborCapFloorMeasureCalculations(
      VolatilityIborCapFloorTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // present value for one scenario
  MultiCurrencyAmount presentValue(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return tradePricer.presentValue(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivities pointSensitivity = pointSensitivity(trade, ratesProvider, volatilities);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private PointSensitivities pointSensitivity(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return tradePricer.presentValueSensitivityRates(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return tradePricer.currencyExposure(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  MultiCurrencyScenarioArray currentCash(
      ResolvedIborCapFloorTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborCapFloorScenarioMarketData capFloorMarketData) {

    IborIndex index = trade.getProduct().getCapFloorLeg().getIndex();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            capFloorMarketData.scenario(i).volatilities(index)));
  }

  // current cash for one scenario
  MultiCurrencyAmount currentCash(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return tradePricer.currentCash(trade, ratesProvider, volatilities);
  }

}
