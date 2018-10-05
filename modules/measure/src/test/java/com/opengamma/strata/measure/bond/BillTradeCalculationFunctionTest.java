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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.bond.DiscountingBillTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
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
      .quantity(100d)
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
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;

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

  public void test_pv01_calibrated() {
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

  public void test_pv01_quote() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBillTradePricer pricer = DiscountingBillTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01CalBucketed = MQ_CALC.sensitivity(pvParamSens, provider).multipliedBy(1e-4);
    MultiCurrencyAmount expectedPv01Cal = expectedPv01CalBucketed.total();

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_MARKET_QUOTE_SUM,
        Measures.PV01_MARKET_QUOTE_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_MARKET_QUOTE_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal))))
        .containsEntry(
            Measures.PV01_MARKET_QUOTE_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed))));
  }

  public void test_calculate_failure() {
    BillTradeCalculationFunction<BillTrade> function = BillTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    Set<Measure> measures = ImmutableSet.of(Measures.FORWARD_FX_RATE);
    assertTrue(function.calculate(TRADE, measures, PARAMS, md, REF_DATA).get(Measures.FORWARD_FX_RATE).isFailure());
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    CurveParameterSize issuerSize = CurveParameterSize.of(ISSUER_CURVE_ID.getCurveName(), 3);
    CurveParameterSize repoSize = CurveParameterSize.of(REPO_CURVE_ID.getCurveName(), 2);
    JacobianCalibrationMatrix issuerMatrix = JacobianCalibrationMatrix.of(
        ImmutableList.of(issuerSize, repoSize),
        DoubleMatrix.copyOf(new double[][] {
            {0.95, 0.03, 0.01, 0.006, 0.004}, {0.03, 0.95, 0.01, 0.005, 0.005}, {0.03, 0.01, 0.95, 0.002, 0.008}}));
    JacobianCalibrationMatrix repoMatrix = JacobianCalibrationMatrix.of(
        ImmutableList.of(issuerSize, repoSize),
        DoubleMatrix.copyOf(new double[][] {{0.003, 0.003, 0.004, 0.97, 0.02}, {0.003, 0.006, 0.001, 0.05, 0.94}}));
    CurveMetadata issuerMetadata = Curves.zeroRates(ISSUER_CURVE_ID.getCurveName(), ACT_360)
        .withInfo(CurveInfoType.JACOBIAN, issuerMatrix);
    CurveMetadata repoMetadata = Curves.zeroRates(REPO_CURVE_ID.getCurveName(), ACT_360)
        .withInfo(CurveInfoType.JACOBIAN, repoMatrix);
    Curve issuerCurve = InterpolatedNodalCurve.of(
        issuerMetadata, DoubleArray.of(1.0, 5.0, 10.0), DoubleArray.of(0.02, 0.04, 0.01), CurveInterpolators.LINEAR);
    Curve repoCurve = InterpolatedNodalCurve.of(
        repoMetadata, DoubleArray.of(0.5, 3.0), DoubleArray.of(0.005, 0.008), CurveInterpolators.LINEAR);
    return new TestMarketDataMap(
        VALUATION_DATE,
        ImmutableMap.of(REPO_CURVE_ID, repoCurve, ISSUER_CURVE_ID, issuerCurve),
        ImmutableMap.of());
  }

}
