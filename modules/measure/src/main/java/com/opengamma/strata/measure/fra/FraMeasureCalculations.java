/*
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivities;
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
   * Default implementation.
   */
  public static final FraMeasureCalculations DEFAULT = new FraMeasureCalculations(
      DiscountingFraTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * The cross gamma sensitivity calculator.
   */
  private static final CurveGammaCalculator CROSS_GAMMA = CurveGammaCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFraTrade}.
   */
  private final DiscountingFraTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedFraTrade}
   */
  FraMeasureCalculations(
      DiscountingFraTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  ScenarioArray<ExplainMap> explainPresentValue(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> explainPresentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // explain present value for one scenario
  ExplainMap explainPresentValue(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.explainPresentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates semi-parallel gamma PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01SemiParallelGammaBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01SemiParallelGammaBucketed(trade, marketData.scenario(i)));
  }

  // semi-parallel gamma PV01 for one scenario
  private CurrencyParameterSensitivities pv01SemiParallelGammaBucketed(
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
  private CurrencyParameterSensitivity calculateCurveSensitivity(
      ResolvedFraTrade trade,
      RatesMarketData marketData,
      CurveId curveId,
      Curve bumpedCurve) {

    MarketData bumpedMarketData = marketData.getMarketData().withValue(curveId, bumpedCurve);
    RatesProvider bumpedRatesProvider = marketData.withMarketData(bumpedMarketData).ratesProvider();
    PointSensitivities pointSensitivities = tradePricer.presentValueSensitivity(trade, bumpedRatesProvider);
    CurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.parameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

  //-------------------------------------------------------------------------
  // calculates single-node gamma PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01SingleNodeGammaBucketed(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01SingleNodeGammaBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // single-node gamma PV01 for one scenario
  private CurrencyParameterSensitivities pv01SingleNodeGammaBucketed(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    CrossGammaParameterSensitivities crossGamma = CROSS_GAMMA.calculateCrossGammaIntraCurve(
        ratesProvider,
        p -> p.parameterSensitivity(tradePricer.presentValueSensitivity(trade, p)));
    return crossGamma.diagonal().multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  DoubleScenarioArray parRate(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return DoubleScenarioArray.of(
        marketData.getScenarioCount(),
        i -> parRate(trade, marketData.scenario(i).ratesProvider()));
  }

  // par rate for one scenario
  double parRate(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.parRate(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  DoubleScenarioArray parSpread(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return DoubleScenarioArray.of(
        marketData.getScenarioCount(),
        i -> parSpread(trade, marketData.scenario(i).ratesProvider()));
  }

  // par spread for one scenario
  double parSpread(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.parSpread(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  ScenarioArray<CashFlows> cashFlows(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cashFlows(trade, marketData.scenario(i).ratesProvider()));
  }

  // cash flows for one scenario
  CashFlows cashFlows(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.cashFlows(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).ratesProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currencyExposure(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currentCash(trade, marketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFraTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currentCash(trade, ratesProvider);
  }

}
