/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.base.Preconditions;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.StandardDataSets;
import com.opengamma.strata.pricer.fx.FxIndexSensitivity;

/**
 * Tests related to {@link ImmutableRatesProvider} for the computation of curve parameters sensitivities.
 */
@Test
public class ImmutableRatesProviderParameterSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ISDA;
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final LocalDate DATE_1 = LocalDate.of(2015, 12, 21);
  private static final LocalDate DATE_2 = LocalDate.of(2016, 1, 21);
  private static final LocalDate DATE_3 = LocalDate.of(2016, 3, 21);
  private static final double AMOUNT_1 = 1000.0;

  private static final PointSensitivities POINT_ZERO_1 =
      PointSensitivities.of(ZeroRateSensitivity.of(USD, DAY_COUNT.relativeYearFraction(VAL_DATE, DATE_1), AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_2 =
      PointSensitivities.of(ZeroRateSensitivity.of(USD, DAY_COUNT.relativeYearFraction(VAL_DATE, DATE_2), AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_3 =
      PointSensitivities.of(ZeroRateSensitivity.of(EUR, DAY_COUNT.relativeYearFraction(VAL_DATE, DATE_1), AMOUNT_1));
  private static final PointSensitivities POINT_ZERO_4 =
      PointSensitivities.of(ZeroRateSensitivity.of(EUR, DAY_COUNT.relativeYearFraction(VAL_DATE, DATE_1), USD, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_1 =
      PointSensitivities.of(
          IborRateSensitivity.of(IborIndexObservation.of(USD_LIBOR_3M, DATE_1, REF_DATA), AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_2 =
      PointSensitivities.of(
          IborRateSensitivity.of(IborIndexObservation.of(USD_LIBOR_3M, DATE_3, REF_DATA), AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_3 =
      PointSensitivities.of(
          IborRateSensitivity.of(IborIndexObservation.of(USD_LIBOR_3M, DATE_1, REF_DATA), EUR, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_4 =
      PointSensitivities.of(
          IborRateSensitivity.of(IborIndexObservation.of(EUR_EURIBOR_3M, DATE_1, REF_DATA), EUR, AMOUNT_1));
  private static final PointSensitivities POINT_ON_1 =
      PointSensitivities.of(
          OvernightRateSensitivity.of(OvernightIndexObservation.of(USD_FED_FUND, DATE_1, REF_DATA), AMOUNT_1));
  private static final PointSensitivities POINT_ON_2 =
      PointSensitivities.of(
          OvernightRateSensitivity.ofPeriod(OvernightIndexObservation.of(USD_FED_FUND, DATE_1, REF_DATA), DATE_2, USD, AMOUNT_1));
  private static final PointSensitivities POINT_ON_3 =
      PointSensitivities.of(
          OvernightRateSensitivity.ofPeriod(OvernightIndexObservation.of(USD_FED_FUND, DATE_2, REF_DATA), DATE_3, USD, AMOUNT_1));
  private static final PointSensitivities POINT_ON_4 =
      PointSensitivities.of(
          OvernightRateSensitivity.of(OvernightIndexObservation.of(EUR_EONIA, DATE_1, REF_DATA), AMOUNT_1));
  private static final PointSensitivities[] POINTS = new PointSensitivities[] {
      POINT_ZERO_1, POINT_ZERO_2, POINT_ZERO_3, POINT_ZERO_4,
      POINT_IBOR_1, POINT_IBOR_2, POINT_IBOR_3, POINT_IBOR_4,
      POINT_ON_1, POINT_ON_2, POINT_ON_3, POINT_ON_4};
  private static final PointSensitivities POINT =
      POINT_ZERO_1.combinedWith(POINT_ZERO_2).combinedWith(POINT_ZERO_3).combinedWith(POINT_ZERO_4)
          .combinedWith(POINT_IBOR_1).combinedWith(POINT_IBOR_2).combinedWith(POINT_IBOR_3).combinedWith(POINT_IBOR_4)
          .combinedWith(POINT_ON_1).combinedWith(POINT_ON_2).combinedWith(POINT_ON_3).combinedWith(POINT_ON_4);

  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, 1.60);

  // rates provider
  private static RatesProvider PROVIDER = StandardDataSets.providerUsdEurDscL3();

  private static final double TOLERANCE_SENSI = 1.0E-8;

  //-------------------------------------------------------------------------
  public void pointToParameterMultiple() {
    CurrencyParameterSensitivities psComputed = PROVIDER.parameterSensitivity(POINT);
    assertEquals(psComputed.getSensitivities().size(), 6);
    CurrencyParameterSensitivities psExpected = CurrencyParameterSensitivities.empty();
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
    ImmutableRatesProvider test = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    ImmutableRatesProvider test_gbp_up = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP_UP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    ImmutableRatesProvider test_gbp_dw = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP_DOWN)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    ImmutableRatesProvider test_usd_up = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD_UP)
        .build();
    ImmutableRatesProvider test_usd_dw = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD_DOWN)
        .build();
    LocalDate matuirtyDate = GBP_USD_WM.calculateMaturityFromFixing(VAL_DATE, REF_DATA);
    double maturityTime = DAY_COUNT.relativeYearFraction(VAL_DATE, matuirtyDate);
    // GBP based
    FxIndexObservation obs = FxIndexObservation.of(GBP_USD_WM, VAL_DATE, REF_DATA);
    PointSensitivityBuilder sensiBuildCmpGBP = test.fxIndexRates(GBP_USD_WM).ratePointSensitivity(obs, GBP);
    FxIndexSensitivity sensiBuildExpGBP = FxIndexSensitivity.of(obs, GBP, USD, 1.0);
    assertTrue(sensiBuildCmpGBP.equals(sensiBuildExpGBP));
    double sense_gbp1 = 0.5 * (test_gbp_up.fxIndexRates(GBP_USD_WM).rate(obs, GBP) -
        test_gbp_dw.fxIndexRates(GBP_USD_WM).rate(obs, GBP)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd1 = 0.5 * (test_usd_up.fxIndexRates(GBP_USD_WM).rate(obs, GBP) -
        test_usd_dw.fxIndexRates(GBP_USD_WM).rate(obs, GBP)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecGBP = ZeroRateSensitivity.of(GBP, maturityTime, USD, sense_gbp1);
    sensiBuildDecGBP = sensiBuildDecGBP.combinedWith(ZeroRateSensitivity.of(USD, maturityTime, USD, sense_usd1));
    CurrencyParameterSensitivities paramSensiCmpGBP = test.parameterSensitivity(sensiBuildCmpGBP.build().normalized());
    CurrencyParameterSensitivities paramSensiExpGBP = test.parameterSensitivity(sensiBuildDecGBP.build().normalized());
    assertTrue(paramSensiCmpGBP.equalWithTolerance(paramSensiExpGBP, EPS_FD));
    // USD based
    PointSensitivityBuilder sensiBuildCmpUSD = test.fxIndexRates(GBP_USD_WM).ratePointSensitivity(obs, USD);
    FxIndexSensitivity sensiBuildExpUSD = FxIndexSensitivity.of(obs, USD, GBP, 1.0);
    assertTrue(sensiBuildCmpUSD.equals(sensiBuildExpUSD));
    double sense_gbp2 = 0.5 * (test_gbp_up.fxIndexRates(GBP_USD_WM).rate(obs, USD) -
        test_gbp_dw.fxIndexRates(GBP_USD_WM).rate(obs, USD)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd2 = 0.5 * (test_usd_up.fxIndexRates(GBP_USD_WM).rate(obs, USD) -
        test_usd_dw.fxIndexRates(GBP_USD_WM).rate(obs, USD)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecUSD = ZeroRateSensitivity.of(GBP, maturityTime, GBP, sense_gbp2);
    sensiBuildDecUSD = sensiBuildDecUSD.combinedWith(ZeroRateSensitivity.of(USD, maturityTime, GBP, sense_usd2));
    CurrencyParameterSensitivities paramSensiCmpUSD = test.parameterSensitivity(sensiBuildCmpUSD.build().normalized());
    CurrencyParameterSensitivities paramSensiExpUSD = test.parameterSensitivity(sensiBuildDecUSD.build().normalized());
    assertTrue(paramSensiCmpUSD.equalWithTolerance(paramSensiExpUSD, EPS_FD));
  }

  public void pointAndParameterPriceIndex() {
    double eps = 1.0e-13;
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    DoubleArray x = DoubleArray.of(0.5, 1.0, 2.0);
    DoubleArray y = DoubleArray.of(224.2, 262.6, 277.5);
    CurveInterpolator interp = CurveInterpolators.NATURAL_CUBIC_SPLINE;
    String curveName = "GB_RPI_CURVE";
    InterpolatedNodalCurve interpCurve = InterpolatedNodalCurve.of(Curves.prices(curveName), x, y, interp);
    ImmutableRatesProvider provider = ImmutableRatesProvider.builder(VAL_DATE)
        .priceIndexCurve(GB_RPI, interpCurve)
        .timeSeries(GB_RPI, LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 200))
        .build();

    double pointSensiValue = 2.5;
    YearMonth refMonth = YearMonth.from(valuationDate.plusMonths(9));
    InflationRateSensitivity pointSensi = InflationRateSensitivity.of(
        PriceIndexObservation.of(GB_RPI, refMonth), pointSensiValue);
    CurrencyParameterSensitivities computed = provider.parameterSensitivity(pointSensi.build());
    DoubleArray sensiComputed = computed.getSensitivities().get(0).getSensitivity();

    InflationRateSensitivity pointSensi1 = InflationRateSensitivity.of(PriceIndexObservation.of(GB_RPI, refMonth), 1);
    DoubleArray sensiExpectedUnit =
        provider.priceIndexValues(GB_RPI).parameterSensitivity(pointSensi1).getSensitivities().get(0).getSensitivity();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpectedUnit.multipliedBy(pointSensiValue), eps));
  }

  //-------------------------------------------------------------------------
  // a curve that produces a constant discount factor
  static class ConstantDiscountFactorCurve implements Curve {

    private CurveMetadata metadata;
    private double discountFactor;

    public ConstantDiscountFactorCurve(String name, double discountFactor) {
      this.metadata = Curves.zeroRates(name, DAY_COUNT);
      this.discountFactor = discountFactor;
    }

    @Override
    public CurveMetadata getMetadata() {
      return metadata;
    }

    @Override
    public Curve withMetadata(CurveMetadata metadata) {
      return this;
    }

    //-------------------------------------------------------------------------
    @Override
    public int getParameterCount() {
      return 1;
    }

    @Override
    public double getParameter(int parameterIndex) {
      Preconditions.checkElementIndex(parameterIndex, 1);
      return discountFactor;
    }

    @Override
    public ParameterMetadata getParameterMetadata(int parameterIndex) {
      return ParameterMetadata.empty();
    }

    @Override
    public ConstantDiscountFactorCurve withParameter(int parameterIndex, double newValue) {
      return new ConstantDiscountFactorCurve(metadata.getCurveName().toString(), newValue);
    }

    //-------------------------------------------------------------------------
    @Override
    public double yValue(double x) {
      return -Math.log(discountFactor) / x;
    }

    @Override
    public UnitParameterSensitivity yValueParameterSensitivity(double x) {
      return createParameterSensitivity(DoubleArray.of(1d));
    }

    @Override
    public double firstDerivative(double x) {
      throw new UnsupportedOperationException();
    }
  }

}
