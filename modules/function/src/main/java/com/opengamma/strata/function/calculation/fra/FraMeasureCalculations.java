/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.result.CurrencyValuesArray;
import com.opengamma.strata.calc.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.result.ScenarioResult;
import com.opengamma.strata.calc.result.ValuesArray;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.function.calculation.RatesMarketData;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
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
      RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParRate(product, marketData.scenario(i)));
  }

  // par rate for one scenario
  private static double calculateParRate(ResolvedFra fra, RatesMarketData marketData) {
    return PRICER.parRate(fra, marketData.ratesProvider());
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(ResolvedFraTrade trade, RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParSpread(product, marketData.scenario(i)));
  }

  // par spread for one scenario
  private static double calculateParSpread(ResolvedFra product, RatesMarketData marketData) {
    return PRICER.parSpread(product, marketData.ratesProvider());
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(ResolvedFraTrade trade, RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(ResolvedFra product, RatesMarketData marketData) {
    return PRICER.presentValue(product, marketData.ratesProvider());
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  static ScenarioResult<ExplainMap> explainPresentValue(ResolvedFraTrade trade, RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateExplainPresentValue(product, marketData.scenario(i)));
  }

  // explain present value for one scenario
  private static ExplainMap calculateExplainPresentValue(ResolvedFra product, RatesMarketData marketData) {
    return PRICER.explainPresentValue(product, marketData.ratesProvider());
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  static ScenarioResult<CashFlows> cashFlows(ResolvedFraTrade trade, RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCashFlows(product, marketData.scenario(i)));
  }

  // cash flows for one scenario
  private static CashFlows calculateCashFlows(ResolvedFra product, RatesMarketData marketData) {
    return PRICER.cashFlows(product, marketData.ratesProvider());
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(ResolvedFraTrade trade, RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(ResolvedFra product, RatesMarketData marketData) {
    RatesProvider ratesProvider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, ratesProvider);
    return ratesProvider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedFra product,
      RatesMarketData marketData) {

    RatesProvider ratesProvider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, ratesProvider);
    return ratesProvider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed gamma PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedGammaPv01(
      ResolvedFraTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFra product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedGammaPv01(product, marketData.scenario(i)));
  }

  // bucketed gamma PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedGammaPv01(
      ResolvedFra product,
      RatesMarketData marketData) {

    // find the curve identifiers and resolve to a single curve
    Currency currency = product.getCurrency();
    Set<IborIndex> indices = product.allIndices();
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
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        curve, currency, c -> calculateCurveSensitivity(product, marketData, curveId, c));
    return CurveCurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // calculates the sensitivity
  private static CurveCurrencyParameterSensitivity calculateCurveSensitivity(
      ResolvedFra product,
      RatesMarketData marketData,
      CurveId curveId,
      Curve bumpedCurve) {

    MarketData bumpedMarketData = marketData.getMarketData().withValue(curveId, bumpedCurve);
    RatesProvider bumpedRatesProvider = marketData.withMarketData(bumpedMarketData).ratesProvider();
    PointSensitivities pointSensitivities = PRICER.presentValueSensitivity(product, bumpedRatesProvider);
    CurveCurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
