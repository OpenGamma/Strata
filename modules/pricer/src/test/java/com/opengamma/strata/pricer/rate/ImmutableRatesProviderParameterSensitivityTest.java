/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveEUR;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Tests related to {@link ImmutableRatesProvider} for the computation of curve parameters sensitivities.
 */
@Test
public class ImmutableRatesProviderParameterSensitivityTest {

  private static final LocalDate DATE_VAL = LocalDate.of(2014, 1, 22);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final LocalDate DATE_1 = LocalDate.of(2015, 12, 21);
  private static final LocalDate DATE_2 = LocalDate.of(2016, 1, 21);
  private static final LocalDate DATE_3 = LocalDate.of(2016, 3, 21);
  private static final double AMOUNT_1 = 1000.0;

  private static final PointSensitivities POINT_ZERO_1 =
      PointSensitivities.of(ZeroRateSensitivity.of(USD, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_2 =
      PointSensitivities.of(ZeroRateSensitivity.of(USD, DATE_2, AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_3 =
      PointSensitivities.of(ZeroRateSensitivity.of(EUR, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_4 =
      PointSensitivities.of(ZeroRateSensitivity.of(EUR, USD, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_1 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_2 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, DATE_3, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_3 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, EUR, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_4 =
      PointSensitivities.of(IborRateSensitivity.of(EUR_EURIBOR_3M, EUR, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_ON_1 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_ON_2 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, USD, DATE_1, DATE_2, AMOUNT_1));
  private static final PointSensitivities POINT_ON_3 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, USD, DATE_2, DATE_3, AMOUNT_1));
  private static final PointSensitivities POINT_ON_4 =
      PointSensitivities.of(OvernightRateSensitivity.of(EUR_EONIA, DATE_1, AMOUNT_1));
  private static final PointSensitivities[] POINTS = new PointSensitivities[] {
      POINT_ZERO_1, POINT_ZERO_2, POINT_ZERO_3, POINT_ZERO_4,
      POINT_IBOR_1, POINT_IBOR_2, POINT_IBOR_3, POINT_IBOR_4,
      POINT_ON_1, POINT_ON_2, POINT_ON_3, POINT_ON_4};
  private static final PointSensitivities POINT =
      POINT_ZERO_1.combinedWith(POINT_ZERO_2).combinedWith(POINT_ZERO_3).combinedWith(POINT_ZERO_4)
          .combinedWith(POINT_IBOR_1).combinedWith(POINT_IBOR_2).combinedWith(POINT_IBOR_3).combinedWith(POINT_IBOR_4)
          .combinedWith(POINT_ON_1).combinedWith(POINT_ON_2).combinedWith(POINT_ON_3).combinedWith(POINT_ON_4);

  // curve providers
  private static final LocalDateDoubleTimeSeries TS_EMTPY = LocalDateDoubleTimeSeries.empty();
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_USD_PAIR =
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL3();
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_EUR_PAIR =
      StandardDataSetsMulticurveEUR.getCurvesEurOisE3();
  private static final FxMatrix FX_MATRIX = FxMatrix.builder()
      .addRate(EUR, USD, 1.20)
      .addRate(GBP, USD, 1.60)
      .build();
  private static final MulticurveProviderDiscount MULTICURVE = MULTICURVE_USD_PAIR.getFirst();
  static {
    MULTICURVE.setCurve(EUR, MULTICURVE_EUR_PAIR.getFirst().getCurve(EUR));
    MULTICURVE.setCurve(Legacy.iborIndex(EUR_EURIBOR_3M),
        MULTICURVE_EUR_PAIR.getFirst().getCurve(Legacy.iborIndex(EUR_EURIBOR_3M)));
    MULTICURVE.setCurve(Legacy.overnightIndex(EUR_EONIA),
        MULTICURVE_EUR_PAIR.getFirst().getCurve(Legacy.overnightIndex(EUR_EONIA)));
    MULTICURVE.setForexMatrix(FX_MATRIX);
  }

  // rates provider
  private static RatesProvider PROVIDER = ImmutableRatesProvider.builder()
      .valuationDate(DATE_VAL)
      .fxMatrix(FX_MATRIX)
      .discountCurves(Legacy.discountCurves(MULTICURVE))
      .indexCurves(Legacy.indexCurves(MULTICURVE))
      .timeSeries(ImmutableMap.of(
          EUR_EURIBOR_3M, TS_EMTPY,
          USD_LIBOR_1M, TS_EMTPY,
          USD_LIBOR_3M, TS_EMTPY,
          EUR_EONIA, TS_EMTPY,
          USD_FED_FUND, TS_EMTPY))
      .dayCount(ACT_ACT_ISDA)
      .build();

  private static final double TOLERANCE_SENSI = 1.0E-8;

  public void pointToParameterOnePointZero() {
    CurveParameterSensitivities ps = PROVIDER.parameterSensitivity(POINT_ZERO_1);
    DoublesPair pair = DoublesPair.of(PROVIDER.relativeTime(DATE_1), AMOUNT_1);
    List<DoublesPair> list = new ArrayList<>();
    list.add(pair);
    double[] vectorExpected = MULTICURVE.parameterSensitivity(MULTICURVE.getName(USD), list);
    NameCurrencySensitivityKey key = NameCurrencySensitivityKey.of(MULTICURVE.getName(USD), USD);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.of(key, vectorExpected);
    assertTrue(ps.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  public void pointToParameterOnePointZeroTwoCurrency() {
    CurveParameterSensitivities ps = PROVIDER.parameterSensitivity(POINT_ZERO_4);
    DoublesPair pair = DoublesPair.of(PROVIDER.relativeTime(DATE_1), AMOUNT_1);
    List<DoublesPair> list = new ArrayList<>();
    list.add(pair);
    double[] vectorExpected = MULTICURVE.parameterSensitivity(MULTICURVE.getName(EUR), list);
    NameCurrencySensitivityKey key = NameCurrencySensitivityKey.of(MULTICURVE.getName(EUR), USD);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.of(key, vectorExpected);
    assertTrue(ps.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  public void pointToParameterOnePointIbor() {
    CurveParameterSensitivities ps = PROVIDER.parameterSensitivity(POINT_IBOR_1);
    LocalDate startDate = USD_LIBOR_3M.calculateEffectiveFromFixing(DATE_1);
    LocalDate endDate = USD_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double startTime = PROVIDER.relativeTime(startDate);
    double endTime = PROVIDER.relativeTime(endDate);
    double af = USD_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    ForwardSensitivity fwd = new SimplyCompoundedForwardSensitivity(startTime, endTime, af, AMOUNT_1);
    List<ForwardSensitivity> list = new ArrayList<>();
    list.add(fwd);
    String curveName = MULTICURVE.getName(Legacy.iborIndex(USD_LIBOR_3M));
    double[] vectorExpected = MULTICURVE.parameterForwardSensitivity(curveName, list);
    NameCurrencySensitivityKey key = NameCurrencySensitivityKey.of(curveName, USD);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.of(key, vectorExpected);
    assertTrue(ps.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  public void pointToParameterOnePointOnOneDate() {
    CurveParameterSensitivities ps = PROVIDER.parameterSensitivity(POINT_ON_1);
    LocalDate startDate = USD_FED_FUND.calculateEffectiveFromFixing(DATE_1);
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(startDate);
    double startTime = PROVIDER.relativeTime(startDate);
    double endTime = PROVIDER.relativeTime(endDate);
    double af = USD_FED_FUND.getDayCount().yearFraction(startDate, endDate);
    ForwardSensitivity fwd = new SimplyCompoundedForwardSensitivity(startTime, endTime, af, AMOUNT_1);
    List<ForwardSensitivity> list = new ArrayList<>();
    list.add(fwd);
    String curveName = MULTICURVE.getName(Legacy.overnightIndex(USD_FED_FUND));
    double[] vectorExpected = MULTICURVE.parameterForwardSensitivity(curveName, list);
    NameCurrencySensitivityKey key = NameCurrencySensitivityKey.of(curveName, USD);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.of(key, vectorExpected);
    assertTrue(ps.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  public void pointToParameterOnePointOnTwoDates() {
    CurveParameterSensitivities psComputed = PROVIDER.parameterSensitivity(POINT_ON_2);
    LocalDate startDate = USD_FED_FUND.calculateEffectiveFromFixing(DATE_1);
    double startTime = PROVIDER.relativeTime(startDate);
    double endTime = PROVIDER.relativeTime(DATE_2);
    double af = USD_FED_FUND.getDayCount().yearFraction(startDate, DATE_2);
    ForwardSensitivity fwd = new SimplyCompoundedForwardSensitivity(startTime, endTime, af, AMOUNT_1);
    List<ForwardSensitivity> list = new ArrayList<>();
    list.add(fwd);
    String curveName = MULTICURVE.getName(Legacy.overnightIndex(USD_FED_FUND));
    double[] vectorExpected = MULTICURVE.parameterForwardSensitivity(curveName, list);
    NameCurrencySensitivityKey key = NameCurrencySensitivityKey.of(curveName, USD);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.of(key, vectorExpected);
    assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  public void pointToParameterMultiple() {
    CurveParameterSensitivities psComputed = PROVIDER.parameterSensitivity(POINT);
    assertEquals(psComputed.getSensitivities().size(), 6);
    CurveParameterSensitivities psExpected = CurveParameterSensitivities.empty();
    for (int i = 0; i < POINTS.length; i++) {
      psExpected = psExpected.combinedWith(PROVIDER.parameterSensitivity(POINTS[i]));
    }
    assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_SENSI));
  }

  //-------------------------------------------------------------------------
  private static final double GBP_DSC = 0.99d;
  private static final double USD_DSC = 0.95d;
  private static final double EPS_FD = 1.0e-7;
  private static final Curve DISCOUNT_CURVE_GBP = new ConstantDiscountFactorCurve("GBP-Discount", GBP_DSC);
  private static final Curve DISCOUNT_CURVE_GBP_UP = new ConstantDiscountFactorCurve("GBP-DiscountUp", GBP_DSC + EPS_FD);
  private static final Curve DISCOUNT_CURVE_GBP_DOWN = new ConstantDiscountFactorCurve("GBP-DiscountDown", GBP_DSC - EPS_FD);
  private static final Curve DISCOUNT_CURVE_USD = new ConstantDiscountFactorCurve("USD-Discount", USD_DSC);
  private static final Curve DISCOUNT_CURVE_USD_UP = new ConstantDiscountFactorCurve("USD-DiscountUp", USD_DSC + EPS_FD);
  private static final Curve DISCOUNT_CURVE_USD_DOWN = new ConstantDiscountFactorCurve("USD-DiscountDown", USD_DSC - EPS_FD);

  public void pointAndParameterFx() {
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.empty();
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(DATE_VAL)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    ImmutableRatesProvider test_gbp_up = ImmutableRatesProvider.builder()
        .valuationDate(DATE_VAL)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP_UP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    ImmutableRatesProvider test_gbp_dw = ImmutableRatesProvider.builder()
        .valuationDate(DATE_VAL)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP_DOWN, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    ImmutableRatesProvider test_usd_up = ImmutableRatesProvider.builder()
        .valuationDate(DATE_VAL)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD_UP))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    ImmutableRatesProvider test_usd_dw = ImmutableRatesProvider.builder()
        .valuationDate(DATE_VAL)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD_DOWN))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, ts))
        .dayCount(ACT_ACT_ISDA)
        .build();
    LocalDate matuirtyDate = WM_GBP_USD.calculateMaturityFromFixing(DATE_VAL);
    double maturityTime = test.relativeTime(matuirtyDate);
    // GBP based
    PointSensitivityBuilder sensiBuildCmpGBP = test.fxIndexRates(WM_GBP_USD).pointSensitivity(GBP, DATE_VAL);
    FxIndexSensitivity sensiBuildExpGBP = FxIndexSensitivity.of(WM_GBP_USD, USD, GBP, DATE_VAL, 1.0);
    assertTrue(sensiBuildCmpGBP.equals(sensiBuildExpGBP));
    double sense_gbp1 = 0.5 * (test_gbp_up.fxIndexRates(WM_GBP_USD).rate(GBP, DATE_VAL) -
        test_gbp_dw.fxIndexRates(WM_GBP_USD).rate(GBP, DATE_VAL)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd1 = 0.5 * (test_usd_up.fxIndexRates(WM_GBP_USD).rate(GBP, DATE_VAL) -
        test_usd_dw.fxIndexRates(WM_GBP_USD).rate(GBP, DATE_VAL)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecGBP = ZeroRateSensitivity.of(GBP, USD, matuirtyDate, sense_gbp1);
    sensiBuildDecGBP = sensiBuildDecGBP.combinedWith(ZeroRateSensitivity.of(USD, USD, matuirtyDate, sense_usd1));
    CurveParameterSensitivities paramSensiCmpGBP = test.parameterSensitivity(sensiBuildCmpGBP.build().normalized());
    CurveParameterSensitivities paramSensiExpGBP = test.parameterSensitivity(sensiBuildDecGBP.build().normalized());
    assertTrue(paramSensiCmpGBP.equalWithTolerance(paramSensiExpGBP, EPS_FD));
    // USD based
    PointSensitivityBuilder sensiBuildCmpUSD = test.fxIndexRates(WM_GBP_USD).pointSensitivity(USD, DATE_VAL);
    FxIndexSensitivity sensiBuildExpUSD = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, DATE_VAL, 1.0);
    assertTrue(sensiBuildCmpUSD.equals(sensiBuildExpUSD));
    double sense_gbp2 = 0.5 * (test_gbp_up.fxIndexRates(WM_GBP_USD).rate(USD, DATE_VAL) -
        test_gbp_dw.fxIndexRates(WM_GBP_USD).rate(USD, DATE_VAL)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd2 = 0.5 * (test_usd_up.fxIndexRates(WM_GBP_USD).rate(USD, DATE_VAL) -
        test_usd_dw.fxIndexRates(WM_GBP_USD).rate(USD, DATE_VAL)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecUSD = ZeroRateSensitivity.of(GBP, GBP, matuirtyDate, sense_gbp2);
    sensiBuildDecUSD = sensiBuildDecUSD.combinedWith(ZeroRateSensitivity.of(USD, GBP, matuirtyDate, sense_usd2));
    CurveParameterSensitivities paramSensiCmpUSD = test.parameterSensitivity(sensiBuildCmpUSD.build().normalized());
    CurveParameterSensitivities paramSensiExpUSD = test.parameterSensitivity(sensiBuildDecUSD.build().normalized());
    assertTrue(paramSensiCmpUSD.equalWithTolerance(paramSensiExpUSD, EPS_FD));
  }

  // a curve that produces a constant discount factor
  static class ConstantDiscountFactorCurve implements Curve {

    private CurveMetadata metadata;
    private double discountFactor;

    public ConstantDiscountFactorCurve(String name, double discountFactor) {
      this.metadata = CurveMetadata.of(name);
      this.discountFactor = discountFactor;
    }

    @Override
    public CurveMetadata getMetadata() {
      return metadata;
    }

    @Override
    public int getParameterCount() {
      return 1;
    }

    @Override
    public double yValue(double x) {
      return -Math.log(discountFactor) / x;
    }

    @Override
    public double[] yValueParameterSensitivity(double x) {
      return new double[] {1d};
    }

    @Override
    public double firstDerivative(double x) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MetaBean metaBean() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <R> Property<R> property(String arg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> propertyNames() {
      throw new UnsupportedOperationException();
    }

  }
  
}
