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

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.ForwardPriceIndexValues;
import com.opengamma.strata.market.value.PriceIndexValues;
import com.opengamma.strata.pricer.datasets.StandardDataSets;

/**
 * Tests related to {@link ImmutableRatesProvider} for the computation of curve parameters sensitivities.
 */
@Test
public class ImmutableRatesProviderParameterSensitivityTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ISDA;
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
      PointSensitivities.of(ZeroRateSensitivity.of(EUR, DATE_1, USD, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_1 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_2 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, DATE_3, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_3 =
      PointSensitivities.of(IborRateSensitivity.of(USD_LIBOR_3M, DATE_1, EUR, AMOUNT_1));
  private static final PointSensitivities POINT_IBOR_4 =
      PointSensitivities.of(IborRateSensitivity.of(EUR_EURIBOR_3M, DATE_1, EUR, AMOUNT_1));
  private static final PointSensitivities POINT_ON_1 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, DATE_1, AMOUNT_1));
  private static final PointSensitivities POINT_ON_2 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, DATE_1, DATE_2, USD, AMOUNT_1));
  private static final PointSensitivities POINT_ON_3 =
      PointSensitivities.of(OvernightRateSensitivity.of(USD_FED_FUND, DATE_2, DATE_3, USD, AMOUNT_1));
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

  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, 1.60);

  // rates provider
  private static RatesProvider PROVIDER = StandardDataSets.providerUsdEurDscL3();

  private static final double TOLERANCE_SENSI = 1.0E-8;

  //-------------------------------------------------------------------------
  public void pointToParameterMultiple() {
    CurveCurrencyParameterSensitivities psComputed = PROVIDER.curveParameterSensitivity(POINT);
    assertEquals(psComputed.getSensitivities().size(), 6);
    CurveCurrencyParameterSensitivities psExpected = CurveCurrencyParameterSensitivities.empty();
    for (int i = 0; i < POINTS.length; i++) {
      psExpected = psExpected.combinedWith(PROVIDER.curveParameterSensitivity(POINTS[i]));
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
    ImmutableRatesProvider test = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    ImmutableRatesProvider test_gbp_up = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP_UP, USD, DISCOUNT_CURVE_USD))
        .build();
    ImmutableRatesProvider test_gbp_dw = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP_DOWN, USD, DISCOUNT_CURVE_USD))
        .build();
    ImmutableRatesProvider test_usd_up = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD_UP))
        .build();
    ImmutableRatesProvider test_usd_dw = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD_DOWN))
        .build();
    LocalDate matuirtyDate = GBP_USD_WM.calculateMaturityFromFixing(VAL_DATE);
    double maturityTime = DAY_COUNT.relativeYearFraction(VAL_DATE, matuirtyDate);
    // GBP based
    PointSensitivityBuilder sensiBuildCmpGBP = test.fxIndexRates(GBP_USD_WM).ratePointSensitivity(GBP, VAL_DATE);
    FxIndexSensitivity sensiBuildExpGBP = FxIndexSensitivity.of(GBP_USD_WM, GBP, VAL_DATE, USD, 1.0);
    assertTrue(sensiBuildCmpGBP.equals(sensiBuildExpGBP));
    double sense_gbp1 = 0.5 * (test_gbp_up.fxIndexRates(GBP_USD_WM).rate(GBP, VAL_DATE) -
        test_gbp_dw.fxIndexRates(GBP_USD_WM).rate(GBP, VAL_DATE)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd1 = 0.5 * (test_usd_up.fxIndexRates(GBP_USD_WM).rate(GBP, VAL_DATE) -
        test_usd_dw.fxIndexRates(GBP_USD_WM).rate(GBP, VAL_DATE)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecGBP = ZeroRateSensitivity.of(GBP, matuirtyDate, USD, sense_gbp1);
    sensiBuildDecGBP = sensiBuildDecGBP.combinedWith(ZeroRateSensitivity.of(USD, matuirtyDate, USD, sense_usd1));
    CurveCurrencyParameterSensitivities paramSensiCmpGBP = test.curveParameterSensitivity(sensiBuildCmpGBP.build().normalized());
    CurveCurrencyParameterSensitivities paramSensiExpGBP = test.curveParameterSensitivity(sensiBuildDecGBP.build().normalized());
    assertTrue(paramSensiCmpGBP.equalWithTolerance(paramSensiExpGBP, EPS_FD));
    // USD based
    PointSensitivityBuilder sensiBuildCmpUSD = test.fxIndexRates(GBP_USD_WM).ratePointSensitivity(USD, VAL_DATE);
    FxIndexSensitivity sensiBuildExpUSD = FxIndexSensitivity.of(GBP_USD_WM, USD, VAL_DATE, GBP, 1.0);
    assertTrue(sensiBuildCmpUSD.equals(sensiBuildExpUSD));
    double sense_gbp2 = 0.5 * (test_gbp_up.fxIndexRates(GBP_USD_WM).rate(USD, VAL_DATE) -
        test_gbp_dw.fxIndexRates(GBP_USD_WM).rate(USD, VAL_DATE)) / EPS_FD * (-maturityTime * GBP_DSC);
    double sense_usd2 = 0.5 * (test_usd_up.fxIndexRates(GBP_USD_WM).rate(USD, VAL_DATE) -
        test_usd_dw.fxIndexRates(GBP_USD_WM).rate(USD, VAL_DATE)) / EPS_FD * (-maturityTime * USD_DSC);
    PointSensitivityBuilder sensiBuildDecUSD = ZeroRateSensitivity.of(GBP, matuirtyDate, GBP, sense_gbp2);
    sensiBuildDecUSD = sensiBuildDecUSD.combinedWith(ZeroRateSensitivity.of(USD, matuirtyDate, GBP, sense_usd2));
    CurveCurrencyParameterSensitivities paramSensiCmpUSD = test.curveParameterSensitivity(sensiBuildCmpUSD.build().normalized());
    CurveCurrencyParameterSensitivities paramSensiExpUSD = test.curveParameterSensitivity(sensiBuildDecUSD.build().normalized());
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
    PriceIndexValues values = ForwardPriceIndexValues.of(
        GB_RPI,
        valuationDate,
        LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 200),
        interpCurve);
    ImmutableRatesProvider provider = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .priceIndexValues(ImmutableMap.of(GB_RPI, values))
        .build();

    double pointSensiValue = 2.5;
    YearMonth refMonth = YearMonth.from(valuationDate.plusMonths(9));
    InflationRateSensitivity pointSensi = InflationRateSensitivity.of(GB_RPI, refMonth, pointSensiValue);
    CurveCurrencyParameterSensitivities computed = provider.curveParameterSensitivity(pointSensi.build());
    DoubleArray sensiComputed = computed.getSensitivities().get(0).getSensitivity();
    DoubleArray sensiExpectedUnit =
        provider.priceIndexValues(GB_RPI).unitParameterSensitivity(refMonth).getSensitivities().get(0).getSensitivity();
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
    public int getParameterCount() {
      return 1;
    }

    @Override
    public double yValue(double x) {
      return -Math.log(discountFactor) / x;
    }

    @Override
    public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
      return CurveUnitParameterSensitivity.of(metadata, DoubleArray.of(1d));
    }

    @Override
    public double firstDerivative(double x) {
      throw new UnsupportedOperationException();
    }
  }
  
}
