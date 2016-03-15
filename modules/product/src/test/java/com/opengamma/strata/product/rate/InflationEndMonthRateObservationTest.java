/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link InflationEndMonthRateObservation}.
 */
@Test
public class InflationEndMonthRateObservationTest {

  private static final double START_INDEX = 535d;
  private static final YearMonth END_MONTH = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationEndMonthRateObservation test =
        InflationEndMonthRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    assertEquals(test.getIndex(), GB_HICP);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationEndMonthRateObservation test =
        InflationEndMonthRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationEndMonthRateObservation test1 =
        InflationEndMonthRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    coverImmutableBean(test1);
    InflationEndMonthRateObservation test2 =
        InflationEndMonthRateObservation.of(CH_CPI, 2324d, YearMonth.of(2015, 4));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationEndMonthRateObservation test =
        InflationEndMonthRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    assertSerialization(test);
  }

}
