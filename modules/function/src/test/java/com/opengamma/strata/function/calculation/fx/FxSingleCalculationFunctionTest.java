/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
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
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateKey;
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
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Test {@link FxSingleCalculationFunction}.
 */
@Test
public class FxSingleCalculationFunctionTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final FxSingle PRODUCT = FxSingle.of(GBP_P1000, USD_M1600, date(2015, 6, 30));
  public static final FxSingleTrade TRADE = FxSingleTrade.builder().product(PRODUCT).build();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getPaymentDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<FxSingleTrade> test = FxSingleFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measure.PAR_SPREAD,
        Measure.PRESENT_VALUE,
        Measure.PV01,
        Measure.BUCKETED_PV01,
        Measure.CURRENCY_EXPOSURE,
        Measure.CURRENT_CASH,
        Measure.FORWARD_FX_RATE);
    FunctionConfig<FxSingleTrade> config =
        FxSingleFunctionGroups.discounting().functionConfig(TRADE, Measure.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(FxSingleCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    FxSingleCalculationFunction function = new FxSingleCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsExactly(GBP, USD);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(GBP), DiscountCurveKey.of(USD)));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.defaultReportingCurrency(TRADE)).hasValue(GBP);
  }

  public void test_simpleMeasures() {
    FxSingleCalculationFunction function = new FxSingleCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingFxSingleProductPricer pricer = DiscountingFxSingleProductPricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(TRADE.getProduct(), provider);
    double expectedParSpread = pricer.parSpread(TRADE.getProduct(), provider);
    MultiCurrencyAmount expectedCurrencyExp = pricer.currencyExposure(TRADE.getProduct(), provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(TRADE.getProduct(), provider.getValuationDate());
    FxRate expectedForwardFx = pricer.forwardFxRate(TRADE.getProduct(), provider);

    Set<Measure> measures = ImmutableSet.of(
        Measure.PRESENT_VALUE, Measure.PAR_SPREAD, Measure.CURRENCY_EXPOSURE, Measure.CURRENT_CASH, Measure.FORWARD_FX_RATE);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measure.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measure.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measure.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCurrencyExp))))
        .containsEntry(
            Measure.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))))
        .containsEntry(
            Measure.FORWARD_FX_RATE, Result.success(DefaultScenarioResult.of(ImmutableList.of(expectedForwardFx))));
  }

  public void test_pv01() {
    FxSingleCalculationFunction function = new FxSingleCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = new MarketDataRatesProvider(new SingleCalculationMarketData(md, 0));
    DiscountingFxSingleProductPricer pricer = DiscountingFxSingleProductPricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(TRADE.getProduct(), provider);
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

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve1 = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.992);
    Curve curve2 = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.991);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DiscountCurveKey.of(GBP), curve1,
            DiscountCurveKey.of(USD), curve2,
            FxRateKey.of(GBP, USD), FxRate.of(GBP, USD, 1.62)),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FxSingleFunctionGroups.class);
    coverPrivateConstructor(FxSingleMeasureCalculations.class);
  }

}
