/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IborRateSensitivityTest {

  public void test_of() {
    IborRateSensitivity test = IborRateSensitivity.of(GBP_LIBOR_3M, GBP, date(2015, 8, 27), 32d);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP_LIBOR_3M);
  }

  public void test_builder() {
    IborRateSensitivity test = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP_LIBOR_3M);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    IborRateSensitivity base = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity test = base.withSensitivity(20d);
    assertEquals(base.getIndex(), GBP_LIBOR_3M);
    assertEquals(base.getCurrency(), GBP);
    assertEquals(base.getDate(), date(2015, 8, 27));
    assertEquals(base.getSensitivity(), 32d);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 20d);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    IborRateSensitivity a1 = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity a2 = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity b = IborRateSensitivity.builder()
        .index(USD_LIBOR_3M)
        .currency(USD)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity c = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(USD)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity d = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 9, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity e = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 9, 27))
        .sensitivity(32d)
        .build();
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b), -1);
    assertEquals(b.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(c), -1);
    assertEquals(c.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(d), -1);
    assertEquals(d.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(e), 1);
    assertEquals(e.compareExcludingSensitivity(a1), -1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborRateSensitivity test = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    coverImmutableBean(test);
    IborRateSensitivity test2 = IborRateSensitivity.builder()
        .index(USD_LIBOR_3M)
        .currency(USD)
        .date(date(2015, 7, 27))
        .sensitivity(16d)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborRateSensitivity test = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    assertSerialization(test);
  }

}
