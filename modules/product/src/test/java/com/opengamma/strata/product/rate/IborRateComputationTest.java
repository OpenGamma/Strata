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
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
@Test
public class IborRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    IborRateComputation test = IborRateComputation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    IborIndexObservation obs = IborIndexObservation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    IborRateComputation expected = IborRateComputation.of(obs);
    assertEquals(test, expected);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getIndex(), obs.getIndex());
    assertEquals(test.getFixingDate(), obs.getFixingDate());
    assertEquals(test.getEffectiveDate(), obs.getEffectiveDate());
    assertEquals(test.getMaturityDate(), obs.getMaturityDate());
    assertEquals(test.getYearFraction(), obs.getYearFraction());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    coverImmutableBean(test);
    IborRateComputation test2 = IborRateComputation.of(GBP_LIBOR_1M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborRateComputation test = IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    assertSerialization(test);
  }

}
