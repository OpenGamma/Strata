/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.value;

import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.RollConventions.DAY_1;
import static com.opengamma.basics.schedule.SchedulePeriodType.FINAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.INITIAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.NORMAL;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.basics.schedule.SchedulePeriod;

/**
 * Test {@link ValueSchedule}.
 */
@Test
public class ValueScheduleTest {

  private static double TOLERANCE = 0.0001d;
  private static ValueStep STEP1 = ValueStep.ofAbsolute(date(2014, 6, 30), 2000d);
  private static ValueStep STEP2 = ValueStep.ofAbsolute(date(2014, 7, 30), 3000d);

  private static SchedulePeriod PERIOD1 = SchedulePeriod.of(
      INITIAL, date(2014, 1, 1), date(2014, 2, 1), P1M, DAY_1);
  private static SchedulePeriod PERIOD2 = SchedulePeriod.of(
      NORMAL, date(2014, 2, 1), date(2014, 3, 1), P1M, DAY_1);
  private static SchedulePeriod PERIOD3 = SchedulePeriod.of(
      FINAL, date(2014, 3, 1), date(2014, 4, 1), date(2014, 3, 2), date(2014, 4, 1), P1M, DAY_1);
  private static ImmutableList<SchedulePeriod> PERIODS = ImmutableList.of(PERIOD1, PERIOD2, PERIOD3);
  
  public void test_of_int() {
    ValueSchedule test = ValueSchedule.of(10000d);
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_of_intSteps() {
    ValueSchedule test = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    assertEquals(test.getInitialValue(), 10000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of(STEP1, STEP2));
  }

  public void test_builder_validEmpty() {
    ValueSchedule test = ValueSchedule.builder().build();
    assertEquals(test.getInitialValue(), 0d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of());
  }

  public void test_builder_validFull() {
    ValueSchedule test = ValueSchedule.builder().initialValue(2000d).steps(ImmutableList.of(STEP1, STEP2)).build();
    assertEquals(test.getInitialValue(), 2000d, TOLERANCE);
    assertEquals(test.getSteps(), ImmutableList.of(STEP1, STEP2));
  }

  //-------------------------------------------------------------------------
  public void test_resolveValues_dateBased() {
    ValueStep step1 = ValueStep.ofAbsolute(date(2014, 2, 1), 300d);
    ValueStep step2 = ValueStep.ofAbsolute(date(2014, 3, 1), 400d);
    
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_dateBased_matchAdjusted() {
    ValueStep step1 = ValueStep.ofAbsolute(date(2014, 2, 1), 300d);
    ValueStep step2 = ValueStep.ofAbsolute(date(2014, 3, 2), 400d);
    
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_indexBased() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofAbsolute(300d));
    ValueStep step2 = ValueStep.of(2, ValueAdjustment.ofAbsolute(400d));
    
    ValueSchedule test0 = ValueSchedule.of(200d, ImmutableList.of());
    assertEquals(test0.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 200d));
    
    ValueSchedule test1a = ValueSchedule.of(200d, ImmutableList.of(step1));
    assertEquals(test1a.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
    
    ValueSchedule test1b = ValueSchedule.of(200d, ImmutableList.of(step2));
    assertEquals(test1b.resolveValues(PERIODS), ImmutableList.of(200d, 200d, 400d));
    
    ValueSchedule test2 = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test2.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 400d));
  }

  public void test_resolveValues_indexBased_duplicateDefinitionValid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofAbsolute(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofAbsolute(300d));
    
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertEquals(test.resolveValues(PERIODS), ImmutableList.of(200d, 300d, 300d));
  }

  public void test_resolveValues_indexBased_duplicateDefinitionInvalid() {
    ValueStep step1 = ValueStep.of(1, ValueAdjustment.ofAbsolute(300d));
    ValueStep step2 = ValueStep.of(1, ValueAdjustment.ofAbsolute(400d));
    
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step1, step2));
    assertThrowsIllegalArg(()-> test.resolveValues(PERIODS));
  }

  public void test_resolveValues_indexBased_indexTooBig() {
    ValueStep step = ValueStep.of(3, ValueAdjustment.ofAbsolute(300d));
    
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThrowsIllegalArg(()-> test.resolveValues(PERIODS));
  }

  public void test_resolveValues_dateBased_indexZeroInvalid() {
    ValueStep step = ValueStep.ofAbsolute(date(2014, 1, 1), 300d);
    
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThrowsIllegalArg(()-> test.resolveValues(PERIODS));
  }

  public void test_resolveValues_dateBased_dateInvalid() {
    ValueStep step = ValueStep.ofAbsolute(date(2014, 4, 1), 300d);
    
    ValueSchedule test = ValueSchedule.of(200d, ImmutableList.of(step));
    assertThrowsIllegalArg(()-> test.resolveValues(PERIODS));
  }

  //-------------------------------------------------------------------------
  public void equals() {
    ValueSchedule a1 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule a2 = ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule b = ValueSchedule.of(5000d, Lists.newArrayList(STEP1, STEP2));
    ValueSchedule c = ValueSchedule.of(10000d, Lists.newArrayList(STEP1));
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2)));
  }

  public void test_serialization() {
    assertSerialization(ValueSchedule.of(10000d, Lists.newArrayList(STEP1, STEP2)));
  }

}
