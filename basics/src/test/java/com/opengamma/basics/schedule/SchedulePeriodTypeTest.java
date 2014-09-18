/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import static com.opengamma.basics.schedule.SchedulePeriodType.FINAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.INITIAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.NORMAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.TERM;
import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link SchedulePeriodType}.
 */
@Test
public class SchedulePeriodTypeTest {

  public void test_of_ints() {
    assertEquals(SchedulePeriodType.of(0, 0), TERM);
    assertEquals(SchedulePeriodType.of(1, 0), TERM);
    assertEquals(SchedulePeriodType.of(2, 0), TERM);
    
    assertEquals(SchedulePeriodType.of(0, 1), TERM);
    assertEquals(SchedulePeriodType.of(1, 1), TERM);
    assertEquals(SchedulePeriodType.of(2, 1), TERM);
    
    assertEquals(SchedulePeriodType.of(0, 2), TERM);
    assertEquals(SchedulePeriodType.of(1, 2), TERM);
    assertEquals(SchedulePeriodType.of(2, 2), TERM);
    
    assertEquals(SchedulePeriodType.of(0, 3), INITIAL);
    assertEquals(SchedulePeriodType.of(1, 3), FINAL);
    assertEquals(SchedulePeriodType.of(2, 3), FINAL);
    
    assertEquals(SchedulePeriodType.of(0, 4), INITIAL);
    assertEquals(SchedulePeriodType.of(1, 4), NORMAL);
    assertEquals(SchedulePeriodType.of(2, 4), FINAL);
    assertEquals(SchedulePeriodType.of(3, 4), FINAL);
    
    assertEquals(SchedulePeriodType.of(0, 5), INITIAL);
    assertEquals(SchedulePeriodType.of(1, 5), NORMAL);
    assertEquals(SchedulePeriodType.of(2, 5), NORMAL);
    assertEquals(SchedulePeriodType.of(3, 5), FINAL);
    assertEquals(SchedulePeriodType.of(4, 5), FINAL);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
      return new Object[][] {
          {INITIAL, "Initial"},
          {NORMAL, "Normal"},
          {FINAL, "Final"},
          {TERM, "Term"},
      };
  }

  @Test(dataProvider = "name")
  public void test_toString(SchedulePeriodType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(SchedulePeriodType convention, String name) {
    assertEquals(SchedulePeriodType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> SchedulePeriodType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> SchedulePeriodType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(SchedulePeriodType.class);
  }

  public void test_serialization() {
    assertSerialization(NORMAL);
  }

  public void test_jodaConvert() {
    assertJodaConvert(SchedulePeriodType.class, NORMAL);
  }

}
