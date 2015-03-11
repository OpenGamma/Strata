/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
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
public class ZeroRateSensitivityTest {

  public void test_of() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP);
  }

  public void test_builder() {
    ZeroRateSensitivity test = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    ZeroRateSensitivity base = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity test = base.withSensitivity(20d);
    assertEquals(base.getCurrency(), GBP);
    assertEquals(base.getDate(), date(2015, 8, 27));
    assertEquals(base.getSensitivity(), 32d);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 20d);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    ZeroRateSensitivity a1 = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity a2 = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity b = ZeroRateSensitivity.builder()
        .currency(USD)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity c = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 9, 27))
        .sensitivity(32d)
        .build();
    IborRateSensitivity d = IborRateSensitivity.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .fixingDate(date(2015, 9, 27))
        .sensitivity(32d)
        .build();
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b), -1);
    assertEquals(b.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(c), -1);
    assertEquals(c.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(d), -1);
    assertEquals(d.compareExcludingSensitivity(a1), 1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRateSensitivity test = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    coverImmutableBean(test);
    ZeroRateSensitivity test2 = ZeroRateSensitivity.builder()
        .currency(USD)
        .date(date(2015, 7, 27))
        .sensitivity(16d)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ZeroRateSensitivity test = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 8, 27))
        .sensitivity(32d)
        .build();
    assertSerialization(test);
  }

}
