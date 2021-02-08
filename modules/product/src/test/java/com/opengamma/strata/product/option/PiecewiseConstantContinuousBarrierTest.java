/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link PiecewiseConstantContinuousBarrier}.
 */
public class PiecewiseConstantContinuousBarrierTest {

  private static final DoubleArray BARRIER_LEVELS = DoubleArray.of(1.0d, 0.5d, Double.MAX_VALUE);
  private static final List<LocalDate> SWITCH_DATES =
      ImmutableList.of(LocalDate.of(2020, 8, 18), LocalDate.of(2021, 8, 18));
  private static final BarrierType BARRIER_TYPE = BarrierType.DOWN;
  private static final KnockType KNOCK_TYPE = KnockType.KNOCK_IN;

  @Test
  public void test_builder() {
    PiecewiseConstantContinuousBarrier test = PiecewiseConstantContinuousBarrier.builder()
        .barrierType(BARRIER_TYPE)
        .knockType(KNOCK_TYPE)
        .barrierLevels(BARRIER_LEVELS)
        .switchDates(SWITCH_DATES).build();
    assertThat(test.getBarrierType()).isEqualTo(BARRIER_TYPE);
    assertThat(test.getKnockType()).isEqualTo(KNOCK_TYPE);
    assertThat(test.getBarrierLevels()).isEqualTo(BARRIER_LEVELS);
    assertThat(test.getSwitchDates()).isEqualTo(SWITCH_DATES);
  }

  @Test
  public void test_of() {
    PiecewiseConstantContinuousBarrier test =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, SWITCH_DATES);
    assertThat(test.getBarrierType()).isEqualTo(BARRIER_TYPE);
    assertThat(test.getKnockType()).isEqualTo(KNOCK_TYPE);
    assertThat(test.getBarrierLevels()).isEqualTo(BARRIER_LEVELS);
    assertThat(test.getSwitchDates()).isEqualTo(SWITCH_DATES);
  }

  @Test
  public void test_level() {
    PiecewiseConstantContinuousBarrier test =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, SWITCH_DATES);
    List<LocalDate> testDates =
        ImmutableList.of(LocalDate.of(2020, 7, 18), LocalDate.of(2020, 8, 18),
            LocalDate.of(2020, 8, 18), LocalDate.of(2021, 8, 18), LocalDate.of(2021, 9, 18));
    DoubleArray testLevels = DoubleArray.of(1.0d, 0.5d, 0.5d, Double.MAX_VALUE, Double.MAX_VALUE);
    for (int i = 0; i < testDates.size(); i++) {
      assertThat(test.getBarrierLevel(testDates.get(i))).isEqualTo(testLevels.get(i));
    }
  }

  @Test
  public void test_level_noswitch() {
    PiecewiseConstantContinuousBarrier test =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, DoubleArray.of(1.0d), ImmutableList.of());
    List<LocalDate> testDates =
        ImmutableList.of(LocalDate.of(2020, 7, 18), LocalDate.of(2020, 8, 18),
            LocalDate.of(2020, 8, 18), LocalDate.of(2021, 8, 18), LocalDate.of(2021, 9, 18));
    DoubleArray testLevels = DoubleArray.of(1.0d, 1.0d, 1.0d, 1.0d, 1.0d);
    for (int i = 0; i < testDates.size(); i++) {
      assertThat(test.getBarrierLevel(testDates.get(i))).isEqualTo(testLevels.get(i));
    }
  }

  @Test
  public void test_inverseKnockType() {
    PiecewiseConstantContinuousBarrier base =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, SWITCH_DATES);
    PiecewiseConstantContinuousBarrier test = base.inverseKnockType();
    PiecewiseConstantContinuousBarrier expected = PiecewiseConstantContinuousBarrier
        .of(BARRIER_TYPE, KnockType.KNOCK_OUT, BARRIER_LEVELS, SWITCH_DATES);
    assertThat(test).isEqualTo(expected);
    assertThat(test.inverseKnockType()).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_fail() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PiecewiseConstantContinuousBarrier
            .of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, ImmutableList.of(LocalDate.of(2020, 8, 18))));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> PiecewiseConstantContinuousBarrier
            .of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS,
                ImmutableList.of(LocalDate.of(2021, 8, 18), LocalDate.of(2020, 8, 18))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    PiecewiseConstantContinuousBarrier test1 =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, SWITCH_DATES);
    PiecewiseConstantContinuousBarrier test2 =
        PiecewiseConstantContinuousBarrier
        .of(BarrierType.UP, KnockType.KNOCK_OUT, DoubleArray.of(0.5d, 1.0d), ImmutableList.of(LocalDate.of(2021, 8, 18)));
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    PiecewiseConstantContinuousBarrier test =
        PiecewiseConstantContinuousBarrier.of(BARRIER_TYPE, KNOCK_TYPE, BARRIER_LEVELS, SWITCH_DATES);
    assertSerialization(test);
  }

}
