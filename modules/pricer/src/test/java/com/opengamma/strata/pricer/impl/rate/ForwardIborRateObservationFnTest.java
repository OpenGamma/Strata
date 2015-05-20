/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATE)).thenReturn(RATE);

    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    ForwardIborRateObservationFn obsFn = ForwardIborRateObservationFn.DEFAULT;
    assertEquals(obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProv), RATE);
  }

  public void test_rateSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    IborIndexRates mockRates = mock(IborIndexRates.class);
    when(mockProv.iborIndexRates(GBP_LIBOR_3M)).thenReturn(mockRates);
    when(mockRates.pointSensitivity(FIXING_DATE)).thenReturn(SENSITIVITY);

    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    ForwardIborRateObservationFn obsFn = ForwardIborRateObservationFn.DEFAULT;
    assertEquals(obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProv), SENSITIVITY);
  }

}
