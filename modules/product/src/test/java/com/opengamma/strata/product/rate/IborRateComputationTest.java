/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
public class IborRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    IborRateComputation test = IborRateComputation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    IborIndexObservation obs = IborIndexObservation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    IborRateComputation expected = IborRateComputation.of(obs);
    assertThat(test).isEqualTo(expected);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getIndex()).isEqualTo(obs.getIndex());
    assertThat(test.getFixingDate()).isEqualTo(obs.getFixingDate());
    assertThat(test.getEffectiveDate()).isEqualTo(obs.getEffectiveDate());
    assertThat(test.getMaturityDate()).isEqualTo(obs.getMaturityDate());
    assertThat(test.getYearFraction()).isEqualTo(obs.getYearFraction());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GBP_LIBOR_3M);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    coverImmutableBean(test);
    IborRateComputation test2 = IborRateComputation.of(GBP_LIBOR_1M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    assertSerialization(test);
  }

}
