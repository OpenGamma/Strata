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

enum StandardIsdaYieldCurveConventions implements IsdaYieldCurveConvention {

  NORTH_AMERICAN_USD(
      "NorthAmericanUsd",
      Currency.USD,
      DayCounts.ACT_360,
      DayCounts.THIRTY_E_360,
      2,
      Frequency.P6M,
      BusinessDayConventions.MODIFIED_FOLLOWING,
      HolidayCalendars.SAT_SUN);

  /*
    public static final IsdaYieldCurveConvention europeanEur =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.EUR)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_360_ISDA)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P12M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention europeanGbp =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.GBP)
          .mmDayCount(DayCounts.ACT_365F)
          .fixedDayCount(DayCounts.ACT_365F)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P6M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention europeanChf =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.CHF)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_360_ISDA)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P12M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.SAT_SUN)
          .build();

  public static final IsdaYieldCurveConvention asianJPY =
      IsdaYieldCurveConvention
          .builder()
          .currency(Currency.JPY)
          .mmDayCount(DayCounts.ACT_360)
          .fixedDayCount(DayCounts.THIRTY_E_360)
          .spotDays(2)
          .fixedPaymentFrequency(Frequency.P6M)
          .badDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
          .holidayCalendar(HolidayCalendars.JPTO)
          .build();
   */

  // name
  private final String name;

  Currency currency;

  DayCount mmDayCount;

  DayCount fixedDayCount;

  int spotDays;

  Frequency fixedPaymentFrequency;

  BusinessDayConvention badDayConvention;

  HolidayCalendar holidayCalendar;

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
