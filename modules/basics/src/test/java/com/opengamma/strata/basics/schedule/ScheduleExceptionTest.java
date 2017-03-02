/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link ScheduleException}.
 */
@Test
public class ScheduleExceptionTest {

  public void test_withDefinition() {
    PeriodicSchedule defn = PeriodicSchedule.of(
        date(2014, 6, 30), date(2014, 8, 30), Frequency.P1M,
        BusinessDayAdjustment.NONE, StubConvention.NONE, false);
    ScheduleException test = new ScheduleException(defn, "Hello {}", "World");
    assertEquals(test.getMessage(), "Hello World");
    assertEquals(test.getDefinition(), Optional.of(defn));
  }

  public void test_withoutDefinition() {
    ScheduleException test = new ScheduleException("Hello {}", "World");
    assertEquals(test.getMessage(), "Hello World");
    assertEquals(test.getDefinition(), Optional.empty());
  }

}
