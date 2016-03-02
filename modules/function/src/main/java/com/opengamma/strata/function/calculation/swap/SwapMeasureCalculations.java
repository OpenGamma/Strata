/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.SingleScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.function.calculation.rate.MarketDataUtils;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.amount.LegAmount;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.amount.SwapLegAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.NotionalPaymentPeriod;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Multi-scenario measure calculations for Swap trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class SwapMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingSwapProductPricer PRICER = DiscountingSwapProductPricer.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;
  /**
   * Special marker value used in place of null.
   */
  private static CurrencyAmount NOT_FOUND = CurrencyAmount.zero(Currency.XXX);

  // restricted constructor
  private SwapMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  static ValuesArray parRate(ResolvedSwapTrade trade, CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParRate(product, marketData.scenario(i)));
  }

  // par rate for one scenario
  private static double calculateParRate(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.parRate(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParSpread(product, marketData.scenario(i)));
  }

  // par spread for one scenario
  private static double calculateParSpread(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.parSpread(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static MultiCurrencyValuesArray presentValue(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static MultiCurrencyAmount calculatePresentValue(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.presentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  static ScenarioResult<ExplainMap> explainPresentValue(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateExplainPresentValue(product, marketData.scenario(i)));
  }

  // explain present value for one scenario
  private static ExplainMap calculateExplainPresentValue(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.explainPresentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  static ScenarioResult<CashFlows> cashFlows(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateCashFlows(product, marketData.scenario(i)));
  }

  // cash flows for one scenario
  private static CashFlows calculateCashFlows(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.cashFlows(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedSwap product,
      MarketData marketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed gamma PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedGammaPv01(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedGammaPv01(product, marketData.scenario(i)));
  }

  // bucketed gamma PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedGammaPv01(
      ResolvedSwap product,
      MarketData marketData) {

    // find the curve and check it is valid
    if (product.isCrossCurrency()) {
      throw new IllegalArgumentException("Implementation only supports a single curve, but swap is cross-currency");
    }
    Currency currency = product.getLegs().get(0).getCurrency();
    NodalCurve nodalCurve = marketData.getValue(DiscountCurveKey.of(currency)).toNodalCurve();

    // find indices and validate there is only one curve
    Set<Index> indices = product.allIndices();
    validateSingleCurve(indices, marketData, nodalCurve);

    // calculate gamma
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve, currency, c -> calculateCurveSensitivity(product, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma)
        .multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // validates that the indices all resolve to the single specified curve
  private static void validateSingleCurve(Set<Index> indices, MarketData marketData, NodalCurve nodalCurve) {
    Set<MarketDataKey<?>> differentForwardCurves = indices.stream()
        .map(MarketDataKeys::indexCurve)
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
      ResolvedSwap expandedSwap,
      Currency currency,
      Set<? extends Index> indices,
      MarketData marketData,
      NodalCurve bumpedCurve) {

    RatesProvider ratesProvider = MarketDataUtils.toSingleCurveRatesProvider(marketData, currency, indices, bumpedCurve);
    PointSensitivities pointSensitivities = DiscountingSwapProductPricer.DEFAULT
        .presentValueSensitivity(expandedSwap, ratesProvider).build();
    CurveCurrencyParameterSensitivities paramSensitivities = ratesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

  //-------------------------------------------------------------------------
  // calculates accrued interest for all scenarios
  static MultiCurrencyValuesArray accruedInterest(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateAccruedInterest(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static MultiCurrencyAmount calculateAccruedInterest(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.accruedInterest(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates leg initial notional for all scenarios
  static SingleScenarioResult<LegAmounts> legInitialNotional(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    LegAmounts legInitialNotional = calculateLegInitialNotional(trade);
    return SingleScenarioResult.of(marketData.getScenarioCount(), legInitialNotional);
  }

  // leg initial notional, which is the same for all scenarios
  // package-scoped for testing
  static LegAmounts calculateLegInitialNotional(ResolvedSwapTrade trade) {
    List<Pair<ResolvedSwapLeg, CurrencyAmount>> notionals = trade.getProduct().getLegs().stream()
        .map(leg -> Pair.of(leg, buildLegNotional(leg)))
        .collect(toList());
    CurrencyAmount firstNotional = notionals.stream()
        .filter(pair -> pair.getSecond() != NOT_FOUND)
        .map(pair -> pair.getSecond())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No notional found on any swap leg"));
    notionals = notionals.stream()
        .map(pair -> pair.getSecond() != NOT_FOUND ? pair : Pair.of(pair.getFirst(), firstNotional))
        .collect(toList());
    ImmutableList<LegAmount> legAmounts = notionals.stream()
        .map(pair -> SwapLegAmount.of(pair.getFirst(), pair.getSecond()))
        .collect(toImmutableList());
    return LegAmounts.of(legAmounts);
  }

  // find the notional
  private static CurrencyAmount buildLegNotional(ResolvedSwapLeg leg) {
    // check for NotionalPaymentPeriod
    PaymentPeriod firstPaymentPeriod = leg.getPaymentPeriods().get(0);
    if (firstPaymentPeriod instanceof NotionalPaymentPeriod) {
      NotionalPaymentPeriod pp = (NotionalPaymentPeriod) firstPaymentPeriod;
      return pp.getNotionalAmount().positive();
    }
    return NOT_FOUND;
  }

  //-------------------------------------------------------------------------
  // calculates leg present value for all scenarios
  static ScenarioResult<LegAmounts> legPresentValue(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateLegPresentValue(product, marketData.scenario(i)));
  }

  // leg present value for one scenario
  private static LegAmounts calculateLegPresentValue(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    List<LegAmount> legAmounts = product.getLegs().stream()
        .map(leg -> legAmount(leg, provider))
        .collect(Collectors.toList());
    return LegAmounts.of(legAmounts);
  }

  // present value for a leg
  private static SwapLegAmount legAmount(ResolvedSwapLeg leg, RatesProvider provider) {
    CurrencyAmount amount = DiscountingSwapLegPricer.DEFAULT.presentValue(leg, provider);
    return SwapLegAmount.of(leg, amount);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrencyExposure(product, marketData.scenario(i)));
  }

  // currency exposure for one scenario
  private static MultiCurrencyAmount calculateCurrencyExposure(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.currencyExposure(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static MultiCurrencyValuesArray currentCash(
      ResolvedSwapTrade trade,
      CalculationMarketData marketData) {

    ResolvedSwap product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrentCash(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static MultiCurrencyAmount calculateCurrentCash(ResolvedSwap product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.currentCash(product, provider);
  }

}
