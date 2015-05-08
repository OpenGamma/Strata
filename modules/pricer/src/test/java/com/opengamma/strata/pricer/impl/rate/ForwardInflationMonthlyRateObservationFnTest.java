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

import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Test.
 */
@Test
public class ForwardInflationMonthlyRateObservationFnTest {
  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REFERENCE_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REFERENCE_END_MONTH = YearMonth.of(2015, 10);
  private static final double RATE_START = 317.0;
  private static final double RATE_END = 344.0;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;
  //TODO add test curve parameter sensitivity with RatesFiniteDifferenceSensitivityCalculator

  public void test_rate() {
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START);
    when(mockProv.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END);
    InflationMonthlyRateObservation ro =
        InflationMonthlyRateObservation.of(GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;
    assertEquals(obsFn.rate(
        ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv), RATE_END / RATE_START - 1.0, EPS);
  }

  public void test_rateSensitivity() {
    InflationMonthlyRateObservation ro = InflationMonthlyRateObservation.of(
        GB_RPIX, REFERENCE_START_MONTH, REFERENCE_END_MONTH);
    ForwardInflationMonthlyRateObservationFn obsFn = ForwardInflationMonthlyRateObservationFn.DEFAULT;
    RatesProvider mockProvStartUp = mock(RatesProvider.class);
    when(mockProvStartUp.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START + EPS_FD);
    when(mockProvStartUp.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END);
    double rateStartUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvStartUp);
    RatesProvider mockProvStartDw = mock(RatesProvider.class);
    when(mockProvStartDw.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START - EPS_FD);
    when(mockProvStartDw.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END);
    double rateStartDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvStartDw);
    double sensiValStart = 0.5 * (rateStartUp - rateStartDw) / EPS_FD;
    PointSensitivityBuilder sensiExpected = InflationRateSensitivity.of(GB_RPIX, REFERENCE_START_MONTH, sensiValStart);
    RatesProvider mockProvEndUp = mock(RatesProvider.class);
    when(mockProvEndUp.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START);
    when(mockProvEndUp.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END + EPS_FD);
    double rateEndUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvEndUp);
    RatesProvider mockProvEndDw = mock(RatesProvider.class);
    when(mockProvEndDw.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START);
    when(mockProvEndDw.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END - EPS_FD);
    double rateEndDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvEndDw);
    double sensiValEnd = 0.5 * (rateEndUp - rateEndDw) / EPS_FD;
    PointSensitivityBuilder sensiEnd = InflationRateSensitivity.of(GB_RPIX, REFERENCE_END_MONTH, sensiValEnd);
    sensiExpected = sensiExpected.combinedWith(sensiEnd);

    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.inflationIndexRate(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(RATE_START);
    when(mockProv.inflationIndexRate(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(RATE_END);
    when(mockProv.inflationIndexRateSensitivity(GB_RPIX, REFERENCE_START_MONTH)).thenReturn(
        InflationRateSensitivity.of(GB_RPIX, REFERENCE_START_MONTH, 1.0d));
    when(mockProv.inflationIndexRateSensitivity(GB_RPIX, REFERENCE_END_MONTH)).thenReturn(
        InflationRateSensitivity.of(GB_RPIX, REFERENCE_END_MONTH, 1.0d));
    PointSensitivityBuilder sensiComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
        mockProv);
    
    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }
}
