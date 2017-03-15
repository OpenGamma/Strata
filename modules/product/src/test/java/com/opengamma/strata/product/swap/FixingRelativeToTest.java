/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
@Test
public class FixingRelativeToTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {FixingRelativeTo.PERIOD_START, "PeriodStart"},
        {FixingRelativeTo.PERIOD_END, "PeriodEnd"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FixingRelativeTo convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FixingRelativeTo convention, String name) {
    assertEquals(FixingRelativeTo.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> FixingRelativeTo.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> FixingRelativeTo.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void selectDate() {
    LocalDate date1 = date(2014, 3, 27);
    LocalDate date2 = date(2014, 6, 27);
    SchedulePeriod period = SchedulePeriod.of(date1, date2);
    assertEquals(FixingRelativeTo.PERIOD_START.selectBaseDate(period), date1);
    assertEquals(FixingRelativeTo.PERIOD_END.selectBaseDate(period), date2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FixingRelativeTo.class);
  }

  public void test_serialization() {
    assertSerialization(FixingRelativeTo.PERIOD_START);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FixingRelativeTo.class, FixingRelativeTo.PERIOD_START);
  }

}
