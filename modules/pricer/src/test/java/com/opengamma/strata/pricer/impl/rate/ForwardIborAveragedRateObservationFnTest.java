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
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateObservation;

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
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

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
      when(mockIbor.rate(FIXING_DATES[i])).thenReturn(FIXING_VALUES[i]);
    }

    double rateExpected = totalWeightedRate / totalWeight;
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    double rateComputed = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov);
    assertEquals(rateComputed, rateExpected, TOLERANCE_RATE);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    assertEquals(obsFn.explainRate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov, builder), rateExpected, TOLERANCE_RATE);

    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.OBSERVATIONS).isPresent(), true);
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().size(), FIXING_DATES.length);
    for (int i = 0; i < 4; i++) {
      ExplainMap childMap = built.get(ExplainKey.OBSERVATIONS).get().get(i);
      assertEquals(childMap.get(ExplainKey.FIXING_DATE), Optional.of(FIXING_DATES[i]));
      assertEquals(childMap.get(ExplainKey.INDEX), Optional.of(GBP_LIBOR_3M));
      assertEquals(childMap.get(ExplainKey.INDEX_VALUE), Optional.of(FIXING_VALUES[i]));
      assertEquals(childMap.get(ExplainKey.WEIGHT), Optional.of(WEIGHTS[i]));
    }
    assertEquals(built.get(ExplainKey.COMBINED_RATE), Optional.of(rateExpected));
  }

  public void test_rateSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeight = 0.0d;
    for (int i = 0; i < FIXING_DATES.length; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeight += WEIGHTS[i];
      when(mockIbor.ratePointSensitivity(FIXING_DATES[i])).thenReturn(SENSITIVITIES[i]);
    }

    PointSensitivities expected = PointSensitivities.of(ImmutableList.of(
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[0], WEIGHTS[0] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[1], WEIGHTS[1] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[2], WEIGHTS[2] / totalWeight),
        IborRateSensitivity.of(GBP_LIBOR_3M, FIXING_DATES[3], WEIGHTS[3] / totalWeight)));
    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov);
    assertEquals(test.build(), expected);
  }

  public void test_rateSensitivity_finiteDifference() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    double eps = 1.0e-7;
    int nDates = FIXING_DATES.length;
    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < nDates; i++) {
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .fixingDate(FIXING_DATES[i])
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      when(mockIbor.ratePointSensitivity(FIXING_DATES[i])).thenReturn(SENSITIVITIES[i]);
    }

    IborAveragedRateObservation ro = IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings);
    ForwardIborAveragedRateObservationFn obsFn = ForwardIborAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov);
    for (int i = 0; i < nDates; ++i) {
      IborIndexRates mockIborUp = mock(IborIndexRates.class);
      SimpleRatesProvider provUp = new SimpleRatesProvider();
      provUp.setIborRates(mockIborUp);
      IborIndexRates mockIborDw = mock(IborIndexRates.class);
      SimpleRatesProvider provDw = new SimpleRatesProvider();
      provDw.setIborRates(mockIborDw);

      for (int j = 0; j < nDates; ++j) {
        if (i == j) {
          when(mockIborUp.rate(FIXING_DATES[j])).thenReturn(FIXING_VALUES[j] + eps);
          when(mockIborDw.rate(FIXING_DATES[j])).thenReturn(FIXING_VALUES[j] - eps);
        } else {
          when(mockIborUp.rate(FIXING_DATES[j])).thenReturn(FIXING_VALUES[j]);
          when(mockIborDw.rate(FIXING_DATES[j])).thenReturn(FIXING_VALUES[j]);
        }
      }
      double rateUp = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, provUp);
      double rateDw = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, provDw);
      double resExpected = 0.5 * (rateUp - rateDw) / eps;
      assertEquals(test.build().getSensitivities().get(i).getSensitivity(), resExpected, eps);
    }
  }

}
