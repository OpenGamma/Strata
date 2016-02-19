/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link InflationBondInterpolatedRateObservation}.
 */
@Test
public class InflationBondInterpolatedRateObservationTest {

  private static final double START_INDEX = 135d;
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationBondInterpolatedRateObservation test = InflationBondInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getStartIndexValue(), START_INDEX);
    assertEquals(test.getReferenceEndMonth(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndInterpolationMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_builder() {
    InflationBondInterpolatedRateObservation test = InflationBondInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .startIndexValue(START_INDEX)
        .referenceEndMonth(END_MONTH_FIRST)
        .referenceEndInterpolationMonth(END_MONTH_SECOND)
        .weight(WEIGHT)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getStartIndexValue(), START_INDEX);
    assertEquals(test.getReferenceEndMonth(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndInterpolationMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationBondInterpolatedRateObservation.builder()
        .index(GB_HICP)
        .startIndexValue(START_INDEX)
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 7))
        .weight(WEIGHT)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationBondInterpolatedRateObservation test = InflationBondInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationBondInterpolatedRateObservation test1 = InflationBondInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    coverImmutableBean(test1);
    InflationBondInterpolatedRateObservation test2 = InflationBondInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .startIndexValue(334d)
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 8))
        .weight(WEIGHT + 0.1d)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationBondInterpolatedRateObservation test = InflationBondInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
