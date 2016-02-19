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
 * Test {@link InflationBondMonthlyRateObservation}.
 */
@Test
public class InflationBondMonthlyRateObservationTest {

  private static final double START_INDEX = 535d;
  private static final YearMonth START_MONTH = YearMonth.of(2014, 1);
  private static final YearMonth END_MONTH = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationBondMonthlyRateObservation test =
        InflationBondMonthlyRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getStartIndexValue(), START_INDEX);
    assertEquals(test.getReferenceEndMonth(), END_MONTH);
  }

  public void test_builder() {
    InflationBondMonthlyRateObservation test = InflationBondMonthlyRateObservation.builder()
        .index(CH_CPI)
        .startIndexValue(START_INDEX)
        .referenceEndMonth(END_MONTH)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getStartIndexValue(), START_INDEX);
    assertEquals(test.getReferenceEndMonth(), END_MONTH);

  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationBondMonthlyRateObservation test =
        InflationBondMonthlyRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationBondMonthlyRateObservation test1 =
        InflationBondMonthlyRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    coverImmutableBean(test1);
    InflationBondMonthlyRateObservation test2 =
        InflationBondMonthlyRateObservation.of(CH_CPI, 2324d, YearMonth.of(2015, 4));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationBondMonthlyRateObservation test =
        InflationBondMonthlyRateObservation.of(GB_HICP, START_INDEX, END_MONTH);
    assertSerialization(test);
  }

}
