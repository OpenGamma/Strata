/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class IsdaCompliantScheduleGenerator {

  private static final DayCount ACT365 = DayCounts.ACT_365F;

  /**
   * This mimics JpmcdsRiskyTimeLine from the ISDA model in c.
   * 
   * @param startDate start date
   * @param endDate end date
   * @param disCurveDates all the points in the discount curve
   * @param spreadCurveDates all the points in the risky curve
   * @return An ascending array of dates that is the unique combination of all the input dates
   *  (including startDate and endDate) that are not strictly
   *  before startDate or after EndDate (hence startDate will be the first entry and enddate the last). 
   */
  public static LocalDate[] getIntegrationNodesAsDates(
      LocalDate startDate,
      LocalDate endDate,
      LocalDate[] disCurveDates,
      LocalDate[] spreadCurveDates) {

    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.notNull(endDate, "null endDate");
    ArgChecker.noNulls(disCurveDates, "nulls in disCurveDates");
    ArgChecker.noNulls(spreadCurveDates, "nulls in spreadCurveDates");

    ArgChecker.isTrue(
        endDate.isAfter(startDate), "endDate of {} is not after startDate of {}", endDate.toString(), startDate.toString());

    int nDisCurvePoints = disCurveDates.length;
    int nSpreadCurvePoints = spreadCurveDates.length;

    LinkedHashSet<LocalDate> set = new LinkedHashSet<>(2 + nDisCurvePoints + nSpreadCurvePoints);
    set.add(startDate);
    for (LocalDate date : disCurveDates) {
      set.add(date);
    }
    for (LocalDate date : spreadCurveDates) {
      set.add(date);
    }
    set.add(endDate);

    int n = set.size();
    LocalDate[] res = new LocalDate[n];
    set.toArray(res);
    Arrays.sort(res);

    // remove dates strictly before startDate and strictly after endDate
    int a = 0;
    int b = n - 1;
    while (res[a].isBefore(startDate)) {
      a++;
    }
    while (res[b].isAfter(endDate)) {
      b--;
    }
    int newLength = b - a + 1;
    if (newLength == n) {
      return res; // nothing got chopped off
    }

    LocalDate[] res2 = new LocalDate[newLength];
    System.arraycopy(res, a, res2, 0, newLength);
    return res2;
  }

  /**
   * This calls getIntegrationNodesAsDates to get an array of dates then calculates the year fraction
   * from today to those points using ACT/365.
   * 
   * @param today the date to measure year-fractions from. Must NOT have today after startDate 
   * @param startDate start date
   * @param endDate end date
   * @param disCurveDates all the points in the discount curve
   * @param spreadCurveDates all the points in the risky curve
   * @return An ascending array of times from today @see getIntegrationNodesAsDates
   */
  public static double[] getIntegrationNodesAsTimes(
      LocalDate today,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate[] disCurveDates,
      LocalDate[] spreadCurveDates) {

    ArgChecker.notNull(today, "null today");
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.isFalse(today.isAfter(startDate), "today is after startDate");

    LocalDate[] dates = getIntegrationNodesAsDates(startDate, endDate, disCurveDates, spreadCurveDates);
    return getYearFractionFromToday(today, dates);
  }

  /**
   * Truncate an sort (ascending) array of dates so the the interior values are strictly after startDate
   * and strictly before endEnd, and startDate and endDate becomes to first and last entries.
   * 
   * @param startDate This will be the first value in the list 
   * @param endDate This will be the last value in the list
   * @param dateList Must be sorted 
   * @return dates between startDate and endDate
   */
  public static LocalDate[] truncateList(LocalDate startDate, LocalDate endDate, LocalDate[] dateList) {
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.notNull(endDate, "null endDate");
    ArgChecker.noNulls(dateList, "nulls in dateList");
    ArgChecker.isTrue(endDate.isAfter(startDate), "require enddate after startDate");
    int n = dateList.length;
    if (n == 0) {
      return new LocalDate[] {startDate, endDate};
    }

    List<LocalDate> temp = new ArrayList<>(n + 2);
    for (LocalDate d : dateList) {
      if (d.isAfter(startDate) && d.isBefore(endDate)) {
        temp.add(d);
      }
    }

    int m = temp.size();
    LocalDate[] tArray = new LocalDate[m];
    temp.toArray(tArray);
    LocalDate[] res = new LocalDate[m + 2];
    res[0] = startDate;
    System.arraycopy(tArray, 0, res, 1, m);
    res[m + 1] = endDate;
    return res;
  }

  /**
   * Year fractions from a fixed date to a set of dates using ACT/365.
   * 
   * @param today the date to measure from 
   * @param dates set of dates to measure to 
   * @return set of year fractions (array of double)
   */
  public static double[] getYearFractionFromToday(LocalDate today, LocalDate[] dates) {
    return getYearFractionFromToday(today, dates, ACT365);
  }

  /**
   * Year fractions from a fixed date to a set of dates using the specified day-count.
   * 
   * @param today  the date to measure from 
   * @param dates  the set of dates to measure to 
   * @param dayCount  the day-count
   * @return set of year fractions (array of double)
   */
  public static double[] getYearFractionFromToday(LocalDate today, LocalDate[] dates, DayCount dayCount) {
    ArgChecker.notNull(today, "null today");
    ArgChecker.noNulls(dates, "nulls in dates");
    ArgChecker.notNull(dayCount, "null dayCount");

    int n = dates.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = dayCount.yearFraction(today, dates[i]);
    }

    return res;
  }

}
