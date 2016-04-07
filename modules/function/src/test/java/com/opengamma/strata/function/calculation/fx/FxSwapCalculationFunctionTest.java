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
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.ResolvedFxSwap;

/**
 * Test {@link FxSwapCalculationFunction}.
 */
@Test
public class FxSwapCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final FxSingle LEG1 = FxSingle.of(GBP_P1000, USD_M1600, date(2015, 6, 30));
  private static final FxSingle LEG2 = FxSingle.of(GBP_P1000.negated(), USD_M1600.negated(), date(2015, 9, 30));
  private static final FxSwap PRODUCT = FxSwap.of(LEG1, LEG2);
  public static final FxSwapTrade TRADE = FxSwapTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getNearLeg().getPaymentDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<FxSwapTrade> test = FxSwapFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PAR_SPREAD,
        Measures.PRESENT_VALUE,
        Measures.PV01,
        Measures.BUCKETED_PV01,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH);
    FunctionConfig<FxSwapTrade> config =
        FxSwapFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(FxSwapCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    FxSwapCalculationFunction function = new FxSwapCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsExactly(GBP, USD);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(GBP), DiscountCurveKey.of(USD)));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(GBP);
  }

  public void test_simpleMeasures() {
    FxSwapCalculationFunction function = new FxSwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingFxSwapProductPricer pricer = DiscountingFxSwapProductPricer.DEFAULT;
    ResolvedFxSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    MultiCurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);
    MultiCurrencyAmount expectedCurrencyExp = pricer.currencyExposure(resolved, provider);
    MultiCurrencyAmount expectedCash = pricer.currentCash(resolved, provider.getValuationDate());

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.PAR_SPREAD,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.FORWARD_FX_RATE);
    assertThat(function.calculate(TRADE, measures, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCurrencyExp))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCash))));
  }

  public void test_pv01() {
    FxSwapCalculationFunction function = new FxSwapCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingFxSwapProductPricer pricer = DiscountingFxSwapProductPricer.DEFAULT;
    ResolvedFxSwap resolved = TRADE.getProduct().resolve(REF_DATA);
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider);
    CurveCurrencyParameterSensitivities pvParamSens = provider.curveParameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurveCurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measures.PV01, Measures.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, md, REF_DATA))
        .containsEntry(
            Measures.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.BUCKETED_PV01, Result.success(ScenarioResult.of(ImmutableList.of(expectedBucketedPv01))));
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
    coverPrivateConstructor(FxSwapFunctionGroups.class);
    coverPrivateConstructor(FxSwapMeasureCalculations.class);
  }

}
