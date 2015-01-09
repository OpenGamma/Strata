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

import com.opengamma.platform.finance.observation.IborInterpolatedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
* Test {@link ForwardIborInterpolatedRateObservationFn}.
*/
@Test
public class ForwardIborInterpolatedRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 11, 2);
  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_IborInterpolatedRateObservation() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    double rate3M = 0.0123d;
    double rate6M = 0.0234d;
    when(mockEnv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE))
        .thenReturn(rate3M);
    when(mockEnv.iborIndexRate(GBP_LIBOR_6M, FIXING_DATE))
        .thenReturn(rate6M);
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
    double rateExpected = (weight3M * rate3M + weight6M * rate6M);
    double rateComputed = obs.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }
  
}
