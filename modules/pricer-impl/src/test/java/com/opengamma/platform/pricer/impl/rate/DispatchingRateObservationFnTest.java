/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.rate.FixedRateObservation;
import com.opengamma.platform.finance.rate.IborAveragedFixing;
import com.opengamma.platform.finance.rate.IborAveragedRateObservation;
import com.opengamma.platform.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.platform.finance.rate.IborRateObservation;
import com.opengamma.platform.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.platform.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.platform.finance.rate.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.rate.RateObservationFn;

/**
 * Test.
 */
@Test
public class DispatchingRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();
  private static final RateObservationFn<IborRateObservation> MOCK_IBOR_EMPTY =
      mock(RateObservationFn.class);
  private static final RateObservationFn<IborInterpolatedRateObservation> MOCK_IBOR_INT_EMPTY =
      mock(RateObservationFn.class);
  private static final RateObservationFn<IborAveragedRateObservation> MOCK_IBOR_AVE_EMPTY =
      mock(RateObservationFn.class);
  private static final RateObservationFn<OvernightCompoundedRateObservation> MOCK_ON_CPD_EMPTY =
      mock(RateObservationFn.class);
  private static final RateObservationFn<OvernightAveragedRateObservation> MOCK_ON_AVE_EMPTY =
      mock(RateObservationFn.class);

  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_rate_FixedRateObservation() {
    FixedRateObservation ro = FixedRateObservation.of(0.0123d);
    DispatchingRateObservationFn test = DispatchingRateObservationFn.DEFAULT;
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), 0.0123d, 0d);
  }

  public void test_rate_IborRateObservation() {
    RateObservationFn<IborRateObservation> mockIbor = mock(RateObservationFn.class);
    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    when(mockIbor.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(0.0123d);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        mockIbor, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), 0.0123d, 0d);
  }

  public void test_rate_IborInterpolatedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<IborInterpolatedRateObservation> mockIborInt = mock(RateObservationFn.class);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    when(mockIborInt.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, mockIborInt, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), mockRate, 0d);
  }

  public void test_rate_IborAverageRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<IborAveragedRateObservation> mockIborAve = mock(RateObservationFn.class);
    LocalDate[] fixingDates = new LocalDate[] {
        date(2014, 6, 30), date(2014, 7, 7), date(2014, 7, 14), date(2014, 7, 21)};
    double[] weights = {0.10d, 0.20d, 0.30d, 0.40d};
    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < fixingDates.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder().fixingDate(fixingDates[i])
          .weight(weights[i]).build();
      fixings.add(fixing);
    }
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    when(mockIborAve.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, mockIborAve, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), mockRate, 0d);
  }

  public void test_rate_OvernightCompoundedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<OvernightCompoundedRateObservation> mockOnCpd = mock(RateObservationFn.class);
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    when(mockOnCpd.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, mockOnCpd, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_OvernightAveragedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<OvernightAveragedRateObservation> mockOnAve = mock(RateObservationFn.class);
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    when(mockOnAve.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, mockOnAve);
    assertEquals(test.rate(MOCK_ENV, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_unknownType() {
    RateObservation mockObservation = mock(RateObservation.class);
    DispatchingRateObservationFn test = DispatchingRateObservationFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.rate(MOCK_ENV, mockObservation, ACCRUAL_START_DATE, ACCRUAL_END_DATE));
  }

}
