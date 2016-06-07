/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fra;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.calc.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.fra.FraDummyData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFra;

/**
 * Test {@link FraCalculationFunction}.
 */
@Test
public class FraCalculationFunctionTest {

  public static final FraTrade TRADE = FraDummyData.FRA_TRADE;

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex INDEX = TRADE.getProduct().getIndex();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final RatesMarketDataLookup RATES_MODEL = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_MODEL);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getStartDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    FraCalculationFunction function = new FraCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    FraCalculationFunction function = new FraCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_MODEL.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra resolved = TRADE.getProduct().resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParRate = pricer.parRate(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);
    ExplainMap expectedExplainPv = pricer.explainPresentValue(resolved, provider);
    CashFlows expectedCashFlows = pricer.cashFlows(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE, Measures.PAR_RATE, Measures.PAR_SPREAD, Measures.EXPLAIN_PRESENT_VALUE, Measures.CASH_FLOWS);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.EXPLAIN_PRESENT_VALUE, Result.success(ScenarioArray.of(ImmutableList.of(expectedExplainPv))))
        .containsEntry(
            Measures.CASH_FLOWS, Result.success(ScenarioArray.of(ImmutableList.of(expectedCashFlows))));
  }

  public void test_pv01() {
    FraCalculationFunction function = new FraCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_MODEL.marketDataView(md.scenario(0)).ratesProvider();
    DiscountingFraProductPricer pricer = DiscountingFraProductPricer.DEFAULT;
    ResolvedFra resolved = TRADE.getProduct().resolve(REF_DATA);
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(Measures.PV01, Measures.BUCKETED_PV01);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01, Result.success(MultiCurrencyValuesArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.BUCKETED_PV01, Result.success(ScenarioArray.of(ImmutableList.of(expectedBucketedPv01))));
  }

  //-------------------------------------------------------------------------
  private ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    return new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DISCOUNT_CURVE_ID, curve, FORWARD_CURVE_ID, curve),
        ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FraMeasureCalculations.class);
  }

}
