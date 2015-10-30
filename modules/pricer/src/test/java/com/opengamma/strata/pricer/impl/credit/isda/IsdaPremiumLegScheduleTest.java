/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Test.
 */
@Test
public class IsdaPremiumLegScheduleTest {

  private static final DayCount ACT360 = DayCounts.ACT_360;
  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;

  // TODO all the null input tests. startDate after endDate etc

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void undefinedStubTest() {
    final LocalDate startDate = LocalDate.of(2012, 6, 7);
    final LocalDate endDate = LocalDate.of(2015, 11, 29); // sunday
    final Period step = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.NONE;
    final boolean protectionStart = false;
    @SuppressWarnings("unused")
    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, step, stubType, FOLLOWING, CALENDAR, protectionStart);
  }

  /**
   * short front stub and end on a weekend at EoM 
   */
  public void scheduleTest1() {
    final LocalDate[] accStart = new LocalDate[] {LocalDate.of(2012, 6, 7), LocalDate.of(2012, 8, 29), LocalDate.of(2012, 11, 29), LocalDate.of(2013, 2, 28), LocalDate.of(2013, 5, 29),
      LocalDate.of(2013, 8, 29), LocalDate.of(2013, 11, 29), LocalDate.of(2014, 2, 28), LocalDate.of(2014, 5, 29), LocalDate.of(2014, 8, 29), LocalDate.of(2014, 12, 1), LocalDate.of(2015, 3, 2),
      LocalDate.of(2015, 5, 29), LocalDate.of(2015, 8, 31) };
    final LocalDate[] accEnd = new LocalDate[] {LocalDate.of(2012, 8, 29), LocalDate.of(2012, 11, 29), LocalDate.of(2013, 2, 28), LocalDate.of(2013, 5, 29), LocalDate.of(2013, 8, 29),
      LocalDate.of(2013, 11, 29), LocalDate.of(2014, 2, 28), LocalDate.of(2014, 5, 29), LocalDate.of(2014, 8, 29), LocalDate.of(2014, 12, 1), LocalDate.of(2015, 3, 2), LocalDate.of(2015, 5, 29),
      LocalDate.of(2015, 8, 31), LocalDate.of(2015, 11, 30) };
    final LocalDate[] pay = new LocalDate[] {LocalDate.of(2012, 8, 29), LocalDate.of(2012, 11, 29), LocalDate.of(2013, 2, 28), LocalDate.of(2013, 5, 29), LocalDate.of(2013, 8, 29),
      LocalDate.of(2013, 11, 29), LocalDate.of(2014, 2, 28), LocalDate.of(2014, 5, 29), LocalDate.of(2014, 8, 29), LocalDate.of(2014, 12, 1), LocalDate.of(2015, 3, 2), LocalDate.of(2015, 5, 29),
      LocalDate.of(2015, 8, 31), LocalDate.of(2015, 11, 30) };
    final int n = pay.length;
    // data check
    ArgChecker.isTrue(n == accStart.length, null);
    ArgChecker.isTrue(n == accEnd.length, null);

    final LocalDate startDate = LocalDate.of(2012, 6, 7);
    final LocalDate endDate = LocalDate.of(2015, 11, 29); // sunday
    final Period step = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;

    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, step, stubType, FOLLOWING, CALENDAR, protectionStart);
    assertEquals(n, schedule.getNumPayments());
    for (int i = 0; i < n; i++) {
      assertEquals(accStart[i], schedule.getAccStartDate(i));
      assertEquals(accEnd[i], schedule.getAccEndDate(i));
      assertEquals(pay[i], schedule.getPaymentDate(i));
    }

  }

  /**
   * Long front stub, start on weekend and end on IMM date 
   */
  public void scheduleTest2() {
    final LocalDate[] accStart = new LocalDate[] {LocalDate.of(2012, 6, 30), LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20) };
    final LocalDate[] accEnd = new LocalDate[] {LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2013, 12, 21) };
    final LocalDate[] pay = new LocalDate[] {LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2013, 12, 20) };
    final int n = pay.length;
    // data check
    ArgChecker.isTrue(n == accStart.length, null);
    ArgChecker.isTrue(n == accEnd.length, null);

    final LocalDate startDate = LocalDate.of(2012, 6, 30); // Saturday
    final LocalDate endDate = LocalDate.of(2013, 12, 20); // IMM date
    final Period step = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.LONG_INITIAL;
    final boolean protectionStart = true;

    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, step, stubType, FOLLOWING, CALENDAR, protectionStart);
    assertEquals(n, schedule.getNumPayments());
    for (int i = 0; i < n; i++) {
      assertEquals(accStart[i], schedule.getAccStartDate(i));
      assertEquals(accEnd[i], schedule.getAccEndDate(i));
      assertEquals(pay[i], schedule.getPaymentDate(i));
    }
  }

  /**
   * short back stub, start and end on IMM date 
   */
  public void scheduleTest3() {
    final LocalDate[] accStart = new LocalDate[] {LocalDate.of(2012, 6, 20), LocalDate.of(2012, 9, 20), LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20) };
    final LocalDate[] accEnd = new LocalDate[] {LocalDate.of(2012, 9, 20), LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 21) };
    final LocalDate[] pay = new LocalDate[] {LocalDate.of(2012, 9, 20), LocalDate.of(2012, 12, 20), LocalDate.of(2013, 3, 20), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20) };
    final int n = pay.length;
    // data check
    ArgChecker.isTrue(n == accStart.length, null);
    ArgChecker.isTrue(n == accEnd.length, null);

    final LocalDate startDate = LocalDate.of(2012, 6, 20); // IMM date
    final LocalDate endDate = LocalDate.of(2013, 9, 20); // IMM date
    final Period step = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.SHORT_FINAL;
    final boolean protectionStart = true;

    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, step, stubType, FOLLOWING, CALENDAR, protectionStart);
    assertEquals(n, schedule.getNumPayments());
    for (int i = 0; i < n; i++) {
      assertEquals(accStart[i], schedule.getAccStartDate(i));
      assertEquals(accEnd[i], schedule.getAccEndDate(i));
      assertEquals(pay[i], schedule.getPaymentDate(i));
    }
  }

  /**
   * long back stub, start and end NOT on IMM date 
   */
  public void scheduleTest4() {
    final LocalDate[] accStart = new LocalDate[] {LocalDate.of(2012, 5, 10), LocalDate.of(2012, 8, 10), LocalDate.of(2012, 11, 12), LocalDate.of(2013, 2, 11), LocalDate.of(2013, 5, 10) };
    final LocalDate[] accEnd = new LocalDate[] {LocalDate.of(2012, 8, 10), LocalDate.of(2012, 11, 12), LocalDate.of(2013, 2, 11), LocalDate.of(2013, 5, 10), LocalDate.of(2013, 10, 21) };
    final LocalDate[] pay = new LocalDate[] {LocalDate.of(2012, 8, 10), LocalDate.of(2012, 11, 12), LocalDate.of(2013, 2, 11), LocalDate.of(2013, 5, 10), LocalDate.of(2013, 10, 21) };
    final int n = pay.length;
    // data check
    ArgChecker.isTrue(n == accStart.length, null);
    ArgChecker.isTrue(n == accEnd.length, null);

    final LocalDate startDate = LocalDate.of(2012, 5, 10);
    final LocalDate endDate = LocalDate.of(2013, 10, 20);
    final Period step = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.LONG_FINAL;
    final boolean protectionStart = true;

    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, step, stubType, FOLLOWING, CALENDAR, protectionStart);
    assertEquals(n, schedule.getNumPayments());
    for (int i = 0; i < n; i++) {
      assertEquals(accStart[i], schedule.getAccStartDate(i));
      assertEquals(accEnd[i], schedule.getAccEndDate(i));
      assertEquals(pay[i], schedule.getPaymentDate(i));
    }
  }

  // TODO other inputs: Period different from 3M, convention "proceeding", proctectionStart = false etc.

  /**
   * This generates a table in the CDS pricing paper 
   */
  @Test(enabled = false)
  public void printTest() {
    final LocalDate tradeDate = LocalDate.of(2013, Month.JULY, 30);
    final Period tenor = Period.ofYears(2);
    final LocalDate stepIn = tradeDate.plusDays(1);
    final LocalDate startDate = getPrevIMMDate(stepIn);
    final LocalDate endDate = getNextIMMDate(tradeDate).plus(tenor);
    final Period paymentInt = Period.ofMonths(3);
    final StubConvention stub = StubConvention.SHORT_INITIAL;
    final double notional = 1e7;
    final double coupon = 1e-2;

    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, paymentInt, stub, FOLLOWING, CALENDAR, true);

    final DateTimeFormatter formatt = DateTimeFormatter.ofPattern("dd-MMM-yy");

    final int n = schedule.getNumPayments();

    System.out.println("\\begin{tabular}{|c|c|c|c|c|}");
    System.out.println("\\hline");
    System.out.println("Accrual Start & Accrual End & Payment Date & Days in & Amount" + " \\\\");
    System.out.println("(Inclusive) & (Exclusive) &  & Period & " + " \\\\");
    System.out.println("\\hline");
    for (int i = 0; i < n; i++) {
      final LocalDate start = schedule.getAccStartDate(i);
      final LocalDate end = schedule.getAccEndDate(i);
      System.out.print(start.format(formatt) + " & ");
      System.out.print(end.format(formatt) + " & ");
      System.out.print(schedule.getPaymentDate(i).format(formatt) + " & ");

      final long firstJulianDate = start.getLong(JulianFields.MODIFIED_JULIAN_DAY);
      final long secondJulianDate = end.getLong(JulianFields.MODIFIED_JULIAN_DAY);
      final int days = (int) (secondJulianDate - firstJulianDate);
      System.out.print(days + " & ");
      final double premium = notional * coupon * ACT360.yearFraction(start, end);
      System.out.format("%.2f" + " \\\\" + "\n", premium);
    }
    System.out.println("\\hline");
    System.out.println("\\end{tabular}");
  }
}
