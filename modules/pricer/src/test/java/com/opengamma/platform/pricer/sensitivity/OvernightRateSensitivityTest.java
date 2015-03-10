/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class OvernightRateSensitivityTest {

  public void test_of_findMaturityDate() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 8, 27), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getEndDate(), date(2015, 8, 28));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP_SONIA);
  }

  public void test_of() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(
        GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 10, 27), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getEndDate(), date(2015, 10, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP_SONIA);
  }

  public void test_builder() {
    OvernightRateSensitivity test = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getEndDate(), date(2015, 10, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurveKey(), GBP_SONIA);
  }

  public void test_builder_badDateOrder() {
    assertThrowsIllegalArg(() -> OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 8, 27))
        .sensitivity(32d)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    OvernightRateSensitivity base = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity test = base.withSensitivity(20d);
    assertEquals(base.getIndex(), GBP_SONIA);
    assertEquals(base.getCurrency(), GBP);
    assertEquals(base.getDate(), date(2015, 8, 27));
    assertEquals(base.getSensitivity(), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 20d);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    OvernightRateSensitivity a1 = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity a2 = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity b = OvernightRateSensitivity.builder()
        .index(USD_FED_FUND)
        .currency(USD)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity c = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(USD)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity d = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(USD)
        .date(date(2015, 9, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    OvernightRateSensitivity e = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 11, 27))
        .sensitivity(32d)
        .build();
    ZeroRateSensitivity f = ZeroRateSensitivity.builder()
        .currency(GBP)
        .date(date(2015, 9, 27))
        .sensitivity(32d)
        .build();
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b), -1);
    assertEquals(b.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(c), -1);
    assertEquals(c.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(e), -1);
    assertEquals(d.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(d), -1);
    assertEquals(e.compareExcludingSensitivity(a1), 1);
    assertEquals(a1.compareExcludingSensitivity(f), 1);
    assertEquals(f.compareExcludingSensitivity(a1), -1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightRateSensitivity test = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    coverImmutableBean(test);
    OvernightRateSensitivity test2 = OvernightRateSensitivity.builder()
        .index(USD_FED_FUND)
        .currency(USD)
        .date(date(2015, 7, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(16d)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightRateSensitivity test = OvernightRateSensitivity.builder()
        .index(GBP_SONIA)
        .currency(GBP)
        .date(date(2015, 8, 27))
        .endDate(date(2015, 10, 27))
        .sensitivity(32d)
        .build();
    assertSerialization(test);
  }

}
