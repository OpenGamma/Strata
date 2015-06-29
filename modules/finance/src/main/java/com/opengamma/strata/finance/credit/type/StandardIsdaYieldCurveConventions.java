/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Standard CDS yield curve conventions
 * <p>
 * See cdsmodel.com and Markit website for details
 */
enum StandardIsdaYieldCurveConventions
    implements IsdaYieldCurveConvention {

  /**
   * The ISDA USD curve.
   */
  ISDA_USD(
      "ISDA_USD",
      Currency.USD,
      DayCounts.ACT_360,
      DayCounts.THIRTY_E_360,
      2,
      Frequency.P6M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.SAT_SUN),
  /**
   * The ISDA EUR curve.
   */
  ISDA_EUR(
      "ISDA_EUR",
      Currency.EUR,
      DayCounts.ACT_360,
      DayCounts.THIRTY_E_360,
      2,
      Frequency.P12M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.SAT_SUN),
  /**
   * The ISDA GBP curve.
   */
  ISDA_GBP(
      "ISDA_GBP",
      Currency.GBP,
      DayCounts.ACT_365F,
      DayCounts.ACT_365F,
      2,
      Frequency.P6M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.SAT_SUN),
  /**
   * The ISDA CHF curve.
   */
  ISDA_CHF(
      "ISDA_CHF",
      Currency.CHF,
      DayCounts.ACT_360,
      DayCounts.THIRTY_360_ISDA,
      2,
      Frequency.P12M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.SAT_SUN),
  /**
   * The ISDA JPY curve.
   */
  ISDA_JPY(
      "ISDA_JPY",
      Currency.JPY,
      DayCounts.ACT_360,
      DayCounts.THIRTY_E_360,
      2,
      Frequency.P6M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.JPTO);

  private final String name;
  private final Currency currency;
  private final DayCount mmDayCount;
  private final DayCount fixedDayCount;
  private final int spotDays;
  private final Frequency fixedPaymentFrequency;
  private final BusinessDayConvention badDayConvention;
  private final HolidayCalendar holidayCalendar;

  // create
  StandardIsdaYieldCurveConventions(String name,
      Currency currency,
      DayCount mmDayCount,
      DayCount fixedDayCount,
      int spotDays,
      Frequency fixedPaymentFrequency,
      BusinessDayConvention badDayConvention,
      HolidayCalendar holidayCalendar) {

    this.name = name;
    this.currency = currency;
    this.mmDayCount = mmDayCount;
    this.fixedDayCount = fixedDayCount;
    this.spotDays = spotDays;
    this.fixedPaymentFrequency = fixedPaymentFrequency;
    this.badDayConvention = badDayConvention;
    this.holidayCalendar = holidayCalendar;
  }

  @Override
  public Currency getCurrency() {
    return currency;
  }

  @Override
  public DayCount getMmDayCount() {
    return mmDayCount;
  }

  @Override
  public DayCount getFixedDayCount() {
    return fixedDayCount;
  }

  @Override
  public int getSpotDays() {
    return spotDays;
  }

  @Override
  public Frequency getFixedPaymentFrequency() {
    return fixedPaymentFrequency;
  }

  @Override
  public BusinessDayConvention getBadDayConvention() {
    return badDayConvention;
  }

  @Override
  public HolidayCalendar getHolidayCalendar() {
    return holidayCalendar;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
