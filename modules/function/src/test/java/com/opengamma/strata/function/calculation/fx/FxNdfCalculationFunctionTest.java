/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
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
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.ResolvedFxNdf;

/**
 * Test {@link FxNdfCalculationFunction}.
 */
@Test
public class FxNdfCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final CurrencyAmount NOTIONAL = CurrencyAmount.of(GBP, (double) 100_000_000);
  private static final FxNdf PRODUCT = FxNdf.builder()
      .agreedFxRate(FX_RATE)
      .settlementCurrencyNotional(NOTIONAL)
      .index(GBP_USD_WM)
      .paymentDate(date(2015, 3, 19))
      .build();
  public static final FxNdfTrade TRADE = FxNdfTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(PRODUCT)
      .build();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getPaymentDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<FxNdfTrade> test = FxNdfFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PRESENT_VALUE,
        Measures.PV01,
        Measures.BUCKETED_PV01,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.FORWARD_FX_RATE);
    FunctionConfig<FxNdfTrade> config =
        FxNdfFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(FxNdfCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    FxNdfCalculationFunction function = new FxNdfCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsExactly(GBP, USD);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(DiscountCurveKey.of(GBP), DiscountCurveKey.of(USD)));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(GBP);
  }

  public void test_simpleMeasures() {
    FxNdfCalculationFunction function = new FxNdfCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingFxNdfProductPricer pricer = DiscountingFxNdfProductPricer.DEFAULT;
    ResolvedFxNdf resolved = TRADE.getProduct().resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    MultiCurrencyAmount expectedCurrencyExp = pricer.currencyExposure(resolved, provider);
    CurrencyAmount expectedCash = pricer.currentCash(resolved, provider);
    FxRate expectedForwardFx = pricer.forwardFxRate(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.FORWARD_FX_RATE);
    assertThat(function.calculate(TRADE, measures, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedCurrencyExp))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedCash))))
        .containsEntry(
            Measures.FORWARD_FX_RATE, Result.success(ScenarioResult.of(ImmutableList.of(expectedForwardFx))));
  }

  public void test_pv01() {
    FxNdfCalculationFunction function = new FxNdfCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingFxNdfProductPricer pricer = DiscountingFxNdfProductPricer.DEFAULT;
    ResolvedFxNdf resolved = TRADE.getProduct().resolve(REF_DATA);
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
    coverPrivateConstructor(FxNdfFunctionGroups.class);
    coverPrivateConstructor(FxNdfMeasureCalculations.class);
  }

}
