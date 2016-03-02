/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.function.calculation.rate.MarketDataUtils;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.product.fra.ResolvedFra;
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
  private static final DiscountingFraProductPricer PRICER = DiscountingFraProductPricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private FraMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(
      ResolvedFraTrade trade,
      CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParRate(product, marketData.scenario(i)));
  }

  // par rate for one scenario
  private static double calculateParRate(ResolvedFra fra, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.parRate(fra, provider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(ResolvedFraTrade trade, CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParSpread(product, marketData.scenario(i)));
  }

  // par spread for one scenario
  private static double calculateParSpread(ResolvedFra product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.parSpread(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(ResolvedFraTrade trade, CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(ResolvedFra product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.presentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  static ScenarioResult<ExplainMap> explainPresentValue(ResolvedFraTrade trade, CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateExplainPresentValue(product, marketData.scenario(i)));
  }

  // explain present value for one scenario
  private static ExplainMap calculateExplainPresentValue(ResolvedFra product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.explainPresentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  static ScenarioResult<CashFlows> cashFlows(ResolvedFraTrade trade, CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCashFlows(product, marketData.scenario(i)));
  }

  // cash flows for one scenario
  private static CashFlows calculateCashFlows(ResolvedFra product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.cashFlows(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(ResolvedFraTrade trade, CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(ResolvedFra product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      ResolvedFraTrade trade,
      CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedFra product,
      MarketData marketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed gamma PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedGammaPv01(
      ResolvedFraTrade trade,
      CalculationMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedGammaPv01(product, marketData.scenario(i)));
  }

  // bucketed gamma PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedGammaPv01(
      ResolvedFra product,
      MarketData marketData) {

    // find the curve and check it is valid
    Currency currency = product.getCurrency();
    NodalCurve nodalCurve = marketData.getValue(DiscountCurveKey.of(currency)).toNodalCurve();

    // find indices and validate there is only one curve
    Set<IborIndex> indices = product.allIndices();
    validateSingleCurve(indices, marketData, nodalCurve);

    // calculate gamma
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve, currency, c -> calculateCurveSensitivity(product, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // validates that the indices all resolve to the single specified curve
  private static void validateSingleCurve(Set<IborIndex> indices, MarketData marketData, NodalCurve nodalCurve) {
    Set<IborIndexCurveKey> differentForwardCurves = indices.stream()
        .map(idx -> IborIndexCurveKey.of(idx))
        .filter(k -> !nodalCurve.equals(marketData.getValue(k)))
        .collect(toSet());
    if (!differentForwardCurves.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but discounting curve is different from " +
              "index curves for indices: {}", differentForwardCurves));
    }
  }

  // calculates the sensitivity
  private static CurveCurrencyParameterSensitivity calculateCurveSensitivity(
      ResolvedFra product,
      Currency currency,
      Set<IborIndex> indices,
      MarketData marketData,
      NodalCurve bumpedCurve) {

    RatesProvider ratesProvider = MarketDataUtils.toSingleCurveRatesProvider(marketData, currency, indices, bumpedCurve);
    PointSensitivities pointSensitivities = PRICER.presentValueSensitivity(product, ratesProvider);
    CurveCurrencyParameterSensitivities paramSensitivities = ratesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
