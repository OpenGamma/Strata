/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
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
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Standard CDS market conventions
 * <p>
 * See cdsmodel.com for details
 */
enum StandardCdsConventions implements CdsConvention {

  NORTH_AMERICAN_USD(
      "NorthAmericanUsd",
      Currency.USD,
      DayCounts.ACT_360,
      BusinessDayConventions.FOLLOWING,
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      HolidayCalendars.USNY,
      StubConvention.SHORT_INITIAL,
      1,
      3),
  EUROPEAN_GBP(
      "EuropeanGbp",
      Currency.GBP,
      DayCounts.ACT_360,
      BusinessDayConventions.FOLLOWING,
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      HolidayCalendars.GBLO,
      StubConvention.SHORT_INITIAL,
      1,
      3),
  EUROPEAN_CHF(
      "EuropeanChf",
      Currency.CHF,
      DayCounts.ACT_360,
      BusinessDayConventions.FOLLOWING,
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      HolidayCalendars.GBLO.combineWith(HolidayCalendars.CHZU),
      StubConvention.SHORT_INITIAL,
      1,
      3),
  EUROPEAN_USD(
      "EuropeanUsd",
      Currency.USD,
      DayCounts.ACT_360,
      BusinessDayConventions.FOLLOWING,
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      HolidayCalendars.GBLO.combineWith(HolidayCalendars.USNY),
      StubConvention.SHORT_INITIAL,
      1,
      3);

  /**
   * Standard European contracts
   * <p>
   * See cdsmodel.com for details
   */

  private final String name;
  private final Currency currency;
  private final DayCount dayCount;
  private final BusinessDayConvention dayConvention;
  private final Frequency paymentFrequency;
  private final RollConvention rollConvention;
  private final boolean payAccOnDefault;
  private final HolidayCalendar calendar;
  private final StubConvention stubConvention;
  private final int stepIn;
  private final int settleLag;

  //create
  StandardCdsConventions(
      String name,
      Currency currency,
      DayCount dayCount,
      BusinessDayConvention dayConvention,
      Frequency paymentFrequency,
      RollConvention rollConvention,
      boolean payAccOnDefault,
      HolidayCalendar calendar,
      StubConvention stubConvention,
      int stepIn,
      int settleLag) {

    this.name = name;
    this.currency = currency;
    this.dayCount = dayCount;
    this.dayConvention = dayConvention;
    this.paymentFrequency = paymentFrequency;
    this.rollConvention = rollConvention;
    this.payAccOnDefault = payAccOnDefault;
    this.calendar = calendar;
    this.stubConvention = stubConvention;
    this.stepIn = stepIn;
    this.settleLag = settleLag;
  }

  @Override
  public Currency getCurrency() {
    return currency;
  }

  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  @Override
  public BusinessDayConvention getDayConvention() {
    return dayConvention;
  }

  @Override
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  @Override
  public RollConvention getRollConvention() {
    return rollConvention;
  }

  @Override
  public boolean getPayAccOnDefault() {
    return payAccOnDefault;
  }

  @Override
  public HolidayCalendar getCalendar() {
    return calendar;
  }

  @Override
  public StubConvention getStubConvention() {
    return stubConvention;
  }

  @Override
  public int getStepIn() {
    return stepIn;
  }

  @Override
  public int getSettleLag() {
    return settleLag;
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
