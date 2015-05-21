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
import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.market.curve.ForwardPriceIndexValues;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;

/**
 * Test.
 */
@Test
public class ForwardInflationMonthlyRateObservationFnTest {

  private static final Interpolator1D INTERPOLATOR = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
      Interpolator1DFactory.LINEAR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final YearMonth VAL_MONTH = YearMonth.of(2014, 6);

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REFERENCE_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REFERENCE_END_MONTH = YearMonth.of(2015, 10);
  private static final double RATE_START = 317.0;
  private static final double RATE_END = 344.0;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;

  //-------------------------------------------------------------------------
  public void test_rate() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_END);

    InflationMonthlyRateObservation ro =
        InflationMonthlyRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;

    double rateExpected = RATE_END / RATE_START - 1.0;
    assertEquals(obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov), rateExpected, EPS);
  }

  //-------------------------------------------------------------------------
  public void test_rateSensitivity() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_END);
    ImmutableRatesProvider provStartUp = createProvider(RATE_START + EPS_FD, RATE_END);
    ImmutableRatesProvider provStartDw = createProvider(RATE_START - EPS_FD, RATE_END);
    ImmutableRatesProvider provEndUp = createProvider(RATE_START, RATE_END + EPS_FD);
    ImmutableRatesProvider provEndDw = createProvider(RATE_START, RATE_END - EPS_FD);

    InflationMonthlyRateObservation ro =
        InflationMonthlyRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;

    double rateSrtUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provStartUp);
    double rateSrtDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provStartDw);
    double rateEndUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndUp);
    double rateEndDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndDw);

    PointSensitivityBuilder sensiStr =
        InflationRateSensitivity.of(GB_RPIX, REFERENCE_START_MONTH, 0.5 * (rateSrtUp - rateSrtDw) / EPS_FD);
    PointSensitivityBuilder sensiEnd =
        InflationRateSensitivity.of(GB_RPIX, REFERENCE_END_MONTH, 0.5 * (rateEndUp - rateEndDw) / EPS_FD);
    PointSensitivityBuilder sensiExpected = sensiStr.combinedWith(sensiEnd);

    PointSensitivityBuilder sensiComputed =
        obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }

  private ImmutableRatesProvider createProvider(
      double rateStart,
      double rateEnd) {

    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_MONTH.atEndOfMonth(), 300);
    InterpolatedDoublesCurve curve = InterpolatedDoublesCurve.from(
        new double[] {4, 16}, new double[] {rateStart, rateEnd}, INTERPOLATOR);
    ForwardPriceIndexValues values = ForwardPriceIndexValues.of(GB_RPIX, VAL_MONTH, timeSeries, curve);
    return ImmutableRatesProvider.builder()
        .valuationDate(DUMMY_ACCRUAL_END_DATE)
        .dayCount(DayCounts.ACT_360)
        .additionalData(ImmutableMap.of(PriceIndexProvider.class, PriceIndexProvider.of(GB_RPIX, values)))
        .build();
  }

}
