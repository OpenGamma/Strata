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
import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test.
 */
@Test
public class ForwardInflationMonthlyRateObservationFnTest {

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REFERENCE_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REFERENCE_END_MONTH = YearMonth.of(2015, 10);
  private static final Double RELATIVE_START_TIME = 0.2; // used to pick up relevant index rate
  private static final Double RELATIVE_END_TIME = 1.2; // used to pick up relevant index rate
  private static final double RATE_START = 317.0;
  private static final double RATE_END = 344.0;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;

  public void test_rate() {
    RatesProvider mockProv = mock(RatesProvider.class);
    PriceIndexCurve mockCurve = mock(PriceIndexCurve.class);
    when(mockProv.data(PriceIndexProvider.class)).thenReturn(PriceIndexProvider.of(GB_RPIX, mockCurve));
    when(mockProv.timeSeries(GB_RPIX)).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockProv.relativeTime(REFERENCE_START_MONTH.atEndOfMonth())).thenReturn(RELATIVE_START_TIME);
    when(mockProv.relativeTime(REFERENCE_END_MONTH.atEndOfMonth())).thenReturn(RELATIVE_END_TIME);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END);
    InflationMonthlyRateObservation ro =
        InflationMonthlyRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;
    assertEquals(obsFn.rate(
        ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv), RATE_END / RATE_START - 1.0, EPS);
  }

  public void test_rateSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    PriceIndexCurve mockCurve = mock(PriceIndexCurve.class);
    when(mockProv.data(PriceIndexProvider.class)).thenReturn(PriceIndexProvider.of(GB_RPIX, mockCurve));
    when(mockProv.timeSeries(GB_RPIX)).thenReturn(LocalDateDoubleTimeSeries.empty());
    when(mockProv.relativeTime(REFERENCE_START_MONTH.atEndOfMonth())).thenReturn(RELATIVE_START_TIME);
    when(mockProv.relativeTime(REFERENCE_END_MONTH.atEndOfMonth())).thenReturn(RELATIVE_END_TIME);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START + EPS_FD);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END);
    InflationMonthlyRateObservation ro =
        InflationMonthlyRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;

    double rateStartUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START - EPS_FD);
    double rateStartDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    when(mockCurve.getPriceIndex(RELATIVE_START_TIME)).thenReturn(RATE_START);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END + EPS_FD);
    double rateEndUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END - EPS_FD);
    double rateEndDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    when(mockCurve.getPriceIndex(RELATIVE_END_TIME)).thenReturn(RATE_END);
    double sensiValStart = 0.5 * (rateStartUp - rateStartDw) / EPS_FD;
    PointSensitivityBuilder sensiExpected = InflationRateSensitivity.of(GB_RPIX, REFERENCE_START_MONTH, sensiValStart);
    double sensiValEnd = 0.5 * (rateEndUp - rateEndDw) / EPS_FD;
    PointSensitivityBuilder sensiEnd = InflationRateSensitivity.of(GB_RPIX, REFERENCE_END_MONTH, sensiValEnd);
    sensiExpected = sensiExpected.combinedWith(sensiEnd);
    PointSensitivityBuilder sensiComputed =
        obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }

}
