/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

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

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link IborIndexObservation}.
 */
@Test
public class IborIndexObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    IborIndexObservation test = IborIndexObservation.of(USD_LIBOR_3M, date(2016, 2, 18), REF_DATA);
    double yearFraction = USD_LIBOR_3M.getDayCount().yearFraction(date(2016, 2, 22), date(2016, 5, 23));
    IborIndexObservation expected = new IborIndexObservation(
        USD_LIBOR_3M, date(2016, 2, 18), date(2016, 2, 22), date(2016, 5, 23), yearFraction);
    assertEquals(test, expected);
    assertEquals(test.getCurrency(), USD);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexObservation test = IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    coverImmutableBean(test);
    IborIndexObservation test2 = IborIndexObservation.of(GBP_LIBOR_1M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborIndexObservation test = IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
    assertSerialization(test);
  }

}
