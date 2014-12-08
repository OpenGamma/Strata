/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

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
public class OvernightCompoundedRateTest {

  public void test_of_noRateCutoff() {
    OvernightCompoundedRate test = OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1));
    OvernightCompoundedRate expected = OvernightCompoundedRate.builder()
        .index(USD_FED_FUND)
        .startDate(date(2014, 6, 1))
        .endDate(date(2014, 7, 1))
        .rateCutOffDays(0)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_rateCutoff_0() {
    OvernightCompoundedRate test = OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1), 0);
    OvernightCompoundedRate expected = OvernightCompoundedRate.builder()
        .index(USD_FED_FUND)
        .startDate(date(2014, 6, 1))
        .endDate(date(2014, 7, 1))
        .rateCutOffDays(0)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_rateCutoff_2() {
    OvernightCompoundedRate test = OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1), 2);
    OvernightCompoundedRate expected = OvernightCompoundedRate.builder()
        .index(USD_FED_FUND)
        .startDate(date(2014, 6, 1))
        .endDate(date(2014, 7, 1))
        .rateCutOffDays(2)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_badDateOrder() {
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 6, 1)));
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 2), date(2014, 6, 1)));
  }

  public void test_of_rateCutoff_negative() {
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1), -1));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(null, date(2014, 6, 1), date(2014, 7, 1)));
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(USD_FED_FUND, null, date(2014, 7, 1)));
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), null));
    assertThrowsIllegalArg(() -> OvernightCompoundedRate.of(null, null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightCompoundedRate test = OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1));
    coverImmutableBean(test);
    OvernightCompoundedRate test2 = OvernightCompoundedRate.of(GBP_SONIA, date(2014, 6, 3), date(2014, 7, 3), 3);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightCompoundedRate test = OvernightCompoundedRate.of(USD_FED_FUND, date(2014, 6, 1), date(2014, 7, 1));
    assertSerialization(test);
  }

}
