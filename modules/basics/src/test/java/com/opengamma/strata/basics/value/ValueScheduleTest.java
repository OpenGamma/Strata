/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ValueSchedule}.
 */
public class ValueScheduleTest {

  private static ValueStep STEP1 = ValueStep.of(date(2014, 6, 30), ValueAdjustment.ofReplace(2000d));
  private static ValueStep STEP2 = ValueStep.of(date(2014, 7, 30), ValueAdjustment.ofReplace(3000d));

  private static SchedulePeriod PERIOD1 = SchedulePeriod.of(date(2014, 1, 1), date(2014, 2, 1));
  private static SchedulePeriod PERIOD2 = SchedulePeriod.of(date(2014, 2, 1), date(2014, 3, 1));
  private static SchedulePeriod PERIOD3 = SchedulePeriod.of(
      date(2014, 3, 1), date(2014, 4, 1), date(2014, 3, 2), date(2014, 4, 1));
  private static ImmutableList<SchedulePeriod> PERIODS = ImmutableList.of(PERIOD1, PERIOD2, PERIOD3);
  private static Schedule SCHEDULE = Schedule.builder()
      .periods(PERIODS)
      .frequency(Frequency.P1M)
      .rollConvention(RollConventions.DAY_1)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_constant_ALWAYS_0() {
    ValueSchedule test = ValueSchedule.ALWAYS_0;
    assertThat(test.getInitialValue()).isEqualTo(0d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_constant_ALWAYS_1() {
    ValueSchedule test = ValueSchedule.ALWAYS_1;
    assertThat(test.getInitialValue()).isEqualTo(1d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_int() {
    ValueSchedule test = ValueSchedule.of(10000d);
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_of_intStepsArray() {
    ValueSchedule test = ValueSchedule.of(10000d, STEP1, STEP2);
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).containsExactly(STEP1, STEP2);
  }

  @Test
  public void test_of_intStepsArray_empty() {
    ValueSchedule test = ValueSchedule.of(10000d, new ValueStep[0]);
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_of_intStepsList() {
    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).containsExactly(STEP1, STEP2);
  }

  @Test
  public void test_of_intStepsList_empty() {
    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList());
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_of_sequence() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ValueAdjustment.ofDeltaAmount(-100));
    ValueSchedule test = ValueSchedule.of(10000d, seq);
    assertThat(test.getInitialValue()).isEqualTo(10000d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
    assertThat(test.getStepSequence()).isEqualTo(Optional.of(seq));
  }

  @Test
  public void test_builder_validEmpty() {
    ValueSchedule test = ValueSchedule.builder().build();
    assertThat(test.getInitialValue()).isEqualTo(0d);
    assertThat(test.getSteps()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_builder_validFull() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ValueAdjustment.ofDeltaAmount(-100));
    ValueSchedule test = ValueSchedule.builder()
        .initialValue(2000d)
        .steps(STEP1, STEP2)
        .stepSequence(seq)
        .build();
    assertThat(test.getInitialValue()).isEqualTo(2000d);
    assertThat(test.getSteps()).containsExactly(STEP1, STEP2);
    assertThat(test.getStepSequence()).isEqualTo(Optional.of(seq));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveValues_dateBased() {
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(date(2014, 3, 1), ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertThat(test0.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertThat(test1a.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertThat(test1b.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThat(test2.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 400d));
  }

  @Test
  public void test_resolveValues_dateBased_matchAdjusted() {
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(date(2014, 3, 2), ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertThat(test0.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertThat(test1a.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertThat(test1b.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThat(test2.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 400d));
  }

  @Test
  public void test_resolveValues_dateBased_ignoreExcess() {
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(date(2014, 2, 15), ValueAdjustment.ofReplace(300d));  // no change to value
    ValueStep step3 = ValueStep.of(date(2014, 3, 1), ValueAdjustment.ofReplace(400d));
    ValueStep step4 = ValueStep.of(date(2014, 3, 15), ValueAdjustment.ofDeltaAmount(0d));  // no change to value
    ValueStep step5 = ValueStep.of(date(2014, 4, 1), ValueAdjustment.ofMultiplier(1d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2, step3, step4, step5));
    assertThat(test.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 400d));
  }

  @Test
  public void test_resolveValues_indexBased() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(2, ValueAdjustment.ofReplace(400d));
    // no steps
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertThat(test0.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 200d));
    // step1
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertThat(test1a.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 300d));
    // step2
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertThat(test1b.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 200d, 400d));
    // step1 and step2
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThat(test2.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 400d));
  }

  @Test
  public void test_resolveValues_indexBased_duplicateDefinitionValid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThat(test.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 300d));
  }

  @Test
  public void test_resolveValues_indexBased_duplicateDefinitionInvalid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofReplace(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofReplace(400d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThatIllegalArgumentException().isThrownBy(() -> test.resolveValues(SCHEDULE));
  }

  @Test
  public void test_resolveValues_dateBased_indexZeroValid() {
    ValueStep step = ValueStep.of(date(2014, 1, 1), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThat(test.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(300d, 300d, 300d));
  }

  @Test
  public void test_resolveValues_indexBased_indexTooBig() {
    ValueStep step = ValueStep.of(3, ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThatIllegalArgumentException().isThrownBy(() -> test.resolveValues(SCHEDULE));
  }

  @Test
  public void test_resolveValues_dateBased_invalidChangeValue() {
    ValueStep step = ValueStep.of(date(2014, 4, 1), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.resolveValues(SCHEDULE))
        .withMessageStartingWith("ValueStep date does not match a period boundary");
  }

  @Test
  public void test_resolveValues_dateBased_invalidDateBefore() {
    ValueStep step = ValueStep.of(date(2013, 12, 31), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.resolveValues(SCHEDULE))
        .withMessageStartingWith("ValueStep date is before the start of the schedule");
  }

  @Test
  public void test_resolveValues_dateBased_invalidDateAfter() {
    ValueStep step = ValueStep.of(date(2014, 4, 3), ValueAdjustment.ofReplace(300d));
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.resolveValues(SCHEDULE))
        .withMessageStartingWith("ValueStep date is after the end of the schedule");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolveValues_sequence() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2014, 2, 1), date(2014, 3, 1), Frequency.P1M, ValueAdjustment.ofDeltaAmount(100));
    ValueSchedule test = ValueSchedule.of(200d, seq);
    assertThat(test.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(200d, 300d, 400d));
  }

  @Test
  public void test_resolveValues_sequenceAndSteps() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2014, 2, 1), date(2014, 3, 1), Frequency.P1M, ValueAdjustment.ofDeltaAmount(100));
    ValueStep step1 = ValueStep.of(date(2014, 1, 1), ValueAdjustment.ofReplace(350d));
    ValueSchedule test = ValueSchedule.builder().initialValue(200d).steps(step1).stepSequence(seq).build();
    assertThat(test.resolveValues(SCHEDULE)).isEqualTo(DoubleArray.of(350d, 450d, 550d));
  }

  @Test
  public void test_resolveValues_sequenceAndStepClash() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2014, 2, 1), date(2014, 3, 1), Frequency.P1M, ValueAdjustment.ofDeltaAmount(100));
    ValueStep step1 = ValueStep.of(date(2014, 2, 1), ValueAdjustment.ofReplace(350d));
    ValueSchedule test = ValueSchedule.builder().initialValue(200d).steps(step1).stepSequence(seq).build();
    assertThatIllegalArgumentException().isThrownBy(() -> test.resolveValues(SCHEDULE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    ValueSchedule a1 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule a2 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule b = ValueSchedule.of(5000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule c = ValueSchedule.of(10000d, Lists.newArrayList(STEP1));
    assertThat(a1)
        .isEqualTo(a1)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ValueStepSequence seq = ValueStepSequence.of(
        date(2014, 2, 1), date(2014, 3, 1), Frequency.P1M, ValueAdjustment.ofDeltaAmount(100));

    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    coverImmutableBean(test);
    coverBeanEquals(test, ValueSchedule.of(20000d, seq));
  }

  @Test
  public void test_serialization() {
    assertSerialization(ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2)));
  }

}
