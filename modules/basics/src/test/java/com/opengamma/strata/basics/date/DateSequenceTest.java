/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link DateSequence}.
 */
@Test
public class DateSequenceTest {

  public void test_QUARTERLY_IMM() {
    DateSequence test = DateSequences.QUARTERLY_IMM;
    assertEquals(test.getName(), "Quarterly-IMM");
    assertEquals(test.toString(), "Quarterly-IMM");
    assertEquals(test.dateMatching(YearMonth.of(2013, 3)), LocalDate.of(2013, 3, 20));
  }

  public void test_QUARTERLY_IMM_of() {
    DateSequence test = DateSequence.of("Quarterly-IMM");
    assertEquals(test, DateSequences.QUARTERLY_IMM);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "quarterlyImm")
  static Object[][] data_quarterlyImm() {
    return new Object[][] {
        {date(2013, 1, 1), date(2013, 3, 20), date(2013, 6, 19), date(2013, 9, 18)},
        {date(2013, 3, 20), date(2013, 6, 19), date(2013, 9, 18), date(2013, 12, 18)},
        {date(2013, 6, 19), date(2013, 9, 18), date(2013, 12, 18), date(2014, 3, 19)},
        {date(2013, 9, 18), date(2013, 12, 18), date(2014, 3, 19), date(2014, 6, 18)},
        {date(2013, 12, 18), date(2014, 3, 19), date(2014, 6, 18), date(2014, 9, 17)},
    };
  }

  @Test(dataProvider = "quarterlyImm")
  public void test_nextOrSameQuarterlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base.plusDays(1);
    while (!date.isAfter(immDate1)) {
      assertEquals(DateSequences.QUARTERLY_IMM.nextOrSame(date), immDate1);
      assertEquals(DateSequences.QUARTERLY_IMM.nthOrSame(date, 1), immDate1);
      assertEquals(DateSequences.QUARTERLY_IMM.nthOrSame(date, 2), immDate2);
      assertEquals(DateSequences.QUARTERLY_IMM.nthOrSame(date, 3), immDate3);
      date = date.plusDays(1);
    }
  }

  @Test(dataProvider = "quarterlyImm")
  public void test_nextQuarterlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base;
    while (!date.isAfter(immDate1)) {
      if (date.equals(immDate1)) {
        assertEquals(DateSequences.QUARTERLY_IMM.next(date), immDate2);
        assertEquals(DateSequences.QUARTERLY_IMM.nth(date, 1), immDate2);
        assertEquals(DateSequences.QUARTERLY_IMM.nth(date, 2), immDate3);
      } else {
        assertEquals(DateSequences.QUARTERLY_IMM.next(date), immDate1);
        assertEquals(DateSequences.QUARTERLY_IMM.nth(date, 1), immDate1);
        assertEquals(DateSequences.QUARTERLY_IMM.nth(date, 2), immDate2);
        assertEquals(DateSequences.QUARTERLY_IMM.nth(date, 3), immDate3);
      }
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "monthlyImm")
  static Object[][] data_monthlyImm() {
    return new Object[][] {
        {date(2014, 12, 17), date(2015, 1, 21), date(2015, 2, 18), date(2015, 3, 18)},
        {date(2015, 1, 21), date(2015, 2, 18), date(2015, 3, 18), date(2015, 4, 15)},
        {date(2015, 2, 18), date(2015, 3, 18), date(2015, 4, 15), date(2015, 5, 20)},
    };
  }

  @Test(dataProvider = "monthlyImm")
  public void test_nextOrSameMonthlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base.plusDays(1);
    while (!date.isAfter(immDate1)) {
      assertEquals(DateSequences.MONTHLY_IMM.nextOrSame(date), immDate1);
      assertEquals(DateSequences.MONTHLY_IMM.nthOrSame(date, 1), immDate1);
      assertEquals(DateSequences.MONTHLY_IMM.nthOrSame(date, 2), immDate2);
      assertEquals(DateSequences.MONTHLY_IMM.nthOrSame(date, 3), immDate3);
      date = date.plusDays(1);
    }
  }

  @Test(dataProvider = "monthlyImm")
  public void test_nextMonthlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base;
    while (!date.isAfter(immDate1)) {
      if (date.equals(immDate1)) {
        assertEquals(DateSequences.MONTHLY_IMM.next(date), immDate2);
        assertEquals(DateSequences.MONTHLY_IMM.nth(date, 1), immDate2);
        assertEquals(DateSequences.MONTHLY_IMM.nth(date, 2), immDate3);
      } else {
        assertEquals(DateSequences.MONTHLY_IMM.next(date), immDate1);
        assertEquals(DateSequences.MONTHLY_IMM.nth(date, 1), immDate1);
        assertEquals(DateSequences.MONTHLY_IMM.nth(date, 2), immDate2);
        assertEquals(DateSequences.MONTHLY_IMM.nth(date, 3), immDate3);
      }
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  public void test_dummy() {
    DummyDateSequence test = new DummyDateSequence();
    assertEquals(test.next(date(2015, 10, 14)), date(2015, 10, 15));
    assertEquals(test.next(date(2015, 10, 15)), date(2015, 10, 22));
    assertEquals(test.next(date(2015, 10, 16)), date(2015, 10, 22));

    assertEquals(test.nextOrSame(date(2015, 10, 14)), date(2015, 10, 15));
    assertEquals(test.nextOrSame(date(2015, 10, 15)), date(2015, 10, 15));
    assertEquals(test.nextOrSame(date(2015, 10, 16)), date(2015, 10, 22));

    assertEquals(test.nth(date(2015, 10, 14), 1), date(2015, 10, 15));
    assertEquals(test.nth(date(2015, 10, 15), 1), date(2015, 10, 22));
    assertEquals(test.nth(date(2015, 10, 16), 1), date(2015, 10, 22));
    assertEquals(test.nth(date(2015, 10, 14), 2), date(2015, 10, 22));
    assertEquals(test.nth(date(2015, 10, 15), 2), date(2015, 10, 29));
    assertEquals(test.nth(date(2015, 10, 16), 2), date(2015, 10, 29));

    assertEquals(test.nthOrSame(date(2015, 10, 14), 1), date(2015, 10, 15));
    assertEquals(test.nthOrSame(date(2015, 10, 15), 1), date(2015, 10, 15));
    assertEquals(test.nthOrSame(date(2015, 10, 16), 1), date(2015, 10, 22));
    assertEquals(test.nthOrSame(date(2015, 10, 14), 2), date(2015, 10, 22));
    assertEquals(test.nthOrSame(date(2015, 10, 15), 2), date(2015, 10, 22));
    assertEquals(test.nthOrSame(date(2015, 10, 16), 2), date(2015, 10, 29));
  }

  //-------------------------------------------------------------------------
  public void test_extendedEnum() {
    assertEquals(DateSequence.extendedEnum().lookupAll().get("Quarterly-IMM"), DateSequences.QUARTERLY_IMM);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DateSequences.class);
    coverEnum(StandardDateSequences.class);
  }

  public void test_serialization() {
    assertSerialization(DateSequences.QUARTERLY_IMM);
    assertSerialization(DateSequences.MONTHLY_IMM);
  }

  public void test_jodaConvert() {
    assertJodaConvert(DateSequence.class, DateSequences.QUARTERLY_IMM);
    assertJodaConvert(DateSequence.class, DateSequences.MONTHLY_IMM);
  }

  //-------------------------------------------------------------------------
  static class DummyDateSequence implements DateSequence {
    @Override
    public LocalDate nextOrSame(LocalDate date) {
      if (date.isBefore(date(2015, 10, 16))) {
        return date(2015, 10, 15);
      }
      if (date.isBefore(date(2015, 10, 23))) {
        return date(2015, 10, 22);
      }
      if (date.isBefore(date(2015, 10, 30))) {
        return date(2015, 10, 29);
      }
      throw new IllegalArgumentException();
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return date(2015, 10, 29);
    }

    @Override
    public String getName() {
      return "Dummy";
    }

  }

}
