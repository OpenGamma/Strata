/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class LocalDateUtilsTest {

  public void test_dayOfYear() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.doy(date), date.getDayOfYear());
      date = date.plusDays(1);
    }
  }

  public void test_plusDays0() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.plusDays(date, 0), date.plusDays(0));
      date = date.plusDays(1);
    }
  }

  public void test_plusDays1() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.plusDays(date, 1), date.plusDays(1));
      date = date.plusDays(1);
    }
  }

  public void test_plusDays3() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.plusDays(date, 3), date.plusDays(3));
      date = date.plusDays(1);
    }
  }

  public void test_plusDays99() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.plusDays(date, 99), date.plusDays(99));
      date = date.plusDays(1);
    }
  }

  public void test_plusDaysM1() {
    LocalDate date = LocalDate.of(2012, 1, 1);
    for (int i = 0; i < 366 * 4; i++) {
      assertEquals(LocalDateUtils.plusDays(date, -1), date.plusDays(-1));
      date = date.plusDays(1);
    }
  }

  public void test_daysBetween() {
    LocalDate base = LocalDate.of(2012, 1, 1);
    LocalDate date = base;
    for (int i = 0; i < 366 * 8; i++) {
      assertEquals(LocalDateUtils.daysBetween(base, date), date.toEpochDay() - base.toEpochDay());
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    assertUtilityClass(LocalDateUtils.class);
  }

}
