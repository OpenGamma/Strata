/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fra;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketData;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fra.ResolvedFraTrade;

/**
 * Multi-scenario measure calculations for FRA trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FraMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingFraTradePricer PRICER = DiscountingFraTradePricer.DEFAULT;
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private FraMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // present value for one scenario
  static CurrencyAmount presentValue(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01CalibratedSum(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // PV01 for one scenario
  static MultiCurrencyAmount pv01CalibratedSum(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // bucketed PV01 for one scenario
  static CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static MultiCurrencyValuesArray pv01MarketQuoteSum(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // bucketed PV01 for one scenario
  static MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // bucketed PV01 for one scenario
  static CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates semi-parallel gamma PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> pv01SemiParallelGammaBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01SemiParallelGammaBucketed(trade, marketData.scenario(i)));
  }

  // semi-parallel gamma PV01 for one scenario
  private static CurrencyParameterSensitivities pv01SemiParallelGammaBucketed(
      ResolvedFraTrade trade,
      RatesMarketData marketData) {

    // find the curve identifiers and resolve to a single curve
    Currency currency = trade.getProduct().getCurrency();
    Set<IborIndex> indices = trade.getProduct().allIndices();
    ImmutableSet<MarketDataId<?>> discountIds = marketData.getLookup().getDiscountMarketDataIds(currency);
    ImmutableSet<MarketDataId<?>> forwardIds = indices.stream()
        .flatMap(idx -> marketData.getLookup().getForwardMarketDataIds(idx).stream())
        .collect(toImmutableSet());
    Set<MarketDataId<?>> allIds = Sets.union(discountIds, forwardIds);
    if (allIds.size() != 1) {
      throw new IllegalArgumentException(Messages.format(
          "Implementation only supports a single curve, but lookup refers to more than one: {}", allIds));
    }
    MarketDataId<?> singleId = allIds.iterator().next();
    if (!(singleId instanceof CurveId)) {
      throw new IllegalArgumentException(Messages.format(
          "Implementation only supports a single curve, but lookup does not refer to a curve: {} {}",
          singleId.getClass().getName(), singleId));
    }
    CurveId curveId = (CurveId) singleId;
    Curve curve = marketData.getMarketData().getValue(curveId);

    // calculate gamma
    CurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        curve, currency, c -> calculateCurveSensitivity(trade, marketData, curveId, c));
    return CurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // calculates the sensitivity
  private static CurrencyParameterSensitivity calculateCurveSensitivity(
      ResolvedFraTrade trade,
      RatesMarketData marketData,
      CurveId curveId,
      Curve bumpedCurve) {

    MarketData bumpedMarketData = marketData.getMarketData().withValue(curveId, bumpedCurve);
    RatesProvider bumpedRatesProvider = marketData.withMarketData(bumpedMarketData).ratesProvider();
    PointSensitivities pointSensitivities = PRICER.presentValueSensitivity(trade, bumpedRatesProvider);
    CurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.parameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> parRate(trade, marketData.scenario(i).ratesProvider()));
  }

  // par rate for one scenario
  static double parRate(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.parRate(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> parSpread(trade, marketData.scenario(i).ratesProvider()));
  }

  // par spread for one scenario
  static double parSpread(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.parSpread(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  static ScenarioArray<CashFlows> cashFlows(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cashFlows(trade, marketData.scenario(i).ratesProvider()));
  }

  // cash flows for one scenario
  static CashFlows cashFlows(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.cashFlows(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).ratesProvider()));
  }

  // currency exposure for one scenario
  static MultiCurrencyAmount currencyExposure(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.currencyExposure(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static CurrencyValuesArray currentCash(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> currentCash(trade, marketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  static CurrencyAmount currentCash(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.currentCash(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  static ScenarioArray<ExplainMap> explainPresentValue(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> explainPresentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // explain present value for one scenario
  static ExplainMap explainPresentValue(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return PRICER.explainPresentValue(trade, ratesProvider);
  }

}
