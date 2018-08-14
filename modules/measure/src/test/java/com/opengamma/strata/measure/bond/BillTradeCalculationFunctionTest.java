/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.bond.DiscountingBillTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.Bill;
import com.opengamma.strata.product.bond.BillPosition;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.BillYieldConvention;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Test {@link BillTradeCalculationFunction}.
 */
@Test
public class BillTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VALUATION_DATE = date(2015, 3, 26);
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("A", "B");
  public static final BillTrade TRADE = BillTrade.builder()
      .product(Bill.builder()
          .notional(AdjustablePayment.of(CurrencyAmount.of(GBP, 100_000), date(2016, 3, 1)))
          .dayCount(ACT_360)
          .legalEntityId(ISSUER_ID)
          .settlementDateOffset(DaysAdjustment.NONE)
          .yieldConvention(BillYieldConvention.DISCOUNT)
          .securityId(SecurityId.of("X", "Y"))
          .build())
      .price(0.9932)
      .info(TradeInfo.of(date(2015, 2, 27)))
      .build();
  public static final ResolvedBillTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final RepoGroup REPO_GROUP = RepoGroup.of("Repo");
  private static final LegalEntityGroup ISSUER_GROUP = LegalEntityGroup.of("Issuer");
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final CurveId REPO_CURVE_ID = CurveId.of("Default", "Repo");
  private static final CurveId ISSUER_CURVE_ID = CurveId.of("Default", "Issuer");
  public static final LegalEntityDiscountingMarketDataLookup LOOKUP = LegalEntityDiscountingMarketDataLookup.of(
      ImmutableMap.of(ISSUER_ID, REPO_GROUP),
      ImmutableMap.of(Pair.of(REPO_GROUP, CURRENCY), REPO_CURVE_ID),
      ImmutableMap.of(ISSUER_ID, ISSUER_GROUP),
      ImmutableMap.of(Pair.of(ISSUER_GROUP, CURRENCY), ISSUER_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(LOOKUP);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(ImmutableSet.of(REPO_CURVE_ID, ISSUER_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_target() {
    BillTradeCalculationFunction<BillTrade> functionTrade = BillTradeCalculationFunction.TRADE;
    BillTradeCalculationFunction<BillPosition> functionPosition = BillTradeCalculationFunction.POSITION;
    assertThat(functionTrade.targetType()).isEqualTo(BillTrade.class);
    assertThat(functionPosition.targetType()).isEqualTo(BillPosition.class);
    assertThat(functionTrade.identifier(TRADE)).isEqualTo(TRADE.getInfo().getId());
  }

  public void test_simpleMeasures() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBillTradePricer pricer = DiscountingBillTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, VALUATION_DATE);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));
  }

  public void test_pv01() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBillTradePricer pricer = DiscountingBillTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed))));
  }

  public void test_calculate_failure() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    Set<Measure> measures = ImmutableSet.of(Measures.FORWARD_FX_RATE);
    assertTrue(function.calculate(TRADE, measures, PARAMS, md, REF_DATA).get(Measures.FORWARD_FX_RATE).isFailure());
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    return new TestMarketDataMap(
        VALUATION_DATE,
        ImmutableMap.of(REPO_CURVE_ID, curve, ISSUER_CURVE_ID, curve),
        ImmutableMap.of());
  }

}
