/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.finance.rate.IborAveragedFixing;
import com.opengamma.strata.finance.rate.IborAveragedRateObservation;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
* Test.
*/
@Test
public class ForwardIborAveragedRateObservationFnTest {

  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
      date(2014, 6, 30), date(2014, 7, 7), date(2014, 7, 14), date(2014, 7, 21)};
  private static final double[] FIXING_VALUES = {0.0123d, 0.0234d, 0.0345d, 0.0456d};
  private static final double[] WEIGHTS = {0.10d, 0.20d, 0.30d, 0.40d};
  private static final IborRateSensitivity[] SENSITIVITIES = {
      IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[0], 1d),
      IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[1], 1d),
      IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[2], 1d),
      IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[3], 1d),
  };

  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 11, 2);
  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_rate() {
    RatesProvider mockProv = mock(RatesProvider.class);
    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeightedRate = 0.0d;
    double totalWeight = 0.0d;
    for (int i = 0; i < FIXING_DATES.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeightedRate += FIXING_VALUES[i] * WEIGHTS[i];
      totalWeight += WEIGHTS[i];
      when(mockProv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[i]))
          .thenReturn(FIXING_VALUES[i]);
    }
    double rateExpected = totalWeightedRate / totalWeight;
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    double rateComputed = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProv);
    assertEquals(rateComputed, rateExpected, TOLERANCE_RATE);
  }

  public void test_rateSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeight = 0.0d;
    for (int i = 0; i < FIXING_DATES.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeight += WEIGHTS[i];
      when(mockProv.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATES[i]))
          .thenReturn(SENSITIVITIES[i]);
    }
    PointSensitivities expected = PointSensitivities.of(ImmutableList.of(
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[0], WEIGHTS[0] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[1], WEIGHTS[1] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[2], WEIGHTS[2] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[3], WEIGHTS[3] / totalWeight)));
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProv);
    assertEquals(test.build(), expected);
  }

  public void test_rateSensitivity_finiteDifference() {
    double eps = 1.0e-7;
    int nDates = FIXING_DATES.length;
    RatesProvider mockProvBase = mock(RatesProvider.class);

    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < nDates; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      when(mockProvBase.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATES[i])).thenReturn(SENSITIVITIES[i]);
    }
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProvBase);
    for (int i = 0; i < nDates; ++i) {
      RatesProvider mockProvUp = mock(RatesProvider.class);
      RatesProvider mockProvDw = mock(RatesProvider.class);
      for (int j = 0; j < nDates; ++j) {
        if (i == j) {
          when(mockProvUp.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[j])).thenReturn(FIXING_VALUES[j] + eps);
          when(mockProvDw.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[j])).thenReturn(FIXING_VALUES[j] - eps);
        } else {
          when(mockProvUp.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[j])).thenReturn(FIXING_VALUES[j]);
          when(mockProvDw.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[j])).thenReturn(FIXING_VALUES[j]);
        }
      }
      double rateUp = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProvUp);
      double rateDw = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, mockProvDw);
      double resExpected = 0.5 * (rateUp - rateDw) / eps;
      assertEquals(test.build().getSensitivities().get(i).getSensitivity(), resExpected, eps);
    }

  }

}
