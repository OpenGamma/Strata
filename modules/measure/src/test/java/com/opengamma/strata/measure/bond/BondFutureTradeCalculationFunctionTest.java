/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.FieldName;
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
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.bond.BondDataSets;
import com.opengamma.strata.pricer.bond.DiscountingBondFutureTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Test {@link BondFutureTradeCalculationFunction}.
 */
@Test
public class BondFutureTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final BondFuture PRODUCT = BondDataSets.FUTURE_PRODUCT_USD;
  private static final BondFutureTrade TRADE = BondDataSets.FUTURE_TRADE_USD;
  public static final ResolvedBondFutureTrade RTRADE = TRADE.resolve(REF_DATA);
  public static final double SETTLE_PRICE = BondDataSets.SETTLE_PRICE_USD;

  private static final LegalEntityId ISSUER_ID = PRODUCT.getDeliveryBasket().get(0).getLegalEntityId();
  private static final RepoGroup REPO_GROUP = RepoGroup.of("Repo");
  private static final LegalEntityGroup ISSUER_GROUP = LegalEntityGroup.of("Issuer");
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final QuoteId QUOTE_ID = QuoteId.of(PRODUCT.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
  private static final CurveId REPO_CURVE_ID = CurveId.of("Default", "Repo");
  private static final CurveId ISSUER_CURVE_ID = CurveId.of("Default", "Issuer");
  public static final LegalEntityDiscountingMarketDataLookup LOOKUP = LegalEntityDiscountingMarketDataLookup.of(
      ImmutableMap.of(ISSUER_ID, REPO_GROUP),
      ImmutableMap.of(Pair.of(REPO_GROUP, CURRENCY), REPO_CURVE_ID),
      ImmutableMap.of(ISSUER_ID, ISSUER_GROUP),
      ImmutableMap.of(Pair.of(ISSUER_GROUP, CURRENCY), ISSUER_CURVE_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(LOOKUP);
  private static final LocalDate VAL_DATE = TRADE.getProduct().getFirstNoticeDate().minusDays(7);
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    BondFutureTradeCalculationFunction<BondFutureTrade> function = BondFutureTradeCalculationFunction.TRADE;
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(QUOTE_ID, REPO_CURVE_ID, ISSUER_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    BondFutureTradeCalculationFunction<BondFutureTrade> function = BondFutureTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, SETTLE_PRICE);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, SETTLE_PRICE);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));
  }

  public void test_pv01_calibrated() {
    BondFutureTradeCalculationFunction<BondFutureTrade> function = BondFutureTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
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
    BondFutureTradeCalculationFunction<BondFutureTrade> function = BondFutureTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    DiscountingBondFutureTradePricer pricer = DiscountingBondFutureTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RTRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    CurrencyParameterSensitivities expectedPv01CalBucketed = MQ_CALC.sensitivity(pvParamSens.multipliedBy(1e-4), provider);
    MultiCurrencyAmount expectedPv01Cal = expectedPv01CalBucketed.total();

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_MARKET_QUOTE_SUM,
        Measures.PV01_MARKET_QUOTE_BUCKETED);
    Map<Measure, Result<?>> computed = function.calculate(TRADE, measures, PARAMS, md, REF_DATA);
    MultiCurrencyScenarioArray sumComputed = (MultiCurrencyScenarioArray) computed.get(Measures.PV01_MARKET_QUOTE_SUM).getValue();
    @SuppressWarnings("unchecked")
    ScenarioArray<CurrencyParameterSensitivities> bucketedComputed =
        (ScenarioArray<CurrencyParameterSensitivities>) computed.get(Measures.PV01_MARKET_QUOTE_BUCKETED).getValue();
    assertEquals(sumComputed.getScenarioCount(), 1);
    assertEquals(sumComputed.get(0).getCurrencies(), ImmutableSet.of(USD));
    assertTrue(DoubleMath.fuzzyEquals(
        sumComputed.get(0).getAmount(USD).getAmount(),
        expectedPv01Cal.getAmount(USD).getAmount(),
        1.0e-10));
    assertEquals(bucketedComputed.getScenarioCount(), 1);
    assertTrue(bucketedComputed.get(0).equalWithTolerance(expectedPv01CalBucketed, 1.0e-10));
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
        VAL_DATE,
        ImmutableMap.of(
            REPO_CURVE_ID, repoCurve,
            ISSUER_CURVE_ID, issuerCurve,
            QUOTE_ID, SETTLE_PRICE * 100),
        ImmutableMap.of());
  }

}
