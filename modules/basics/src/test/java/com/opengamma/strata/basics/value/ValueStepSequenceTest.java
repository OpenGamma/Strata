/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;

/**
 * Test {@link ValueStepSequence}.
 */
@Test
public class ValueStepSequenceTest {

  private static final ValueAdjustment ADJ = ValueAdjustment.ofDeltaAmount(-100);
  private static final ValueAdjustment ADJ2 = ValueAdjustment.ofDeltaAmount(-200);
  private static final ValueAdjustment ADJ_BAD = ValueAdjustment.ofReplace(100);

  //-------------------------------------------------------------------------
  public void test_of() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    assertEquals(test.getFirstStepDate(), date(2016, 4, 20));
    assertEquals(test.getLastStepDate(), date(2016, 10, 20));
    assertEquals(test.getFrequency(), Frequency.P3M);
    assertEquals(test.getAdjustment(), ADJ);
  }

  public void test_of_invalid() {
    assertThrowsIllegalArg(() -> ValueStepSequence.of(date(2016, 4, 20), date(2016, 4, 19), Frequency.P3M, ADJ));
    assertThrowsIllegalArg(() -> ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ_BAD));
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    ValueStep baseStep = ValueStep.of(date(2016, 1, 20), ValueAdjustment.ofReplace(500d));
    List<ValueStep> steps = test.resolve(ImmutableList.of(baseStep), RollConventions.NONE);
    assertEquals(steps.size(), 4);
    assertEquals(steps.get(0), baseStep);
    assertEquals(steps.get(1), ValueStep.of(date(2016, 4, 20), ADJ));
    assertEquals(steps.get(2), ValueStep.of(date(2016, 7, 20), ADJ));
    assertEquals(steps.get(3), ValueStep.of(date(2016, 10, 20), ADJ));
  }

  public void test_resolve_invalid() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P12M, ADJ);
    ValueStep baseStep = ValueStep.of(date(2016, 1, 20), ValueAdjustment.ofReplace(500d));
    assertThrowsIllegalArg(() -> test.resolve(ImmutableList.of(baseStep), RollConventions.NONE));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    coverImmutableBean(test);
    ValueStepSequence test2 = ValueStepSequence.of(date(2016, 4, 1), date(2016, 10, 1), Frequency.P1M, ADJ2);
    coverImmutableBean(test2);
  }

  public void test_serialization() {
    ValueStepSequence test = ValueStepSequence.of(date(2016, 4, 20), date(2016, 10, 20), Frequency.P3M, ADJ);
    assertSerialization(test);
  }

}
