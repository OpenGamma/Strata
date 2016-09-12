/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.product.credit.CdsDatesLogic.getCdsDateSet;
import static com.opengamma.strata.product.credit.CdsDatesLogic.getNextCdsDate;
import static com.opengamma.strata.product.credit.CdsDatesLogic.getNextIndexRollDate;
import static com.opengamma.strata.product.credit.CdsDatesLogic.getPreviousCdsDate;
import static com.opengamma.strata.product.credit.CdsDatesLogic.isCdsDate;
import static com.opengamma.strata.product.credit.CdsDatesLogic.isIndexRollDate;
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
public class CdsDatesLogicTest {

  private static final Period[] TENORS = new Period[] {
      Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2),
      Period.ofYears(3), Period.ofYears(5), Period.ofYears(10)};

  public void isCdsTest() {
    LocalDate date = LocalDate.of(2013, Month.MARCH, 20);
    assertTrue(isCdsDate(date));

    date = LocalDate.of(2013, Month.APRIL, 20);
    assertFalse(isCdsDate(date));

    date = LocalDate.of(2013, Month.DECEMBER, 23);
    assertFalse(isCdsDate(date));
  }

  public void onCdsDateTest() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 20);
    final LocalDate nextCds = getNextCdsDate(today);
    final LocalDate[] dates = getCdsDateSet(nextCds, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), dates[0]);
  }

  public void cdsSetTest() {
    final LocalDate date = LocalDate.of(2013, Month.MARCH, 20);
    final int n = 5;
    final LocalDate[] cdsDates = getCdsDateSet(date, n);
    assertTrue(cdsDates.length == n);
    assertEquals(date, cdsDates[0]);
    assertEquals(LocalDate.of(2013, Month.JUNE, 20), cdsDates[1]);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), cdsDates[2]);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), cdsDates[3]);
    assertEquals(LocalDate.of(2014, Month.MARCH, 20), cdsDates[4]);
  }

  public void nonCdsDateTest() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 26);
    final LocalDate nextCds = getNextCdsDate(today);
    final LocalDate[] dates = getCdsDateSet(nextCds, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.DECEMBER, 20), dates[0]);
  }

  public void cdsDateM1Test() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 19);
    final LocalDate nextCds = getNextCdsDate(today);
    final LocalDate[] dates = getCdsDateSet(nextCds, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), dates[0]);
  }

  public void stepinCdsDateM2Test() {
    final LocalDate today = LocalDate.of(2013, Month.MARCH, 18);
    final LocalDate nextCds = getNextCdsDate(today);
    final LocalDate[] dates = getCdsDateSet(nextCds, TENORS);
    assertEquals(dates.length, TENORS.length);
    assertEquals(LocalDate.of(2013, Month.SEPTEMBER, 20), dates[0]);
  }

  public void nextCdsTest() {
    LocalDate today = LocalDate.of(2011, Month.JUNE, 21);
    LocalDate nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.SEPTEMBER, 20), nextCds);

    today = LocalDate.of(2011, Month.JUNE, 20);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.SEPTEMBER, 20), nextCds);

    today = LocalDate.of(2011, Month.DECEMBER, 20);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2012, Month.MARCH, 20), nextCds);

    today = LocalDate.of(2011, Month.JUNE, 18);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.JUNE, 20), nextCds);

    today = LocalDate.of(1976, Month.JULY, 30);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(1976, Month.SEPTEMBER, 20), nextCds);

    today = LocalDate.of(1977, Month.FEBRUARY, 13);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(1977, Month.MARCH, 20), nextCds);

    today = LocalDate.of(2013, Month.MARCH, 1);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2013, Month.MARCH, 20), nextCds);

    today = LocalDate.of(2013, Month.DECEMBER, 25);
    nextCds = getNextCdsDate(today);
    assertEquals(LocalDate.of(2014, Month.MARCH, 20), nextCds);
  }

  public void prevCdsTest() {
    LocalDate today = LocalDate.of(2011, Month.JUNE, 21);
    LocalDate prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.JUNE, 20), prevCds);

    today = LocalDate.of(2011, Month.JUNE, 20);
    prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.MARCH, 20), prevCds);

    prevCds = getPreviousCdsDate(prevCds);
    assertEquals(LocalDate.of(2010, Month.DECEMBER, 20), prevCds);

    today = LocalDate.of(2011, Month.JUNE, 18);
    prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(2011, Month.MARCH, 20), prevCds);

    today = LocalDate.of(1976, Month.JULY, 30);
    prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(1976, Month.JUNE, 20), prevCds);

    today = LocalDate.of(1977, Month.FEBRUARY, 13);
    prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(1976, Month.DECEMBER, 20), prevCds);

    today = LocalDate.of(2013, Month.MARCH, 1);
    prevCds = getPreviousCdsDate(today);
    assertEquals(LocalDate.of(2012, Month.DECEMBER, 20), prevCds);
  }

  public void isIndexRollDateTest() {
    LocalDate date0 = LocalDate.of(2013, 3, 14);
    LocalDate date1 = LocalDate.of(2013, 6, 20);
    LocalDate date2 = LocalDate.of(2013, 3, 20);
    LocalDate date3 = LocalDate.of(2013, 9, 20);

    assertFalse(isIndexRollDate(date0));
    assertFalse(isIndexRollDate(date1));
    assertTrue(isIndexRollDate(date2));
    assertTrue(isIndexRollDate(date3));
  }

  public void getNextIndexRollDateTest() {
    final LocalDate[] dates = new LocalDate[] {LocalDate.of(2013, 3, 14), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 3, 20),
        LocalDate.of(2013, 9, 20), LocalDate.of(2013, 1, 21),
        LocalDate.of(2013, 3, 21), LocalDate.of(2013, 9, 19), LocalDate.of(2013, 9, 21), LocalDate.of(2013, 11, 21)};

    final LocalDate[] datesExp = new LocalDate[] {LocalDate.of(2013, 3, 20), LocalDate.of(2013, 9, 20),
        LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2013, 3, 20),
        LocalDate.of(2013, 9, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2014, 3, 20)};

    for (int i = 0; i < dates.length; ++i) {
      assertEquals(getNextIndexRollDate(dates[i]), datesExp[i]);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CdsDatesLogic.class);
  }

}
