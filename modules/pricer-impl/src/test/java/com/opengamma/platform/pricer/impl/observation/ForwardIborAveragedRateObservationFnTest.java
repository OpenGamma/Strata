/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.platform.finance.observation.IborAveragedFixing;
import com.opengamma.platform.finance.observation.IborAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.platform.pricer.sensitivity.PointSensitivities;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;

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

  public void rate() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
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
      when(mockEnv.iborIndexRate(GBP_LIBOR_3M, FIXING_DATES[i]))
          .thenReturn(FIXING_VALUES[i]);
    }
    double rateExpected = totalWeightedRate / totalWeight;
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    double rateComputed = obsFn.rate(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    assertEquals(rateComputed, rateExpected, TOLERANCE_RATE);
  }

  public void rateSensitivity() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeight = 0.0d;
    for (int i = 0; i < FIXING_DATES.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeight += WEIGHTS[i];
      when(mockEnv.iborIndexRateSensitivity(GBP_LIBOR_3M, FIXING_DATES[i]))
          .thenReturn(SENSITIVITIES[i]);
    }
    PointSensitivities expected = PointSensitivities.of(ImmutableList.of(
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[0], WEIGHTS[0] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[1], WEIGHTS[1] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[2], WEIGHTS[2] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[3], WEIGHTS[3] / totalWeight)));
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(mockEnv, ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE);
    assertEquals(test.build(), expected);
  }

}
