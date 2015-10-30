/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * For a CDS with set set of payments on the fixed leg, this holds the payments dates and the accrual start and end dates. It does not
 * hold the payment amounts with depends on the day-count (normally ACT/360) and the spread.
 */
public class IsdaPremiumLegSchedule {

  private final int _nPayments;
  private final LocalDate[] _accStartDates;
  private final LocalDate[] _accEndDates;
  private final LocalDate[] _paymentDates;
  private final LocalDate[] _nominalPaymentDates;

  /**
   * This mimics JpmcdsDateListMakeRegular. Produces a set of ascending dates by following the rules:<p>
   * If the stub is at the front end, we role backwards from the endDate at an integer multiple of the specified step size (e.g. 3M),
   * adding these date until we pass the startDate(this date is not added). If the stub type is short, the startDate is added (as the first date), hence the first period
   * will be less than (or equal to) the remaining periods. If the stub type is long, the startDate is also added, but the date immediately
   * after that is removed, so the first period is longer than the remaining.<p>
   * If the stub is at the back end, we role forward from the startDate at an integer multiple of the specified step size (e.g. 3M),
   * adding these date until we pass the endDate(this date is not added). If the stub type is short, the endDate is added (as the last date), hence the last period
   * will be less than (or equal to) the other periods. If the stub type is long, the endDate is also added, but the date immediately
   * before that is removed, so the last period is longer than the others.
   *
   * @param startDate The start date - this will be the first entry in the list
   * @param endDate The end date - this will be the last entry in the list
   * @param step the step period (e.g. 3M - will produce dates every 3 months, with adjustments at the beginning or end based on stub type)
   * @param stubType the stub convention
   * @return an array of LocalDate
   */
  public static LocalDate[] getUnadjustedDates(LocalDate startDate, LocalDate endDate, Period step, StubConvention stubType) {
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.notNull(endDate, "null endDate");
    ArgChecker.notNull(step, "step");
    ArgChecker.notNull(stubType, "null stubType");
    ArgChecker.isFalse(endDate.isBefore(startDate), "end date is before startDate");

    if (startDate.isEqual(endDate)) { // this can only happen if protectionStart == true
      LocalDate[] tempDates = new LocalDate[2];
      tempDates[0] = startDate;
      tempDates[1] = endDate;
      return tempDates;
    }

    ArgChecker.isFalse(stubType == StubConvention.NONE, "NONE is not allowed as a stub convention");
    ArgChecker.isFalse(stubType == StubConvention.BOTH, "BOTH is not allowed as a stub convention");

    long firstJulianDate = startDate.getLong(JulianFields.MODIFIED_JULIAN_DAY);
    long secondJulianDate = endDate.getLong(JulianFields.MODIFIED_JULIAN_DAY);
    double days = step.getDays() + 365.0 * (step.getMonths() / 12. + step.getYears());
    int nApprox = 3 + (int) ((secondJulianDate - firstJulianDate) / days);

    List<LocalDate> dates = new ArrayList<>(nApprox);

    // stub at front end, so start at endDate and work backwards
    if (stubType.isCalculateBackwards()) {
      int intervals = 0;
      LocalDate tDate = endDate;
      while (tDate.isAfter(startDate)) {
        dates.add(tDate);
        Period tStep = step.multipliedBy(++intervals); // this mimics ISDA c code, rather than true market convention
        tDate = endDate.minus(tStep);
      }

      int n = dates.size();
      if (tDate.isEqual(startDate) || n == 1 || stubType == StubConvention.SHORT_INITIAL) {
        dates.add(startDate);
      } else {
        // long front stub - remove the last date entry in the list and replace it with startDate
        dates.remove(n - 1);
        dates.add(startDate);
      }

      int m = dates.size();
      LocalDate[] res = new LocalDate[m];
      // want to output in ascending chronological order, so need to reverse the list
      int j = m - 1;
      for (int i = 0; i < m; i++, j--) {
        res[j] = dates.get(i);
      }
      return res;

      // stub at back end, so start at startDate and work forward
    } else {
      int intervals = 0;
      LocalDate tDate = startDate;
      while (tDate.isBefore(endDate)) {
        dates.add(tDate);
        Period tStep = step.multipliedBy(++intervals); // this mimics ISDA c code, rather than true market convention
        tDate = startDate.plus(tStep);
      }

      int n = dates.size();
      if (tDate.isEqual(endDate) || n == 1 || stubType == StubConvention.SHORT_FINAL) {
        dates.add(endDate);
      } else {
        // long back stub - remove the last date entry in the list and replace it with endDate
        dates.remove(n - 1);
        dates.add(endDate);
      }
      LocalDate[] res = new LocalDate[dates.size()];
      return dates.toArray(res);
    }

  }

  public static IsdaPremiumLegSchedule truncateSchedule(LocalDate stepin, IsdaPremiumLegSchedule schedule) {
    return schedule.truncateSchedule(stepin);
  }

  /**
   * Remove all payment intervals before the given date 
   * @param stepin a date 
   * @return truncate schedule
   */
  public IsdaPremiumLegSchedule truncateSchedule(LocalDate stepin) {
    if (!_accStartDates[0].isBefore(stepin)) {
      return this; // nothing to truncate
    }

    int index = getAccStartDateIndex(stepin);
    if (index < 0) {
      index = -(index + 1) - 1; // keep the one before the insertion point
    }

    return truncateSchedule(index);
  }

  /**
   * makes a new ISDAPremiumLegSchedule with payment before index removed 
   * @param index the index of the old schedule that will be the zero index of the new
   * @return truncate schedule
   */
  public IsdaPremiumLegSchedule truncateSchedule(int index) {
    return new IsdaPremiumLegSchedule(_nominalPaymentDates, _paymentDates, _accStartDates, _accEndDates, index);
  }

  /**
   * Truncation constructor
   * @param paymentDates
   * @param accStartDates
   * @param accEndDates
   * @param index copy the date starting from this index
   */
  private IsdaPremiumLegSchedule(
      LocalDate[] nominalPaymentDates,
      LocalDate[] paymentDates,
      LocalDate[] accStartDates,
      LocalDate[] accEndDates,
      int index) {

    ArgChecker.noNulls(nominalPaymentDates, "unadjustedDates");
    ArgChecker.noNulls(paymentDates, "paymentDates");
    ArgChecker.noNulls(accStartDates, "accStartDates");
    ArgChecker.noNulls(accEndDates, "accEndDates");

    int n = paymentDates.length;
    _nPayments = n - index;
    ArgChecker.isTrue(
        n == nominalPaymentDates.length,
        "nominalPaymentDates length of {} does not match paymentDates length of {}", nominalPaymentDates.length, _nPayments);
    ArgChecker.isTrue(
        n == accStartDates.length,
        "accStartDates length of {} does not match paymentDates length of {}", accStartDates.length, _nPayments);
    ArgChecker.isTrue(
        n == accEndDates.length,
        "accEndDates length of {} does not match paymentDates length of {}", accEndDates.length, _nPayments);

    _nominalPaymentDates = new LocalDate[_nPayments];
    _paymentDates = new LocalDate[_nPayments];
    _accStartDates = new LocalDate[_nPayments];
    _accEndDates = new LocalDate[_nPayments];
    System.arraycopy(nominalPaymentDates, index, _nominalPaymentDates, 0, _nPayments);
    System.arraycopy(paymentDates, index, _paymentDates, 0, _nPayments);
    System.arraycopy(accStartDates, index, _accStartDates, 0, _nPayments);
    System.arraycopy(accEndDates, index, _accEndDates, 0, _nPayments);
  }

  /**
   * Mimics JpmcdsCdsFeeLegMake
   * @param startDate The protection start date
   * @param endDate The protection end date
   * @param step The period or frequency at which payments are made (e.g. every three months)
   * @param stubType The stub convention
   * @param businessdayAdjustmentConvention options are 'following' or 'proceeding'
   * @param calandar A holiday calendar
   * @param protectionStart If true, protection starts are the beginning rather than end of day (protection still ends at end of day).
   */
  public IsdaPremiumLegSchedule(
      LocalDate startDate,
      LocalDate endDate,
      Period step,
      StubConvention stubType,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calandar,
      boolean protectionStart) {

    this(getUnadjustedDates(startDate, endDate, step, stubType), businessdayAdjustmentConvention, calandar, protectionStart);
  }

  public IsdaPremiumLegSchedule(
      LocalDate[] unadjustedDates,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar,
      boolean protectionStart) {

    _nPayments = unadjustedDates.length - 1;
    _nominalPaymentDates = new LocalDate[_nPayments];
    _paymentDates = new LocalDate[_nPayments];
    _accStartDates = new LocalDate[_nPayments];
    _accEndDates = new LocalDate[_nPayments];

    LocalDate dPrev = unadjustedDates[0];
    LocalDate dPrevAdj = dPrev; // first date is never adjusted
    for (int i = 0; i < _nPayments; i++) {
      LocalDate dNext = unadjustedDates[i + 1];
      LocalDate dNextAdj = businessDayAdjustDate(dNext, calendar, businessdayAdjustmentConvention);
      _accStartDates[i] = dPrevAdj;
      _accEndDates[i] = dNextAdj;
      _nominalPaymentDates[i] = dNext;
      _paymentDates[i] = dNextAdj;
      dPrev = dNext;
      dPrevAdj = dNextAdj;
    }

    // the last accrual date is not adjusted for business-day 
    _accEndDates[_nPayments - 1] = getFinalAccEndDate(unadjustedDates[_nPayments], protectionStart);
  }

  public static LocalDate getFinalAccEndDate(LocalDate unadjustedDate, boolean protectionStart) {
    ArgChecker.notNull(unadjustedDate, "unadjustedDate");
    if (protectionStart) {
      return unadjustedDate.plusDays(1); // extra day of accrued interest
    } else {
      return unadjustedDate;
    }
  }

  public int getNumPayments() {
    return _nPayments;
  }

  public LocalDate getAccStartDate(int index) {
    return _accStartDates[index];
  }

  public LocalDate getAccEndDate(int index) {
    return _accEndDates[index];
  }

  public LocalDate getPaymentDate(int index) {
    return _paymentDates[index];
  }

  public LocalDate getNominalPaymentDate(int index) {
    return _nominalPaymentDates[index];
  }

  /**
   * finds the index in accStartDate that matches the given date, or if date is not a member of accStartDate returns (-insertionPoint -1)
   * @see Arrays#binarySearch
   * @param date The date to find
   * @return index or code giving insertion point
   */
  public int getAccStartDateIndex(LocalDate date) {
    return Arrays.binarySearch(_accStartDates, date, null);
  }

  /**
   * finds the index in paymentDate that matches the given date, or if date is not a member of paymentDate returns (-insertionPoint -1)
   * @see Arrays#binarySearch
   * @param date The date to find
   * @return index or code giving insertion point
   */
  public int getPaymentDateIndex(LocalDate date) {
    return Arrays.binarySearch(_paymentDates, date, null);
  }

  public int getNominalPaymentDateIndex(LocalDate date) {
    return Arrays.binarySearch(_nominalPaymentDates, date, null);
  }

  /**
   * The accrual start date, end date and payment date at the given index
   * @param index the index (from zero)
   * @return array of LocalDate
   */
  public LocalDate[] getAccPaymentDateTriplet(int index) {
    return new LocalDate[] {_accStartDates[index], _accEndDates[index], _paymentDates[index]};
  }

  private LocalDate businessDayAdjustDate(LocalDate date, HolidayCalendar calendar, BusinessDayConvention convention) {

    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(calendar, "HolidayCalendar");
    ArgChecker.notNull(convention, "Business day adjustment");

    return convention.adjust(date, calendar);
  }

}
