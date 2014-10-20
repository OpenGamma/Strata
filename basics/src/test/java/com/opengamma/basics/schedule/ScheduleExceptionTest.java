/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.basics.date.BusinessDayAdjustment;

/**
 * Test {@link ScheduleException}.
 */
@Test
public class ScheduleExceptionTest {

  public void test_of_ints() {
    PeriodicScheduleDefn defn = PeriodicScheduleDefn.of(
        date(2014, 6, 30), date(2014, 8, 30), Frequency.P1M,
        BusinessDayAdjustment.NONE, StubConvention.NONE, false);
    ScheduleException test = new ScheduleException(defn , "Hello {}", "World");
    assertEquals(test.getMessage(), "Hello World");
    assertEquals(test.getDefinition(), defn);
  }

}
