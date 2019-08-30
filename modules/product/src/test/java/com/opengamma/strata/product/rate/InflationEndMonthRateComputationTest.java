/*
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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link InflationEndMonthRateComputation}.
 */
public class InflationEndMonthRateComputationTest {

  private static final double START_INDEX = 535d;
  private static final YearMonth END_MONTH = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    InflationEndMonthRateComputation test =
        InflationEndMonthRateComputation.of(GB_HICP, START_INDEX, END_MONTH);
    assertThat(test.getIndex()).isEqualTo(GB_HICP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    InflationEndMonthRateComputation test =
        InflationEndMonthRateComputation.of(GB_HICP, START_INDEX, END_MONTH);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GB_HICP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    InflationEndMonthRateComputation test1 =
        InflationEndMonthRateComputation.of(GB_HICP, START_INDEX, END_MONTH);
    coverImmutableBean(test1);
    InflationEndMonthRateComputation test2 =
        InflationEndMonthRateComputation.of(CH_CPI, 2324d, YearMonth.of(2015, 4));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    InflationEndMonthRateComputation test =
        InflationEndMonthRateComputation.of(GB_HICP, START_INDEX, END_MONTH);
    assertSerialization(test);
  }

}
