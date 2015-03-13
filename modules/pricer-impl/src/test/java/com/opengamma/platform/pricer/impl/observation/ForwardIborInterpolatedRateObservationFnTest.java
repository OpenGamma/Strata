/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.platform.finance.observation.IborInterpolatedRateObservation;
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

}
