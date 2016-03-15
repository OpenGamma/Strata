/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link InflationMonthlyRateObservation}.
 */
@Test
public class InflationMonthlyRateObservationTest {

  private static final YearMonth START_MONTH = YearMonth.of(2014, 1);
  private static final YearMonth END_MONTH = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationMonthlyRateObservation test =
        InflationMonthlyRateObservation.of(GB_HICP, START_MONTH, END_MONTH);
    assertEquals(test.getIndex(), GB_HICP);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationMonthlyRateObservation.of(GB_HICP, END_MONTH, START_MONTH));
    assertThrowsIllegalArg(() -> InflationMonthlyRateObservation.of(GB_HICP, START_MONTH, START_MONTH));
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationMonthlyRateObservation test =
        InflationMonthlyRateObservation.of(GB_HICP, START_MONTH, END_MONTH);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationMonthlyRateObservation test1 =
        InflationMonthlyRateObservation.of(GB_HICP, START_MONTH, END_MONTH);
    coverImmutableBean(test1);
    InflationMonthlyRateObservation test2 =
        InflationMonthlyRateObservation.of(CH_CPI, YearMonth.of(2014, 4), YearMonth.of(2015, 4));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationMonthlyRateObservation test =
        InflationMonthlyRateObservation.of(GB_HICP, START_MONTH, END_MONTH);
    assertSerialization(test);
  }

}
