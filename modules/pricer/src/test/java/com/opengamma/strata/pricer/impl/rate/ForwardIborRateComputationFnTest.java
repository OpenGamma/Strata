/*
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
import java.util.Optional;

import org.testng.annotations.Test;

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
@Test
public class ForwardIborRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final LocalDate ACCRUAL_START_DATE = date(2014, 7, 2);
  private static final LocalDate ACCRUAL_END_DATE = date(2014, 10, 2);
  private static final double RATE = 0.0123d;
  private static final IborRateComputation GBP_LIBOR_3M_COMP = IborRateComputation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
  private static final IborRateSensitivity SENSITIVITY = IborRateSensitivity.of(GBP_LIBOR_3M_COMP.getObservation(), 1d);

  public void test_rate() {
    SimpleRatesProvider prov = new SimpleRatesProvider();
    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(FIXING_DATE, RATE);
    IborIndexRates mockIbor = new TestingIborIndexRates(
        GBP_LIBOR_3M, FIXING_DATE, LocalDateDoubleTimeSeries.empty(), timeSeries);
    prov.setIborRates(mockIbor);

    ForwardIborRateComputationFn obsFn = ForwardIborRateComputationFn.DEFAULT;
    assertEquals(obsFn.rate(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov), RATE);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    assertEquals(obsFn.explainRate(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov, builder), RATE);

    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.OBSERVATIONS).isPresent(), true);
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().size(), 1);
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FIXING_DATE), Optional.of(FIXING_DATE));
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.INDEX), Optional.of(GBP_LIBOR_3M));
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.INDEX_VALUE), Optional.of(RATE));
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().get(0).get(ExplainKey.FROM_FIXING_SERIES), Optional.of(true));
    assertEquals(built.get(ExplainKey.COMBINED_RATE), Optional.of(RATE));
  }

  public void test_rateSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    when(mockIbor.ratePointSensitivity(GBP_LIBOR_3M_COMP.getObservation())).thenReturn(SENSITIVITY);

    ForwardIborRateComputationFn obsFn = ForwardIborRateComputationFn.DEFAULT;
    assertEquals(obsFn.rateSensitivity(GBP_LIBOR_3M_COMP, ACCRUAL_START_DATE, ACCRUAL_END_DATE, prov), SENSITIVITY);
  }

}
