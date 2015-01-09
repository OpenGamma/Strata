/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.observation.FixedRateObservation;
import com.opengamma.platform.finance.observation.IborInterpolatedRateObservation;
import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
 * Test.
 */
@Test
public class DispatchingRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);

  public void test_rate_FixedRateObservation() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    FixedRateObservation ro = FixedRateObservation.of(0.0123d);
    DispatchingRateObservationFn test = DispatchingRateObservationFn.DEFAULT;
    assertEquals(test.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), 0.0123d, 0d);
  }

  public void test_rate_IborRateObservation() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    RateObservationFn<IborRateObservation> mockIbor = mock(RateObservationFn.class);
    RateObservationFn<IborInterpolatedRateObservation> mockIborInt = mock(RateObservationFn.class);
    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    when(mockIbor.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(0.0123d);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(mockIbor, mockIborInt);
    assertEquals(test.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), 0.0123d, 0d);
  }

  public void test_rate_IborInterpolatedRateObservation() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    double mockRate = 0.0123d;
    RateObservationFn<IborRateObservation> mockIbor = mock(RateObservationFn.class);
    RateObservationFn<IborInterpolatedRateObservation> mockIborInt = mock(RateObservationFn.class);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    when(mockIborInt.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(mockIbor, mockIborInt);
    assertEquals(test.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), mockRate, 0d);
  }

  public void test_rate_unknownType() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    RateObservation mockObservation = mock(RateObservation.class);
    DispatchingRateObservationFn test = DispatchingRateObservationFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.rate(mockEnv, mockObservation, ACCRUAL_START_DATE, ACCRUAL_END_DATE));
  }

}
