/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
public class ForwardIborRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);
  private static final LocalDate FORWARD_START_DATE = ACCRUAL_START_DATE.minusDays(2);
  private static final LocalDate FORWARD_END_DATE = ACCRUAL_END_DATE.minusDays(2);
  private static final double RATE = 0.0123d;
  private static final IborRateComputation GBP_LIBOR_3M_COMP = IborRateComputation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
  private static final IborRateSensitivity SENSITIVITY = IborRateSensitivity.of(GBP_LIBOR_3M_COMP.getObservation(), 1d);

  @Test
  public void test_rate() {
    SimpleRatesProvider prov = new SimpleRatesProvider();
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(FIXING_DATE, RATE);
    IborIndexRates mockIbor = new TestingIborIndexRates(
        GBP_LIBOR_3M, FIXING_DATE, LocalDateDoubleTimeSeries.empty(), timeSeries);
    prov.setIborRates(mockIbor);

    ForwardIborRateComputationFn obsFn = ForwardIborRateComputationFn.DEFAULT;
    assertThat(obsFn.rate(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov)).isEqualTo(RATE);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    assertThat(obsFn.explainRate(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov, builder)).isEqualTo(RATE);

    ExplainMap built = builder.build();
    assertThat(built.get(ExplainKey.OBSERVATIONS)).isPresent();
    assertThat(built.get(ExplainKey.OBSERVATIONS).get()).hasSize(1);
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FIXING_DATE)).isEqualTo(Optional.of(FIXING_DATE));
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.INDEX)).isEqualTo(Optional.of(GBP_LIBOR_3M));
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.INDEX_VALUE)).isEqualTo(Optional.of(RATE));
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FROM_FIXING_SERIES)).isEqualTo(Optional.of(true));
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FORWARD_RATE_START_DATE)).isEqualTo(Optional.of(FORWARD_START_DATE));
    assertThat(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FORWARD_RATE_END_DATE)).isEqualTo(Optional.of(FORWARD_END_DATE));
    assertThat(built.get(ExplainKey.COMBINED_RATE)).isEqualTo(Optional.of(RATE));
  }

  @Test
  public void test_rateSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    when(mockIbor.ratePointSensitivity(GBP_LIBOR_3M_COMP.getObservation())).thenReturn(SENSITIVITY);

    ForwardIborRateComputationFn obsFn = ForwardIborRateComputationFn.DEFAULT;
    assertThat(obsFn.rateSensitivity(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov)).isEqualTo(SENSITIVITY);
  }

}
