/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.IborRateSensitivity;

/**
 * Test.
 */
@Test
public class ForwardIborRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);
  private static final double RATE = 0.0123d;
  private static final IborRateSensitivity SENSITIVITY = IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATE, 1d);

  public void test_rate() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE))
        .thenReturn(RATE);
    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    ForwardIborRateObservationFn obsFn = ForwardIborRateObservationFn.DEFAULT;
    assertEquals(obsFn.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), RATE);
  }

  public void test_rateSensitivity() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATE))
        .thenReturn(SENSITIVITY);
    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    ForwardIborRateObservationFn obsFn = ForwardIborRateObservationFn.DEFAULT;
    assertEquals(obsFn.rateSensitivity(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), SENSITIVITY);
  }

}
