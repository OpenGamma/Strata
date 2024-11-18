/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;

/**
 * Test {@link ValueStepSequence}.
 */
public class ValueStepSequenceTest {

  private static final ValueAdjustment ADJ = ValueAdjustment.ofDeltaAmount(-100);
  private static final ValueAdjustment ADJ2 = ValueAdjustment.ofDeltaAmount(-200);
  private static final ValueAdjustment ADJ_BAD = ValueAdjustment.ofReplace(100);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    assertThat(test.getFirstStepDate()).isEqualTo(date(2016, 4, 20));
    assertThat(test.getLastStepDate()).isEqualTo(date(2016, 10, 20));
    assertThat(test.getFrequency()).isEqualTo(Frequency.P3M);
    assertThat(test.getAdjustment()).isEqualTo(ADJ);
  }

  @Test
  public void test_of_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStepSequence.of(date(2016, 4, 20), date(2016, 4, 19), Frequency.P3M, ADJ));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ_BAD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    ValueStep baseStep = ValueStep.of(date(2016, 1, 20), ValueAdjustment.ofReplace(500d));
    List<ValueStep> steps = test.resolve(ImmutableList.of(baseStep), RollConventions.NONE);
    assertThat(steps.size()).isEqualTo(4);
    assertThat(steps.get(0)).isEqualTo(baseStep);
    assertThat(steps.get(1)).isEqualTo(ValueStep.of(date(2016, 4, 20), ADJ));
    assertThat(steps.get(2)).isEqualTo(ValueStep.of(date(2016, 7, 20), ADJ));
    assertThat(steps.get(3)).isEqualTo(ValueStep.of(date(2016, 10, 20), ADJ));
  }

  @Test
  public void test_resolve_with_roll_convention() {
    ValueStepSequence test = ValueStepSequence.of(date(2022, 9, 21), date(2026, 9, 21), Frequency.P12M, ADJ);
    List<ValueStep> steps = test.resolve(ImmutableList.of(), RollConventions.IMM);
    assertThat(steps.size()).isEqualTo(5);
    assertThat(steps.get(0)).isEqualTo(ValueStep.of(date(2022, 9, 21), ADJ));
    assertThat(steps.get(1)).isEqualTo(ValueStep.of(date(2023, 9, 20), ADJ));
    assertThat(steps.get(2)).isEqualTo(ValueStep.of(date(2024, 9, 18), ADJ));
    assertThat(steps.get(3)).isEqualTo(ValueStep.of(date(2025, 9, 17), ADJ));
    assertThat(steps.get(4)).isEqualTo(ValueStep.of(date(2026, 9, 16), ADJ));
  }

  @Test
  public void test_resolve_invalid() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P12M, ADJ);
    ValueStep baseStep = ValueStep.of(date(2016, 1, 20), ValueAdjustment.ofReplace(500d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.resolve(ImmutableList.of(baseStep), RollConventions.NONE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    coverImmutableBean(test);
    ValueStepSequence test2 = ValueStepSequence.of(date(2016, 4, 1), date(2016, 10, 1), Frequency.P1M, ADJ2);
    coverImmutableBean(test2);
  }

  @Test
  public void test_serialization() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    assertSerialization(test);
  }

}
