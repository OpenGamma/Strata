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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndexObservation;

/**
 * Test {@link InflationEndInterpolatedRateComputation}.
 */
public class InflationEndInterpolatedRateComputationTest {

  private static final double START_INDEX = 135d;
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    InflationEndInterpolatedRateComputation test = InflationEndInterpolatedRateComputation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertThat(test.getIndex()).isEqualTo(GB_HICP);
    assertThat(test.getEndObservation().getFixingMonth()).isEqualTo(END_MONTH_FIRST);
    assertThat(test.getEndSecondObservation().getFixingMonth()).isEqualTo(END_MONTH_SECOND);
    assertThat(test.getStartIndexValue()).isEqualTo(START_INDEX);
    assertThat(test.getWeight()).isCloseTo(WEIGHT, offset(1.0e-14));
  }

  @Test
  public void test_wrongMonthOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InflationEndInterpolatedRateComputation.meta().builder()
            .set(InflationEndInterpolatedRateComputation.meta().startIndexValue(), START_INDEX)
            .set(InflationEndInterpolatedRateComputation.meta().endObservation(),
                PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
            .set(InflationEndInterpolatedRateComputation.meta().endSecondObservation(),
                PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
            .set(InflationEndInterpolatedRateComputation.meta().weight(), WEIGHT)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    InflationEndInterpolatedRateComputation test = InflationEndInterpolatedRateComputation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GB_HICP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    InflationEndInterpolatedRateComputation test1 = InflationEndInterpolatedRateComputation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    coverImmutableBean(test1);
    InflationEndInterpolatedRateComputation test2 = InflationEndInterpolatedRateComputation.of(
        CH_CPI, 334d, YearMonth.of(2010, 7), WEIGHT + 1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    InflationEndInterpolatedRateComputation test = InflationEndInterpolatedRateComputation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
