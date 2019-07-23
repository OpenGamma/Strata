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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link DateSequence}.
 */
public class DateSequenceTest {

  @Test
  public void test_QUARTERLY_IMM() {
    DateSequence test = DateSequences.QUARTERLY_IMM;
    assertThat(test.getName()).isEqualTo("Quarterly-IMM");
    assertThat(test.toString()).isEqualTo("Quarterly-IMM");
    assertThat(test.dateMatching(YearMonth.of(2013, 3))).isEqualTo(LocalDate.of(2013, 3, 20));
  }

  @Test
  public void test_QUARTERLY_IMM_of() {
    DateSequence test = DateSequence.of("Quarterly-IMM");
    assertThat(test).isEqualTo(DateSequences.QUARTERLY_IMM);
  }

  @Test
  public void test_QUARTERLY_10TH_of() {
    DateSequence test = DateSequence.of("Quarterly-10th");
    assertThat(test).isEqualTo(DateSequences.QUARTERLY_10TH);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_quarterlyImm() {
    return new Object[][] {
        {date(2013, 1, 1), date(2013, 3, 20), date(2013, 6, 19), date(2013, 9, 18)},
        {date(2013, 3, 20), date(2013, 6, 19), date(2013, 9, 18), date(2013, 12, 18)},
        {date(2013, 6, 19), date(2013, 9, 18), date(2013, 12, 18), date(2014, 3, 19)},
        {date(2013, 9, 18), date(2013, 12, 18), date(2014, 3, 19), date(2014, 6, 18)},
        {date(2013, 12, 18), date(2014, 3, 19), date(2014, 6, 18), date(2014, 9, 17)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_quarterlyImm")
  public void test_nextOrSameQuarterlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base.plusDays(1);
    while (!date.isAfter(immDate1)) {
      assertThat(DateSequences.QUARTERLY_IMM.nextOrSame(date)).isEqualTo(immDate1);
      assertThat(DateSequences.QUARTERLY_IMM.nthOrSame(date, 1)).isEqualTo(immDate1);
      assertThat(DateSequences.QUARTERLY_IMM.nthOrSame(date, 2)).isEqualTo(immDate2);
      assertThat(DateSequences.QUARTERLY_IMM.nthOrSame(date, 3)).isEqualTo(immDate3);
      date = date.plusDays(1);
    }
    assertThat(DateSequences.QUARTERLY_IMM.dateMatching(YearMonth.from(date))).isEqualTo(immDate1);
  }

  @ParameterizedTest
  @MethodSource("data_quarterlyImm")
  public void test_nextQuarterlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base;
    while (!date.isAfter(immDate1)) {
      if (date.equals(immDate1)) {
        assertThat(DateSequences.QUARTERLY_IMM.next(date)).isEqualTo(immDate2);
        assertThat(DateSequences.QUARTERLY_IMM.nth(date, 1)).isEqualTo(immDate2);
        assertThat(DateSequences.QUARTERLY_IMM.nth(date, 2)).isEqualTo(immDate3);
      } else {
        assertThat(DateSequences.QUARTERLY_IMM.next(date)).isEqualTo(immDate1);
        assertThat(DateSequences.QUARTERLY_IMM.nth(date, 1)).isEqualTo(immDate1);
        assertThat(DateSequences.QUARTERLY_IMM.nth(date, 2)).isEqualTo(immDate2);
        assertThat(DateSequences.QUARTERLY_IMM.nth(date, 3)).isEqualTo(immDate3);
      }
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_monthlyImm() {
    return new Object[][] {
        {date(2014, 12, 17), date(2015, 1, 21), date(2015, 2, 18), date(2015, 3, 18)},
        {date(2015, 1, 21), date(2015, 2, 18), date(2015, 3, 18), date(2015, 4, 15)},
        {date(2015, 2, 18), date(2015, 3, 18), date(2015, 4, 15), date(2015, 5, 20)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_monthlyImm")
  public void test_nextOrSameMonthlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base.plusDays(1);
    while (!date.isAfter(immDate1)) {
      assertThat(DateSequences.MONTHLY_IMM.nextOrSame(date)).isEqualTo(immDate1);
      assertThat(DateSequences.MONTHLY_IMM.nthOrSame(date, 1)).isEqualTo(immDate1);
      assertThat(DateSequences.MONTHLY_IMM.nthOrSame(date, 2)).isEqualTo(immDate2);
      assertThat(DateSequences.MONTHLY_IMM.nthOrSame(date, 3)).isEqualTo(immDate3);
      date = date.plusDays(1);
    }
    assertThat(DateSequences.MONTHLY_IMM.dateMatching(YearMonth.from(date))).isEqualTo(immDate1);
  }

  @ParameterizedTest
  @MethodSource("data_monthlyImm")
  public void test_nextMonthlyImm(LocalDate base, LocalDate immDate1, LocalDate immDate2, LocalDate immDate3) {
    LocalDate date = base;
    while (!date.isAfter(immDate1)) {
      if (date.equals(immDate1)) {
        assertThat(DateSequences.MONTHLY_IMM.next(date)).isEqualTo(immDate2);
        assertThat(DateSequences.MONTHLY_IMM.nth(date, 1)).isEqualTo(immDate2);
        assertThat(DateSequences.MONTHLY_IMM.nth(date, 2)).isEqualTo(immDate3);
      } else {
        assertThat(DateSequences.MONTHLY_IMM.next(date)).isEqualTo(immDate1);
        assertThat(DateSequences.MONTHLY_IMM.nth(date, 1)).isEqualTo(immDate1);
        assertThat(DateSequences.MONTHLY_IMM.nth(date, 2)).isEqualTo(immDate2);
        assertThat(DateSequences.MONTHLY_IMM.nth(date, 3)).isEqualTo(immDate3);
      }
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_quarterly10th() {
    return new Object[][] {
        {date(2013, 1, 1), date(2013, 3, 10), date(2013, 6, 10), date(2013, 9, 10)},
        {date(2013, 3, 20), date(2013, 6, 10), date(2013, 9, 10), date(2013, 12, 10)},
        {date(2013, 6, 19), date(2013, 9, 10), date(2013, 12, 10), date(2014, 3, 10)},
        {date(2013, 9, 18), date(2013, 12, 10), date(2014, 3, 10), date(2014, 6, 10)},
        {date(2013, 12, 18), date(2014, 3, 10), date(2014, 6, 10), date(2014, 9, 10)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_quarterly10th")
  public void test_nextOrSameQuarterly10th(LocalDate base, LocalDate expect1, LocalDate expect2, LocalDate expect3) {
    LocalDate date = base.plusDays(1);
    while (!date.isAfter(expect1)) {
      assertThat(DateSequences.QUARTERLY_10TH.nextOrSame(date)).isEqualTo(expect1);
      assertThat(DateSequences.QUARTERLY_10TH.nthOrSame(date, 1)).isEqualTo(expect1);
      assertThat(DateSequences.QUARTERLY_10TH.nthOrSame(date, 2)).isEqualTo(expect2);
      assertThat(DateSequences.QUARTERLY_10TH.nthOrSame(date, 3)).isEqualTo(expect3);
      date = date.plusDays(1);
    }
    assertThat(DateSequences.QUARTERLY_10TH.dateMatching(YearMonth.from(date))).isEqualTo(expect1);
  }

  @ParameterizedTest
  @MethodSource("data_quarterly10th")
  public void test_nextQuarterly10th(LocalDate base, LocalDate expect1, LocalDate expect2, LocalDate expect3) {
    LocalDate date = base;
    while (!date.isAfter(expect1)) {
      if (date.equals(expect1)) {
        assertThat(DateSequences.QUARTERLY_10TH.next(date)).isEqualTo(expect2);
        assertThat(DateSequences.QUARTERLY_10TH.nth(date, 1)).isEqualTo(expect2);
        assertThat(DateSequences.QUARTERLY_10TH.nth(date, 2)).isEqualTo(expect3);
      } else {
        assertThat(DateSequences.QUARTERLY_10TH.next(date)).isEqualTo(expect1);
        assertThat(DateSequences.QUARTERLY_10TH.nth(date, 1)).isEqualTo(expect1);
        assertThat(DateSequences.QUARTERLY_10TH.nth(date, 2)).isEqualTo(expect2);
        assertThat(DateSequences.QUARTERLY_10TH.nth(date, 3)).isEqualTo(expect3);
      }
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_dummy() {
    DummyDateSequence test = new DummyDateSequence();
    assertThat(test.next(date(2015, 10, 14))).isEqualTo(date(2015, 10, 15));
    assertThat(test.next(date(2015, 10, 15))).isEqualTo(date(2015, 10, 22));
    assertThat(test.next(date(2015, 10, 16))).isEqualTo(date(2015, 10, 22));

    assertThat(test.nextOrSame(date(2015, 10, 14))).isEqualTo(date(2015, 10, 15));
    assertThat(test.nextOrSame(date(2015, 10, 15))).isEqualTo(date(2015, 10, 15));
    assertThat(test.nextOrSame(date(2015, 10, 16))).isEqualTo(date(2015, 10, 22));

    assertThat(test.nth(date(2015, 10, 14), 1)).isEqualTo(date(2015, 10, 15));
    assertThat(test.nth(date(2015, 10, 15), 1)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nth(date(2015, 10, 16), 1)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nth(date(2015, 10, 14), 2)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nth(date(2015, 10, 15), 2)).isEqualTo(date(2015, 10, 29));
    assertThat(test.nth(date(2015, 10, 16), 2)).isEqualTo(date(2015, 10, 29));

    assertThat(test.nthOrSame(date(2015, 10, 14), 1)).isEqualTo(date(2015, 10, 15));
    assertThat(test.nthOrSame(date(2015, 10, 15), 1)).isEqualTo(date(2015, 10, 15));
    assertThat(test.nthOrSame(date(2015, 10, 16), 1)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nthOrSame(date(2015, 10, 14), 2)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nthOrSame(date(2015, 10, 15), 2)).isEqualTo(date(2015, 10, 22));
    assertThat(test.nthOrSame(date(2015, 10, 16), 2)).isEqualTo(date(2015, 10, 29));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_extendedEnum() {
    assertThat(DateSequence.extendedEnum().lookupAll().get("Quarterly-IMM")).isEqualTo(DateSequences.QUARTERLY_IMM);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(DateSequences.class);
    coverEnum(StandardDateSequences.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DateSequences.QUARTERLY_IMM);
    assertSerialization(DateSequences.MONTHLY_IMM);
  }

  @Test
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
