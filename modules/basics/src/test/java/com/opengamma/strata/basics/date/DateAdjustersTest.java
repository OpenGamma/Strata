/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link DateAdjusters}.
 */
@Test
public class DateAdjustersTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "nextLeapDay")
  static Object[][] data_nextLeapDay() {
    return new Object[][] {
        {2000, 1, 1, 2000},
        {2000, 2, 1, 2000},
        {2000, 2, 28, 2000},
        {2000, 2, 29, 2004},
        {2000, 3, 1, 2004},

        {2009, 1, 1, 2012},
        {2009, 2, 1, 2012},
        {2009, 2, 28, 2012},
        {2009, 3, 1, 2012},

        {2010, 1, 1, 2012},
        {2010, 2, 1, 2012},
        {2010, 2, 28, 2012},
        {2010, 3, 1, 2012},

        {2012, 1, 1, 2012},
        {2012, 2, 1, 2012},
        {2012, 2, 28, 2012},
        {2012, 2, 29, 2016},
        {2012, 3, 1, 2016},

        {2013, 1, 1, 2016},
        {2013, 2, 1, 2016},
        {2013, 2, 28, 2016},
        {2013, 3, 1, 2016},

        {2014, 1, 1, 2016},
        {2014, 2, 1, 2016},
        {2014, 2, 28, 2016},
        {2014, 3, 1, 2016},

        {2015, 1, 1, 2016},
        {2015, 2, 1, 2016},
        {2015, 2, 28, 2016},
        {2015, 3, 1, 2016},

        {2016, 1, 1, 2016},
        {2016, 2, 1, 2016},
        {2016, 2, 28, 2016},
        {2016, 2, 29, 2020},
        {2016, 3, 1, 2020},

        {2017, 1, 1, 2020},

        {2096, 1, 1, 2096},
        {2096, 2, 1, 2096},
        {2096, 2, 28, 2096},
        {2096, 2, 29, 2104},
        {2096, 3, 1, 2104},

        {2100, 1, 1, 2104},
        {2100, 2, 1, 2104},
        {2100, 2, 28, 2104},
        {2100, 3, 1, 2104},
    };
  }

  @Test(dataProvider = "nextLeapDay")
  public void test_nextLeapDay_LocalDate(int year, int month, int day, int expectedYear) {
    LocalDate date = LocalDate.of(year, month, day);
    LocalDate test = DateAdjusters.nextLeapDay().adjust(date);
    assertEquals(test.getYear(), expectedYear);
    assertEquals(test.getMonthValue(), 2);
    assertEquals(test.getDayOfMonth(), 29);
  }

  @Test(dataProvider = "nextLeapDay")
  public void test_nextLeapDay_Temporal(int year, int month, int day, int expectedYear) {
    LocalDate date = LocalDate.of(year, month, day);
    LocalDate test = (LocalDate) DateAdjusters.nextLeapDay().adjustInto(date);
    assertEquals(test.getYear(), expectedYear);
    assertEquals(test.getMonthValue(), 2);
    assertEquals(test.getDayOfMonth(), 29);
  }

  @Test(dataProvider = "nextLeapDay")
  public void test_nextOrSameLeapDay_LocalDate(int year, int month, int day, int expectedYear) {
    LocalDate date = LocalDate.of(year, month, day);
    LocalDate test = DateAdjusters.nextOrSameLeapDay().adjust(date);
    if (month == 2 && day == 29) {
      assertEquals(test, date);
    } else {
      assertEquals(test.getYear(), expectedYear);
      assertEquals(test.getMonthValue(), 2);
      assertEquals(test.getDayOfMonth(), 29);
    }
  }

  @Test(dataProvider = "nextLeapDay")
  public void test_nextOrSameLeapDay_Temporal(int year, int month, int day, int expectedYear) {
    LocalDate date = LocalDate.of(year, month, day);
    LocalDate test = (LocalDate) DateAdjusters.nextOrSameLeapDay().adjustInto(date);
    if (month == 2 && day == 29) {
      assertEquals(test, date);
    } else {
      assertEquals(test.getYear(), expectedYear);
      assertEquals(test.getMonthValue(), 2);
      assertEquals(test.getDayOfMonth(), 29);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    assertUtilityClass(DateAdjusters.class);
  }

}
