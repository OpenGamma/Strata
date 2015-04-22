/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.ignoreThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborAveragedFixing;
import com.opengamma.strata.finance.rate.IborAveragedRateObservation;
import com.opengamma.strata.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;

/**
 * Test.
 */
@Test
public class DispatchingRateObservationFnTest {

  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);

  private static final RatesProvider MOCK_PROV = new MockRatesProvider();
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
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), 0.0123d, 0d);
  }

  public void test_rate_IborRateObservation() {
    RateObservationFn<IborRateObservation> mockIbor = mock(RateObservationFn.class);
    IborRateObservation ro = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    when(mockIbor.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(0.0123d);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        mockIbor, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), 0.0123d, 0d);
  }

  public void test_rate_IborInterpolatedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<IborInterpolatedRateObservation> mockIborInt = mock(RateObservationFn.class);
    IborInterpolatedRateObservation ro = IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    when(mockIborInt.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, mockIborInt, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, 0d);
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
    when(mockIborAve.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, mockIborAve, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, 0d);
  }

  public void test_rate_OvernightCompoundedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<OvernightCompoundedRateObservation> mockOnCpd = mock(RateObservationFn.class);
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    when(mockOnCpd.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, mockOnCpd, MOCK_ON_AVE_EMPTY);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_OvernightAveragedRateObservation() {
    double mockRate = 0.0123d;
    RateObservationFn<OvernightAveragedRateObservation> mockOnAve = mock(RateObservationFn.class);
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    when(mockOnAve.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV))
        .thenReturn(mockRate);
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, mockOnAve);
    assertEquals(test.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV), mockRate, TOLERANCE_RATE);
  }

  public void test_rate_unknownType() {
    RateObservation mockObservation = mock(RateObservation.class);
    DispatchingRateObservationFn test = DispatchingRateObservationFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.rate(mockObservation, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DispatchingRateObservationFn test = new DispatchingRateObservationFn(
        MOCK_IBOR_EMPTY, MOCK_IBOR_INT_EMPTY, MOCK_IBOR_AVE_EMPTY, MOCK_ON_CPD_EMPTY, MOCK_ON_AVE_EMPTY);
    FixedRateObservation fixed = FixedRateObservation.of(0.0123d);
    IborRateObservation ibor = IborRateObservation.of(GBP_LIBOR_3M, FIXING_DATE);
    IborInterpolatedRateObservation iborInt =
        IborInterpolatedRateObservation.of(GBP_LIBOR_3M, GBP_LIBOR_6M, FIXING_DATE);
    IborAveragedRateObservation iborAvg =
        IborAveragedRateObservation.of(GBP_LIBOR_3M, ImmutableList.of(IborAveragedFixing.of(FIXING_DATE)));
    OvernightCompoundedRateObservation onCpd =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    OvernightAveragedRateObservation onAvg =
        OvernightAveragedRateObservation.of(USD_FED_FUND, ACCRUAL_START_DATE, ACCRUAL_END_DATE, 0);
    RateObservation mock = mock(RateObservation.class);
    ignoreThrows(() -> test.rateSensitivity(fixed, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(ibor, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(iborInt, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(iborAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(onCpd, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(onAvg, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
    ignoreThrows(() -> test.rateSensitivity(mock, ACCRUAL_START_DATE, ACCRUAL_END_DATE, MOCK_PROV));
  }

}
