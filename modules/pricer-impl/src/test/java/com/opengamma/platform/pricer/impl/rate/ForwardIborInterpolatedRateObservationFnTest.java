/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.platform.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.platform.pricer.sensitivity.PointSensitivities;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;

/**
* Test.
*/
@Test
public class ForwardIborInterpolatedRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 11, 2);
  private static final double RATE3 = 0.0123d;
  private static final double RATE6 = 0.0234d;
  private static final IborRateSensitivity SENSITIVITY3 = IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATE, 1d);
  private static final IborRateSensitivity SENSITIVITY6 = IborRateSensitivity.of(GBP_LIBOR_6M, FIXING_DATE, 1d);
  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_rate() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE))
        .thenReturn(RATE3);
    when(mockEnv.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE))
        .thenReturn(RATE6);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    ForwardIborInterpolatedRateObservationFn obs = ForwardIborInterpolatedRateObservationFn.DEFAULT;
    LocalDate fixingStartDate = ro.getShortIndex().calculateEffectiveFromFixing(ro.getFixingDate());
    LocalDate fixingEndDate3M = ro.getShortIndex().calculateMaturityFromEffective(fixingStartDate);
    LocalDate fixingEndDate6M = ro.getLongIndex().calculateMaturityFromEffective(fixingStartDate);
    double days3M = fixingEndDate3M.toEpochDay() - ro.getFixingDate().toEpochDay(); //nb days in 3M fixing period
    double days6M = fixingEndDate6M.toEpochDay() - ro.getFixingDate().toEpochDay(); //nb days in 6M fixing period
    double daysCpn = ACCRUAL_END_DATE.toEpochDay() - ro.getFixingDate().toEpochDay();
    double weight3M = (days6M - daysCpn) / (days6M - days3M);
    double weight6M = (daysCpn - days3M) / (days6M - days3M);
    double rateExpected = (weight3M * RATE3 + weight6M * RATE6);
    double rateComputed = obs.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    assertEquals(rateComputed, rateExpected, TOLERANCE_RATE);
  }

  public void test_rateSensitivity() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATE))
        .thenReturn(SENSITIVITY3);
    when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_6M, FIXING_DATE))
        .thenReturn(SENSITIVITY6);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    ForwardIborInterpolatedRateObservationFn obsFn = ForwardIborInterpolatedRateObservationFn.DEFAULT;
    LocalDate fixingStartDate = ro.getShortIndex().calculateEffectiveFromFixing(ro.getFixingDate());
    LocalDate fixingEndDate3M = ro.getShortIndex().calculateMaturityFromEffective(fixingStartDate);
    LocalDate fixingEndDate6M = ro.getLongIndex().calculateMaturityFromEffective(fixingStartDate);
    double days3M = fixingEndDate3M.toEpochDay() - ro.getFixingDate().toEpochDay(); //nb days in 3M fixing period
    double days6M = fixingEndDate6M.toEpochDay() - ro.getFixingDate().toEpochDay(); //nb days in 6M fixing period
    double daysCpn = ACCRUAL_END_DATE.toEpochDay() - ro.getFixingDate().toEpochDay();
    double weight3M = (days6M - daysCpn) / (days6M - days3M);
    double weight6M = (daysCpn - days3M) / (days6M - days3M);
    IborRateSensitivity sens3 = IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATE, weight3M);
    IborRateSensitivity sens6 = IborRateSensitivity.of(GBP_LIBOR_6M, FIXING_DATE, weight6M);
    PointSensitivities expected = PointSensitivities.of(ImmutableList.of(sens3, sens6));
    PointSensitivityBuilder test = obsFn.rateSensitivity(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    assertEquals(test.build(), expected);
  }

  public void test_rateSensitivity_finiteDifference() {
    double eps = 1.0e-7;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(SENSITIVITY3);
    when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_6M, FIXING_DATE)).thenReturn(SENSITIVITY6);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    ForwardIborInterpolatedRateObservationFn obs = ForwardIborInterpolatedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obs.rateSensitivity(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);

    PricingEnvironment mockEnvUp3M = mock(PricingEnvironment.class);
    when(mockEnvUp3M.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(RATE3 + eps);
    when(mockEnvUp3M.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE)).thenReturn(RATE6);
    double rateUp3M = obs.rate(mockEnvUp3M, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    PricingEnvironment mockEnvDw3M = mock(PricingEnvironment.class);
    when(mockEnvDw3M.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(RATE3 - eps);
    when(mockEnvDw3M.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE)).thenReturn(RATE6);
    double rateDw3M = obs.rate(mockEnvDw3M, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    double senseExpected3M = 0.5 * (rateUp3M - rateDw3M) / eps;

    PricingEnvironment mockEnvUp6M = mock(PricingEnvironment.class);
    when(mockEnvUp6M.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(RATE3);
    when(mockEnvUp6M.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE)).thenReturn(RATE6 + eps);
    double rateUp6M = obs.rate(mockEnvUp6M, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    PricingEnvironment mockEnvDw6M = mock(PricingEnvironment.class);
    when(mockEnvDw6M.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(RATE3);
    when(mockEnvDw6M.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE)).thenReturn(RATE6 - eps);
    double rateDw6M = obs.rate(mockEnvDw6M, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    double senseExpected6M = 0.5 * (rateUp6M - rateDw6M) / eps;

    assertEquals(test.build().getSensitivities().get(0).getSensitivity(), senseExpected3M, eps);
    assertEquals(test.build().getSensitivities().get(1).getSensitivity(), senseExpected6M, eps);
  }

}
