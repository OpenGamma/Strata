/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.ScenarioMarketData;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.result.ScenarioResult;
import com.opengamma.strata.calc.result.ValuesArray;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.calculation.RatesMarketDataLookup;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.id.IndexQuoteId;
import com.opengamma.strata.market.id.CurveId;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link SwapCalculationFunction}.
 */
@Test
public class SwapCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  public static final SwapTrade TRADE = FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M
      .createTrade(date(2016, 6, 30), Tenor.TENOR_10Y, BuySell.BUY, 1_000_000, 0.01, REF_DATA);

  private static final Currency CURRENCY = TRADE.getProduct().getPayLeg().get().getCurrency();
  private static final IborIndex INDEX = (IborIndex) TRADE.getProduct().allIndices().iterator().next();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().getUnadjusted().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    ResolvedSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    MultiCurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParRate = pricer.parRate(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(resolved, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(resolved, provider);
    MultiCurrencyAmount expectedExposure = pricer.currencyExposure(resolved, provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.PAR_RATE,
        Measures.PAR_SPREAD,
        Measures.EXPLAIN_PRESENT_VALUE,
        Measures.CASH_FLOWS, Measures.CURRENCY_EXPOSURE, Measures.CURRENT_CASH);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.EXPLAIN_PRESENT_VALUE, Result.success(ScenarioResult.of(ImmutableList.of(expectedExplainPv))))
        .containsEntry(
            Measures.CASH_FLOWS, Result.success(ScenarioResult.of(ImmutableList.of(expectedCashFlows))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))));
  }

  public void test_pv01() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    ResolvedSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider).build();
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measures.PV01, Measures.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.BUCKETED_PV01, Result.success(ScenarioResult.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  private ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DISCOUNT_CURVE_ID, curve, FORWARD_CURVE_ID, curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SwapMeasureCalculations.class);
  }

}
