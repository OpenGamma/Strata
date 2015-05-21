/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.PriceIndices.GB_RPIX;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.market.curve.ForwardPriceIndexValues;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;

/**
 * Test.
 */
@Test
public class ForwardInflationInterpolatedRateObservationFnTest {

  private static final Interpolator1D INTERPOLATOR = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
      Interpolator1DFactory.LINEAR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final YearMonth VAL_MONTH = YearMonth.of(2014, 6);

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REF_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REF_START_MONTH_INTERP = YearMonth.of(2014, 11);
  private static final YearMonth REF_END_MONTH = YearMonth.of(2015, 10);
  private static final YearMonth REF_END_MONTH_INTERP = YearMonth.of(2015, 11);
  private static final double RATE_START = 317.0;
  private static final double RATE_START_INTERP = 325.0;
  private static final double RATE_END = 344.0;
  private static final double RATE_END_INTERP = 349.0;
  private static final double WEIGHT = 0.5;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;

  //-------------------------------------------------------------------------
  public void test_rate() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP);

    InflationInterpolatedRateObservation ro =
        InflationInterpolatedRateObservation.of(GB_RPIX, REF_START_MONTH, REF_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateObservationFn obsFn = ForwardInflationInterpolatedRateObservationFn.DEFAULT;

    double rateExpected = (WEIGHT * RATE_END + (1.0 - WEIGHT) * RATE_END_INTERP) /
        (WEIGHT * RATE_START + (1.0 - WEIGHT) * RATE_START_INTERP) - 1.0;
    assertEquals(obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov), rateExpected, EPS);
  }

  //-------------------------------------------------------------------------
  public void test_rateSensitivity() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtUp = createProvider(RATE_START + EPS_FD, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtDw = createProvider(RATE_START - EPS_FD, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtIntUp = createProvider(RATE_START, RATE_START_INTERP + EPS_FD, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtIntDw = createProvider(RATE_START, RATE_START_INTERP - EPS_FD, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provEndUp = createProvider(RATE_START, RATE_START_INTERP, RATE_END + EPS_FD, RATE_END_INTERP);
    ImmutableRatesProvider provEndDw = createProvider(RATE_START, RATE_START_INTERP, RATE_END - EPS_FD, RATE_END_INTERP);
    ImmutableRatesProvider provEndIntUp = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP + EPS_FD);
    ImmutableRatesProvider provEndIntDw = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP - EPS_FD);

    InflationInterpolatedRateObservation ro =
        InflationInterpolatedRateObservation.of(GB_RPIX, REF_START_MONTH, REF_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateObservationFn obsFn = ForwardInflationInterpolatedRateObservationFn.DEFAULT;

    double rateSrtUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtUp);
    double rateSrtDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtDw);
    double rateSrtIntUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtIntUp);
    double rateSrtIntDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtIntDw);
    double rateEndUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndUp);
    double rateEndDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndDw);
    double rateEndIntUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndIntUp);
    double rateEndIntDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndIntDw);

    PointSensitivityBuilder sensSrt =
        InflationRateSensitivity.of(GB_RPIX, REF_START_MONTH, 0.5 * (rateSrtUp - rateSrtDw) / EPS_FD);
    PointSensitivityBuilder sensSrtInt =
        InflationRateSensitivity.of(GB_RPIX, REF_START_MONTH_INTERP, 0.5 * (rateSrtIntUp - rateSrtIntDw) / EPS_FD);
    PointSensitivityBuilder sensEnd =
        InflationRateSensitivity.of(GB_RPIX, REF_END_MONTH, 0.5 * (rateEndUp - rateEndDw) / EPS_FD);
    PointSensitivityBuilder sensEndInt =
        InflationRateSensitivity.of(GB_RPIX, REF_END_MONTH_INTERP, 0.5 * (rateEndIntUp - rateEndIntDw) / EPS_FD);
    PointSensitivityBuilder sensiExpected =
        sensSrt.combinedWith(sensSrtInt).combinedWith(sensEnd).combinedWith(sensEndInt);

    PointSensitivityBuilder sensiComputed =
        obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }

  private ImmutableRatesProvider createProvider(
      double rateStart,
      double rateStartInterp,
      double rateEnd,
      double rateEndInterp) {

    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_MONTH.atEndOfMonth(), 300);
    InterpolatedDoublesCurve curve = InterpolatedDoublesCurve.from(
        new double[] {4, 5, 16, 17}, new double[] {rateStart, rateStartInterp, rateEnd, rateEndInterp}, INTERPOLATOR);
    ForwardPriceIndexValues values = ForwardPriceIndexValues.of(GB_RPIX, VAL_MONTH, timeSeries, curve);
    return ImmutableRatesProvider.builder()
        .valuationDate(DUMMY_ACCRUAL_END_DATE)
        .dayCount(DayCounts.ACT_360)
        .additionalData(ImmutableMap.of(PriceIndexProvider.class, PriceIndexProvider.of(GB_RPIX, values)))
        .build();
  }

}
