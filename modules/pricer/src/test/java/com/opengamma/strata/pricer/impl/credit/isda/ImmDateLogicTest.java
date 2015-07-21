/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getIMMDateSet;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.isIMMDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ImmDateLogicTest {

  private static final Period[] TENORS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(5), Period.ofYears(10) };

  @Test
  void isIMMTest() {
    LocalDate date = LocalDate.of(2013, Month.MARCH, 20);
    assertTrue(isIMMDate(date));

    date = LocalDate.of(2013, Month.APRIL, 20);
    assertFalse(isIMMDate(date));

    date = LocalDate.of(2013, Month.DECEMBER, 23);
    assertFalse(isIMMDate(date));
  }

  @Test
  public void onIMMDateTest() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 20);
    final LocalDate nextIMM = getNextIMMDate(today);
    final LocalDate[] dates = getIMMDateSet(nextIMM, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), dates[0]);
  }

  @Test
  public void immSetTest() {
    final LocalDate date = LocalDate.of(2013, Month.MARCH, 20);
    final int n = 5;
    final LocalDate[] immDates = getIMMDateSet(date, n);
    assertTrue(immDates.length == n);
    assertEquals(date, immDates[0]);
    assertEquals(LocalDate.of(2013, Month.JUNE, 20), immDates[1]);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), immDates[2]);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), immDates[3]);
    assertEquals(LocalDate.of(2014, Month.MARCH, 20), immDates[4]);
  }

  @Test
  public void nonIMMDateTest() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 26);
    final LocalDate nextIMM = getNextIMMDate(today);
    final LocalDate[] dates = getIMMDateSet(nextIMM, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), dates[0]);
  }

  @Test
  public void IMMDateM1Test() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 19);
    final LocalDate nextIMM = getNextIMMDate(today);
    final LocalDate[] dates = getIMMDateSet(nextIMM, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), dates[0]);
  }

  @Test
  public void stepinIMMDateM2Test() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 18);
    final LocalDate nextIMM = getNextIMMDate(today);
    final LocalDate[] dates = getIMMDateSet(nextIMM, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), dates[0]);
  }

  @Test
  public void nextIMMTest() {
    LocalDate today = LocalDate.of(2011, Month.JUNE, 21);
    LocalDate nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.SEPTEMBER, 20), nextIMM);

    today = LocalDate.of(2011, Month.JUNE, 20);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.SEPTEMBER, 20), nextIMM);

    today = LocalDate.of(2011, Month.DECEMBER, 20);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2012, Month.MARCH, 20), nextIMM);

    today = LocalDate.of(2011, Month.JUNE, 18);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.JUNE, 20), nextIMM);

    today = LocalDate.of(1976, Month.JULY, 30);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(1976, Month.SEPTEMBER, 20), nextIMM);

    today = LocalDate.of(1977, Month.FEBRUARY, 13);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(1977, Month.MARCH, 20), nextIMM);

    today = LocalDate.of(2013, Month.MARCH, 1);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2013, Month.MARCH, 20), nextIMM);

    today = LocalDate.of(2013, Month.DECEMBER, 25);
    nextIMM = getNextIMMDate(today);
    assertEquals(LocalDate.of(2014, Month.MARCH, 20), nextIMM);

  }

  @Test
  public void prevIMMTest() {
    LocalDate today = LocalDate.of(2011, Month.JUNE, 21);
    LocalDate prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.JUNE, 20), prevIMM);

    today = LocalDate.of(2011, Month.JUNE, 20);
    prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.MARCH, 20), prevIMM);

    prevIMM = getPrevIMMDate(prevIMM);
    assertEquals(LocalDate.of(2010, Month.DECEMBER, 20), prevIMM);

    today = LocalDate.of(2011, Month.JUNE, 18);
    prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(2011, Month.MARCH, 20), prevIMM);

    today = LocalDate.of(1976, Month.JULY, 30);
    prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(1976, Month.JUNE, 20), prevIMM);

    today = LocalDate.of(1977, Month.FEBRUARY, 13);
    prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(1976, Month.DECEMBER, 20), prevIMM);

    today = LocalDate.of(2013, Month.MARCH, 1);
    prevIMM = getPrevIMMDate(today);
    assertEquals(LocalDate.of(2012, Month.DECEMBER, 20), prevIMM);

  }

  /**
   * 
   */
  @Test
  public void isIndexRollDateTest() {
    LocalDate date0 = LocalDate.of(2013, 3, 14);
    LocalDate date1 = LocalDate.of(2013, 6, 20);
    LocalDate date2 = LocalDate.of(2013, 3, 20);
    LocalDate date3 = LocalDate.of(2013, 9, 20);

    assertFalse(ImmDateLogic.isIndexRollDate(date0));
    assertFalse(ImmDateLogic.isIndexRollDate(date1));
    assertTrue(ImmDateLogic.isIndexRollDate(date2));
    assertTrue(ImmDateLogic.isIndexRollDate(date3));
  }

  /**
   * 
   */
  @Test
  public void getNextIndexRollDateTest() {
    final LocalDate[] dates = new LocalDate[] {LocalDate.of(2013, 3, 14), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2013, 1, 21),
      LocalDate.of(2013, 3, 21), LocalDate.of(2013, 9, 19), LocalDate.of(2013, 9, 21), LocalDate.of(2013, 11, 21) };

    final LocalDate[] datesExp = new LocalDate[] {LocalDate.of(2013, 3, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2013, 3, 20),
      LocalDate.of(2013, 9, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2014, 3, 20) };

    for (int i = 0; i < dates.length; ++i) {
      assertEquals(ImmDateLogic.getNextIndexRollDate(dates[i]), datesExp[i]);
    }
  }
}
