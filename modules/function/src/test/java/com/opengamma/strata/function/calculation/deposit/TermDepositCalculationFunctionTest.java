/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.deposit;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_ISDA;
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
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
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
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * Test {@link TermDepositCalculationFunction}.
 */
@Test
public class TermDepositCalculationFunctionTest {

  public static final TermDepositTrade TRADE = TermDepositTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(date(2015, 6, 1))
          .build())
      .product(TermDeposit.builder()
          .buySell(BuySell.BUY)
          .startDate(date(2015, 6, 1))
          .endDate(date(2015, 9, 1))
          .currency(Currency.GBP)
          .notional(10000000d)
          .dayCount(THIRTY_360_ISDA)
          .rate(0.002)
          .build())
      .build();

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final LocalDate VAL_DATE = TRADE.getProduct().getEndDate().minusDays(7);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<TermDepositTrade> test = TermDepositFunctionGroups.discounting();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PAR_RATE,
        Measures.PAR_SPREAD,
        Measures.PRESENT_VALUE,
        Measures.PV01,
        Measures.BUCKETED_PV01);
    FunctionConfig<TermDepositTrade> config =
        TermDepositFunctionGroups.discounting().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(TermDepositCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    TermDepositCalculationFunction function = new TermDepositCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(DiscountCurveKey.of(CURRENCY)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    TermDepositCalculationFunction function = new TermDepositCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingTermDepositProductPricer pricer = DiscountingTermDepositProductPricer.DEFAULT;
    ResolvedTermDeposit resolved = TRADE.getProduct().resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider);
    double expectedParRate = pricer.parRate(resolved, provider);
    double expectedParSpread = pricer.parSpread(resolved, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PRESENT_VALUE_MULTI_CCY,
        Measures.PAR_RATE,
        Measures.PAR_SPREAD);
    assertThat(function.calculate(TRADE, measures, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_RATE, Result.success(ValuesArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(ValuesArray.of(ImmutableList.of(expectedParSpread))));
  }

  public void test_pv01() {
    TermDepositCalculationFunction function = new TermDepositCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    DiscountingTermDepositProductPricer pricer = DiscountingTermDepositProductPricer.DEFAULT;
    ResolvedTermDeposit resolved = TRADE.getProduct().resolve(REF_DATA);
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
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(DiscountCurveKey.of(CURRENCY), curve),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(TermDepositFunctionGroups.class);
    coverPrivateConstructor(TermDepositMeasureCalculations.class);
  }

}
