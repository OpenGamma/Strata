/*
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;

/**
* Test.
*/
@Test
public class ForwardIborAveragedRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndexObservation[] OBSERVATIONS = new IborIndexObservation[] {
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA),
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 7, 7), REF_DATA),
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 7, 14), REF_DATA),
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 7, 21), REF_DATA)};
  private static final double[] FIXING_VALUES = {0.0123d, 0.0234d, 0.0345d, 0.0456d};
  private static final double[] WEIGHTS = {0.10d, 0.20d, 0.30d, 0.40d};
  private static final IborRateSensitivity[] SENSITIVITIES = {
      IborRateSensitivity.of(OBSERVATIONS[0], 1d),
      IborRateSensitivity.of(OBSERVATIONS[1], 1d),
      IborRateSensitivity.of(OBSERVATIONS[2], 1d),
      IborRateSensitivity.of(OBSERVATIONS[3], 1d),
  };

  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 11, 2);
  private static final double TOLERANCE_RATE = 1.0E-10;

  public void test_rate() {
    LocalDate fixingDate = OBSERVATIONS[0].getFixingDate();
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(fixingDate, FIXING_VALUES[0]);
    LocalDateDoubleTimeSeries rates = LocalDateDoubleTimeSeries.builder()
        .put(OBSERVATIONS[1].getFixingDate(), FIXING_VALUES[1])
        .put(OBSERVATIONS[2].getFixingDate(), FIXING_VALUES[2])
        .put(OBSERVATIONS[3].getFixingDate(), FIXING_VALUES[3])
        .build();
    IborIndexRates mockIbor = new TestingIborIndexRates(
        GBP_LIBOR_3M, fixingDate, rates, timeSeries);
    SimpleRatesProvider prov = new SimpleRatesProvider(fixingDate);
    prov.setIborRates(mockIbor);

    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeightedRate = 0.0d;
    double totalWeight = 0.0d;
    for (int i = 0; i < OBSERVATIONS.length; i++) {
      IborIndexObservation obs = OBSERVATIONS[i];
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .observation(obs)
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeightedRate += FIXING_VALUES[i] * WEIGHTS[i];
      totalWeight += WEIGHTS[i];
    }

    double rateExpected = totalWeightedRate / totalWeight;
    IborAveragedRateComputation ro = IborAveragedRateComputation.of(fixings);
    ForwardIborAveragedRateComputationFn obsFn = ForwardIborAveragedRateComputationFn.DEFAULT;
    double rateComputed = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov);
    assertEquals(rateComputed, rateExpected, TOLERANCE_RATE);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    assertEquals(obsFn.explainRate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov, builder), rateExpected, TOLERANCE_RATE);

    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.OBSERVATIONS).isPresent(), true);
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().size(), OBSERVATIONS.length);
    for (int i = 0; i < 4; i++) {
      ExplainMap childMap = built.get(ExplainKey.OBSERVATIONS).get().get(i);
      assertEquals(childMap.get(ExplainKey.FIXING_DATE), Optional.of(OBSERVATIONS[i].getFixingDate()));
      assertEquals(childMap.get(ExplainKey.INDEX), Optional.of(GBP_LIBOR_3M));
      assertEquals(childMap.get(ExplainKey.INDEX_VALUE), Optional.of(FIXING_VALUES[i]));
      assertEquals(childMap.get(ExplainKey.WEIGHT), Optional.of(WEIGHTS[i]));
      assertEquals(childMap.get(ExplainKey.FROM_FIXING_SERIES), i == 0 ? Optional.of(true) : Optional.empty());
    }
    assertEquals(built.get(ExplainKey.COMBINED_RATE), Optional.of(rateExpected));
  }

  public void test_rateSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    List<IborAveragedFixing> fixings = new ArrayList<>();
    double totalWeight = 0.0d;
    for (int i = 0; i < OBSERVATIONS.length; i++) {
      IborIndexObservation obs = OBSERVATIONS[i];
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .observation(obs)
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      totalWeight += WEIGHTS[i];
      when(mockIbor.ratePointSensitivity(obs)).thenReturn(SENSITIVITIES[i]);
    }

    PointSensitivities expected = PointSensitivities.of(ImmutableList.of(
        IborRateSensitivity.of(OBSERVATIONS[0], WEIGHTS[0] / totalWeight),
        IborRateSensitivity.of(OBSERVATIONS[1], WEIGHTS[1] / totalWeight),
        IborRateSensitivity.of(OBSERVATIONS[2], WEIGHTS[2] / totalWeight),
        IborRateSensitivity.of(OBSERVATIONS[3], WEIGHTS[3] / totalWeight)));
    IborAveragedRateComputation ro = IborAveragedRateComputation.of(fixings);
    ForwardIborAveragedRateComputationFn obsFn = ForwardIborAveragedRateComputationFn.DEFAULT;
    PointSensitivityBuilder test = obsFn.rateSensitivity(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov);
    assertEquals(test.build(), expected);
  }

  public void test_rateSensitivity_finiteDifference() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    double eps = 1.0e-7;
    int nDates = OBSERVATIONS.length;
    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < nDates; i++) {
      IborIndexObservation obs = OBSERVATIONS[i];
      IborAveragedFixing fixing = IborAveragedFixing.builder()
          .observation(obs)
          .weight(WEIGHTS[i])
          .build();
      fixings.add(fixing);
      when(mockIbor.ratePointSensitivity(obs)).thenReturn(SENSITIVITIES[i]);
    }

    IborAveragedRateComputation ro = IborAveragedRateComputation.of(fixings);
    ForwardIborAveragedRateComputationFn obsFn = ForwardIborAveragedRateComputationFn.DEFAULT;
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
          when(mockIborUp.rate(OBSERVATIONS[j])).thenReturn(FIXING_VALUES[j] + eps);
          when(mockIborDw.rate(OBSERVATIONS[j])).thenReturn(FIXING_VALUES[j] - eps);
        } else {
          when(mockIborUp.rate(OBSERVATIONS[j])).thenReturn(FIXING_VALUES[j]);
          when(mockIborDw.rate(OBSERVATIONS[j])).thenReturn(FIXING_VALUES[j]);
        }
      }
      double rateUp = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, provUp);
      double rateDw = obsFn.rate(ro, ACCRUAL_START_DATE, ACCRUAL_END_DATE, provDw);
      double resExpected = 0.5 * (rateUp - rateDw) / eps;
      assertEquals(test.build().getSensitivities().get(i).getSensitivity(), resExpected, eps);
    }
  }

}
