/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link InflationMonthlyRateComputation}.
 */
public class InflationMonthlyRateComputationTest {

  private static final YearMonth START_MONTH = YearMonth.of(2014, 1);
  private static final YearMonth END_MONTH = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    InflationMonthlyRateComputation test =
        InflationMonthlyRateComputation.of(GB_HICP, START_MONTH, END_MONTH);
    assertThat(test.getIndex()).isEqualTo(GB_HICP);
  }

  @Test
  public void test_wrongMonthOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InflationMonthlyRateComputation.of(GB_HICP, END_MONTH, START_MONTH));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InflationMonthlyRateComputation.of(GB_HICP, START_MONTH, START_MONTH));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    InflationMonthlyRateComputation test =
        InflationMonthlyRateComputation.of(GB_HICP, START_MONTH, END_MONTH);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GB_HICP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    InflationMonthlyRateComputation test1 =
        InflationMonthlyRateComputation.of(GB_HICP, START_MONTH, END_MONTH);
    coverImmutableBean(test1);
    InflationMonthlyRateComputation test2 =
        InflationMonthlyRateComputation.of(CH_CPI, YearMonth.of(2014, 4), YearMonth.of(2015, 4));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    InflationMonthlyRateComputation test =
        InflationMonthlyRateComputation.of(GB_HICP, START_MONTH, END_MONTH);
    assertSerialization(test);
  }

}
