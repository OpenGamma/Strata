/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FIXED_RATECALC_SWAP_LEG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swap.SwapDummyData;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link SwapCalculationFunction}.
 */
@Test
public class SwapCalculationFunctionTest {

  public static final SwapTrade TRADE = SwapDummyData.SWAP_TRADE;
  private static final IborIndex INDEX = (IborIndex) TRADE.getProduct().allIndices().iterator().next();
  private static final Currency CURRENCY = TRADE.getProduct().getPayLeg().get().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<SwapTrade> test = SwapFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_RATE,
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.EXPLAIN_PRESENT_VALUE,
        Measure.CASH_FLOWS,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.BUCKETED_GAMMA_PV01,
        Measure.ACCRUED_INTEREST,
        Measure.LEG_INITIAL_NOTIONAL,
        Measure.LEG_PRESENT_VALUE,
        Measure.CURRENCY_EXPOSURE,
        Measure.CURRENT_CASH        );
    FunctionConfig<SwapTrade> config =
        SwapFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(SwapCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(CURRENCY), IborIndexCurveKey.of(INDEX)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.defaultReportingCurrency(TRADE)).hasValue(CURRENCY);
  }

  public void test_simpleMeasures() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(TRADE.getProduct(), provider);
    double expectedParRate = pricer.parRate(TRADE.getProduct(), provider);
    double expectedParSpread = pricer.parSpread(TRADE.getProduct(), provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(TRADE.getProduct(), provider);
    CashFlows expectedCashFlows = pricer.cashFlows(TRADE.getProduct(), provider);
    MultiCurrencyAmount expectedExposure = pricer.currencyExposure(TRADE.getProduct(), provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(TRADE.getProduct(), provider);

    Set<Measure> measures = ImmutableSet.of(
        Measure.PRESENT_VALUE, Measure.PAR_RATE, Measure.PAR_SPREAD, Measure.EXPLAIN_PRESENT_VALUE,
        Measure.CASH_FLOWS, Measure.CURRENCY_EXPOSURE, Measure.CURRENT_CASH);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measure.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measure.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measure.EXPLAIN_PRESENT_VALUE, Result.success(DefaultScenarioResult.of(ImmutableList.of(expectedExplainPv))))
        .containsEntry(
            Measure.CASH_FLOWS, Result.success(DefaultScenarioResult.of(ImmutableList.of(expectedCashFlows))))
        .containsEntry(
            Measure.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedExposure))))
        .containsEntry(
            Measure.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))));
  }

  public void test_pv01() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(TRADE.getProduct(), provider).build();
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measure.PV01, Measure.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measure.BUCKETED_PV01, Result.success(FxConvertibleList.of(ImmutableList.of(expectedBucketedPv01))));
  }

  public void test_accruedInterest() {
    SwapCalculationFunction function = new SwapCalculationFunction();
    Currency ccy = FIXED_RATECALC_SWAP_LEG.getCurrency();
    LocalDate valDate = FIXED_RATECALC_SWAP_LEG.getEndDate().minusDays(7);
    SwapTrade trade = SwapTrade.builder()
        .product(Swap.of(FIXED_RATECALC_SWAP_LEG))
        .build();

    Set<Measure> measures = ImmutableSet.of(Measure.ACCRUED_INTEREST);
    CalculationMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));

    // create a period with altered end date, and price it
    ExpandedSwapLeg expanded = FIXED_RATECALC_SWAP_LEG.expand();
    RatePaymentPeriod rpp = (RatePaymentPeriod) expanded.getPaymentPeriods().get(expanded.getPaymentPeriods().size() - 1);
    RateAccrualPeriod rap = rpp.getAccrualPeriods().get(0);
    RateAccrualPeriod rap2 = rap.toBuilder()
        .endDate(valDate)
        .unadjustedEndDate(valDate)
        .yearFraction(DayCounts.ACT_365F.yearFraction(rap.getStartDate(), valDate))
        .build();
    RatePaymentPeriod rpp2 = rpp.toBuilder().accrualPeriods(rap2).build();
    CurrencyAmount expected = CurrencyAmount.of(
        ccy, DiscountingRatePaymentPeriodPricer.DEFAULT.forecastValue(rpp2, provider));

    MultiCurrencyValuesArray expectedArray = MultiCurrencyValuesArray.of(MultiCurrencyAmount.of(expected));
    assertThat(function.calculate(trade, measures, md))
        .containsEntry(Measure.ACCRUED_INTEREST, Result.success(expectedArray));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DiscountCurveKey.of(CURRENCY), curve, IborIndexCurveKey.of(INDEX), curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SwapFunctionGroups.class);
    coverPrivateConstructor(SwapMeasureCalculations.class);
  }

}
