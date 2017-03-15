/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

/**
 * Test {@link PriceIndexObservation}.
 */
@Test
public class PriceIndexObservationTest {

  private static final YearMonth FIXING_MONTH = YearMonth.of(2016, 2);

  public void test_of() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getFixingMonth(), FIXING_MONTH);
    assertEquals(test.getCurrency(), GB_HICP.getCurrency());
    assertEquals(test.toString(), "PriceIndexObservation[GB-HICP on 2016-02]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    coverImmutableBean(test);
    PriceIndexObservation test2 = PriceIndexObservation.of(CH_CPI, FIXING_MONTH.plusMonths(1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    assertSerialization(test);
  }

}
