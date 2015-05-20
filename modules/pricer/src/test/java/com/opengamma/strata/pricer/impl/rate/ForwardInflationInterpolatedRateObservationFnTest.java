/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.PriceIndices.GB_RPIX;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurve;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test.
 */
@Test
public class ForwardInflationInterpolatedRateObservationFnTest {

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REFERENCE_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REFERENCE_END_MONTH = YearMonth.of(2015, 10);
  private static final Double RELATIVE_START_TIME = 0.2; // used to pick up relevant index rate
  private static final Double RELATIVE_START_TIME_INTERP = 0.3; // used to pick up relevant index rate
  private static final Double RELATIVE_END_TIME = 1.2; // used to pick up relevant index rate
  private static final Double RELATIVE_END_TIME_INTERP = 1.3; // used to pick up relevant index rate
  private static final double RATE_START = 317.0;
  private static final double RATE_START_INTERP = 325.0;
  private static final double RATE_END = 344.0;
  private static final double RATE_END_INTERP = 349.0;
  private static final double WEIGHT = 0.5;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;

  public void test_rate() {
    RatesProvider mockProv = mock(RatesProvider.class);
    PriceIndexCurve mockCurve = mock(PriceIndexCurve.class);
    when(mockProv.data(PriceIndexProvider.class)).thenReturn(PriceIndexProvider.of(GB_RPIX, mockCurve));
    when(mockProv.timeSeries(GB_RPIX)).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockProv.relativeTime(REFERENCE_START_MONTH.atEndOfMonth())).thenReturn(RELATIVE_START_TIME);
    when(mockProv.relativeTime(REFERENCE_END_MONTH.atEndOfMonth())).thenReturn(RELATIVE_END_TIME);
    when(mockProv.relativeTime(
        REFERENCE_START_MONTH.plusMonths(1).atEndOfMonth())).thenReturn(RELATIVE_START_TIME_INTERP);
    when(mockProv.relativeTime(REFERENCE_END_MONTH.plusMonths(1).atEndOfMonth())).thenReturn(RELATIVE_END_TIME_INTERP);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME_INTERP)).thenReturn(RATE_START_INTERP);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME_INTERP)).thenReturn(RATE_END_INTERP);
    InflationInterpolatedRateObservation ro =
        InflationInterpolatedRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateObservationFn obsFn = ForwardInflationInterpolatedRateObservationFn.DEFAULT;
    double rateExpected = (WEIGHT * RATE_END + (1.0 - WEIGHT) * RATE_END_INTERP) /
        (WEIGHT * RATE_START + (1.0 - WEIGHT) * RATE_START_INTERP) - 1.0;
    assertEquals(obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv), rateExpected, EPS);
  }

  public void test_rateSensitivity() {
    InflationInterpolatedRateObservation ro = InflationInterpolatedRateObservation.of(
        GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateObservationFn obsFn = ForwardInflationInterpolatedRateObservationFn.DEFAULT;

    YearMonth refStartMonthInterp = REFERENCE_START_MONTH.plusMonths(1);
    YearMonth refEndMonthInterp = REFERENCE_END_MONTH.plusMonths(1);
    RatesProvider[] mockProvs = new RatesProvider[8];
    PriceIndexCurve[] mockCurves = new PriceIndexCurve[8];
    setRateProvider(mockProvs, mockCurves);
    when(mockCurves[0].getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START + EPS_FD);
    when(mockCurves[1].getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START - EPS_FD);
    when(mockCurves[2].getPriceIndex(RELATIVE_START_TIME_INTERP)).thenReturn(RATE_START_INTERP + EPS_FD);
    when(mockCurves[3].getPriceIndex(RELATIVE_START_TIME_INTERP)).thenReturn(RATE_START_INTERP - EPS_FD);
    when(mockCurves[4].getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END + EPS_FD);
    when(mockCurves[5].getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END - EPS_FD);
    when(mockCurves[6].getPriceIndex(RELATIVE_END_TIME_INTERP)).thenReturn(RATE_END_INTERP + EPS_FD);
    when(mockCurves[7].getPriceIndex(RELATIVE_END_TIME_INTERP)).thenReturn(RATE_END_INTERP - EPS_FD);
    YearMonth[] months = new YearMonth[] {
        REFERENCE_START_MONTH, refStartMonthInterp, REFERENCE_END_MONTH, refEndMonthInterp};
    PointSensitivityBuilder sensiExpected = PointSensitivityBuilder.none();
    for (int i = 0; i < 4; ++i) {
      double rateUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvs[2 * i]);
      double rateDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvs[2 * i + 1]);
      double sensiVal = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensi = InflationRateSensitivity.of(GB_RPIX, months[i], sensiVal);
      sensiExpected = sensiExpected.combinedWith(sensi);
    }

    RatesProvider[] mockProv = new RatesProvider[1];
    PriceIndexCurve[] mockCurve = new PriceIndexCurve[1];
    setRateProvider(mockProv, mockCurve);
    PointSensitivityBuilder sensiComputed =
        obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv[0]);

    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }

  private void setRateProvider(RatesProvider[] provs, PriceIndexCurve[] curves) {
    int n = provs.length;
    for (int i = 0; i < n; ++i) {
      provs[i] = mock(RatesProvider.class);
      curves[i] = mock(PriceIndexCurve.class);
      when(provs[i].data(PriceIndexProvider.class)).thenReturn(PriceIndexProvider.of(GB_RPIX, curves[i]));
      when(provs[i].timeSeries(GB_RPIX)).thenReturn(LocalDateDoubleTimeSeries.empty());
      when(provs[i].relativeTime(REFERENCE_START_MONTH.atEndOfMonth())).thenReturn(RELATIVE_START_TIME);
      when(provs[i].relativeTime(REFERENCE_END_MONTH.atEndOfMonth())).thenReturn(RELATIVE_END_TIME);
      when(provs[i].relativeTime(
          REFERENCE_START_MONTH.plusMonths(1).atEndOfMonth())).thenReturn(RELATIVE_START_TIME_INTERP);
      when(provs[i].relativeTime(REFERENCE_END_MONTH.plusMonths(1).atEndOfMonth()))
          .thenReturn(RELATIVE_END_TIME_INTERP);
      when(curves[i].getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START);
      when(curves[i].getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END);
      when(curves[i].getPriceIndex(RELATIVE_START_TIME_INTERP)).thenReturn(RATE_START_INTERP);
      when(curves[i].getPriceIndex(RELATIVE_END_TIME_INTERP)).thenReturn(RATE_END_INTERP);
    }
  }

}
