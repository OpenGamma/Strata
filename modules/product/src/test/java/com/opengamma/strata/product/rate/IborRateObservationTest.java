/**
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

import java.time.LocalDate;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test.
 */
@Test
public class IborRateObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    IborRateObservation test = IborRateObservation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    IborRateObservation expected = IborRateObservation.builder()
        .index(USD_LIBOR_3M)
        .fixingDate(date(2016, 2, 18))
        .effectiveDate(date(2016, 2, 22))
        .maturityDate(date(2016, 5, 23))
        .yearFraction(USD_LIBOR_3M.getDayCount().yearFraction(date(2016, 2, 22), date(2016, 5, 23)))
        .build();
    assertEquals(test, expected);
    assertEquals(test.getCurrency(), USD);
  }

  public void test_bind() {
    Function<LocalDate, IborRateObservation> test = IborRateObservation.bind(USD_LIBOR_3M, REF_DATA);
    IborRateObservation expected = IborRateObservation.builder()
        .index(USD_LIBOR_3M)
        .fixingDate(date(2016, 2, 18))
        .effectiveDate(date(2016, 2, 22))
        .maturityDate(date(2016, 5, 23))
        .yearFraction(USD_LIBOR_3M.getDayCount().yearFraction(date(2016, 2, 22), date(2016, 5, 23)))
        .build();
    assertEquals(test.apply(date(2016, 2, 18)), expected);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborRateObservation test = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborRateObservation test = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    coverImmutableBean(test);
    IborRateObservation test2 = IborRateObservation.of(GBP_LIBOR_1M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborRateObservation test = IborRateObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    assertSerialization(test);
  }

}
