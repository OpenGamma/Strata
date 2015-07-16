/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;

/**
 * Test.
 */
@Test
public class IsdaCompliantScheduleGeneratorTest {
  private static final DayCount ACT = DayCounts.ACT_365F;

  private static final LocalDate START_DATE = LocalDate.of(2013, 2, 13);
  private static final LocalDate END_DATE = LocalDate.of(2015, 6, 30);
  private static final LocalDate[] DISCOUNT_CURVE_DATES = new LocalDate[] {LocalDate.of(2013, 3, 13), LocalDate.of(2013, 4, 13), LocalDate.of(2013, 4, 12), LocalDate.of(2014, 7, 2),
    LocalDate.of(2015, 6, 30) };
  private static final LocalDate[] SPREAD_CURVE_DATES = new LocalDate[] {LocalDate.of(2013, 2, 23), LocalDate.of(2013, 4, 13), LocalDate.of(2014, 2, 17), LocalDate.of(2017, 4, 30),
    LocalDate.of(2014, 7, 2), LocalDate.of(2015, 4, 30) };

  /**
   *
   */
  public void getIntegrationNodesAsDatesTest() {

    final LocalDate[] expected = new LocalDate[] {START_DATE, LocalDate.of(2013, 2, 23), LocalDate.of(2013, 3, 13), LocalDate.of(2013, 4, 12), LocalDate.of(2013, 4, 13), LocalDate.of(2014, 2, 17),
      LocalDate.of(2014, 7, 2), LocalDate.of(2015, 4, 30), END_DATE };
    final int n = expected.length;

    LocalDate[] res = IsdaCompliantScheduleGenerator.getIntegrationNodesAsDates(START_DATE, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
    assertEquals("", n, res.length);
    for (int i = 0; i < n; i++) {
      assertTrue(expected[i].equals(res[i]));
    }

    final LocalDate lateStartDate = LocalDate.of(2013, 3, 13);
    final LocalDate[] expectedLateStart = new LocalDate[] {lateStartDate, LocalDate.of(2013, 4, 12), LocalDate.of(2013, 4, 13), LocalDate.of(2014, 2, 17), LocalDate.of(2014, 7, 2),
      LocalDate.of(2015, 4, 30), END_DATE };
    final int nLateStart = expectedLateStart.length;
    res = IsdaCompliantScheduleGenerator.getIntegrationNodesAsDates(lateStartDate, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
    assertEquals(nLateStart, res.length);
    for (int i = 0; i < nLateStart; i++) {
      assertTrue(expectedLateStart[i].equals(res[i]));
    }

    final LocalDate lateEndDate = LocalDate.of(2018, 8, 30);
    final LocalDate[] discCurveDatesRe = new LocalDate[] {LocalDate.of(2013, 3, 13), LocalDate.of(2013, 4, 14), LocalDate.of(2013, 4, 12), LocalDate.of(2014, 7, 5), LocalDate.of(2015, 6, 30) };
    final LocalDate[] expectedLateEnd = new LocalDate[] {START_DATE, LocalDate.of(2013, 2, 23), LocalDate.of(2013, 3, 13), LocalDate.of(2013, 4, 12), LocalDate.of(2013, 4, 13),
      LocalDate.of(2013, 4, 14), LocalDate.of(2014, 2, 17), LocalDate.of(2014, 7, 2), LocalDate.of(2014, 7, 5), LocalDate.of(2015, 4, 30), LocalDate.of(2015, 6, 30), LocalDate.of(2017, 4, 30),
      lateEndDate };
    final int nLateEnd = expectedLateEnd.length;
    res = IsdaCompliantScheduleGenerator.getIntegrationNodesAsDates(START_DATE, lateEndDate, discCurveDatesRe, SPREAD_CURVE_DATES);
    assertEquals(nLateEnd, res.length);
    for (int i = 0; i < nLateEnd; i++) {
      assertTrue(expectedLateEnd[i].equals(res[i]));
    }
  }

  /**
   *
   */
  void nodesAsTimesTest() {
    final LocalDate today = LocalDate.of(2013, 1, 23);
    final LocalDate[] expectedDates = new LocalDate[] {START_DATE, LocalDate.of(2013, 2, 23), LocalDate.of(2013, 3, 13), LocalDate.of(2013, 4, 12), LocalDate.of(2013, 4, 13),
      LocalDate.of(2014, 2, 17), LocalDate.of(2014, 7, 2), LocalDate.of(2015, 4, 30), END_DATE };
    final int n = expectedDates.length;
    final double[] expected = new double[n];
    for (int i = 0; i < n; ++i) {
      expected[i] = ACT.yearFraction(today, expectedDates[i]);
    }

    final double[] res = IsdaCompliantScheduleGenerator.getIntegrationNodesAsTimes(today, START_DATE, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
    assertEquals(n, res.length);
    for (int i = 0; i < n; ++i) {
      assertEquals(expected[i], res[i]);
    }
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nodesAsTimesNullTodayTest() {
    final LocalDate today = null;
    IsdaCompliantScheduleGenerator.getIntegrationNodesAsTimes(today, START_DATE, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nodesAsTimesNullStartTest() {
    final LocalDate today = LocalDate.of(2013, 1, 23);
    final LocalDate start = null;
    IsdaCompliantScheduleGenerator.getIntegrationNodesAsTimes(today, start, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nodesAsTimeTodayAfterStartTest() {
    final LocalDate today = LocalDate.of(2013, 3, 23);
    IsdaCompliantScheduleGenerator.getIntegrationNodesAsTimes(today, START_DATE, END_DATE, DISCOUNT_CURVE_DATES, SPREAD_CURVE_DATES);
  }

  /**
   *
   */
  public void truncateListTest() {
    final LocalDate[] dateList = new LocalDate[] {LocalDate.of(1912, 2, 29), LocalDate.of(1013, 12, 13), LocalDate.of(2000, 2, 2), LocalDate.of(2014, 7, 2), LocalDate.of(2015, 12, 30) };
    final LocalDate startDate = LocalDate.of(1992, 2, 13);
    final LocalDate endDate = LocalDate.of(2015, 6, 30);

    LocalDate[] expected = new LocalDate[] {startDate, LocalDate.of(2000, 2, 2), LocalDate.of(2014, 7, 2), endDate };
    int n = expected.length;
    LocalDate[] res = IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, dateList);
    assertEquals(n, res.length);
    for (int i = 0; i < n; ++i) {
      assertTrue(expected[i].isEqual(res[i]));
    }

    final LocalDate[] emptyDateList = new LocalDate[] {};
    expected = new LocalDate[] {startDate, endDate };
    n = expected.length;
    res = IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, emptyDateList);
    assertEquals(n, res.length);
    for (int i = 0; i < n; ++i) {
      assertTrue(expected[i].isEqual(res[i]));
    }
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void truncateListNullStartTest() {
    final LocalDate[] dateList = new LocalDate[] {LocalDate.of(1912, 2, 29), LocalDate.of(1013, 12, 13), LocalDate.of(2000, 2, 2), LocalDate.of(2014, 7, 2), LocalDate.of(2015, 12, 30) };
    final LocalDate startDate = null;
    final LocalDate endDate = LocalDate.of(2015, 6, 30);
    IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, dateList);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void truncateListNullEndTest() {
    final LocalDate[] dateList = new LocalDate[] {LocalDate.of(1912, 2, 29), LocalDate.of(1013, 12, 13), LocalDate.of(2000, 2, 2), LocalDate.of(2014, 7, 2), LocalDate.of(2015, 12, 30) };
    final LocalDate startDate = LocalDate.of(1992, 2, 13);
    final LocalDate endDate = null;
    IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, dateList);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void truncateListNullListTest() {
    final LocalDate[] dateList = null;
    final LocalDate startDate = LocalDate.of(1992, 2, 13);
    final LocalDate endDate = LocalDate.of(2015, 6, 30);
    IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, dateList);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void truncateListStartAfterEndTest() {
    final LocalDate[] dateList = new LocalDate[] {LocalDate.of(1912, 2, 29), LocalDate.of(1013, 12, 13), LocalDate.of(2000, 2, 2), LocalDate.of(2014, 7, 2), LocalDate.of(2015, 12, 30) };
    final LocalDate startDate = LocalDate.of(2022, 2, 13);
    final LocalDate endDate = LocalDate.of(2015, 6, 30);
    IsdaCompliantScheduleGenerator.truncateList(startDate, endDate, dateList);
  }

}
