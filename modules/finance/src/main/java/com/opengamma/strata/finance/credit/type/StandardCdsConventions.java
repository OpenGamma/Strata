/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Enumerates the standard CDS market conventions.
 * <p>
 * See cdsmodel.com for details
 */
public enum StandardCdsConventions implements CdsConvention {

  /**
   * The North-American USD convention.
   */
  NORTH_AMERICAN_USD(
      "NorthAmericanUsd",
      Currency.USD,
      DayCounts.ACT_360,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING,
          HolidayCalendars.USNY),
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      StubConvention.SHORT_INITIAL,
      1,
      3),

  /**
   * The European GBP convention.
   */
  EUROPEAN_GBP(
      "EuropeanGbp",
      Currency.GBP,
      DayCounts.ACT_360,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING,
          HolidayCalendars.GBLO),
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      StubConvention.SHORT_INITIAL,
      1,
      3),

  /**
   * The European CHF convention.
   */
  EUROPEAN_CHF(
      "EuropeanChf",
      Currency.CHF,
      DayCounts.ACT_360,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING,
          HolidayCalendars.GBLO.combineWith(HolidayCalendars.CHZU)),
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      StubConvention.SHORT_INITIAL,
      1,
      3),

  /**
   * The European USD convention.
   */
  EUROPEAN_USD(
      "EuropeanUsd",
      Currency.USD,
      DayCounts.ACT_360,
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING,
          HolidayCalendars.GBLO.combineWith(HolidayCalendars.USNY)),
      Frequency.P3M,
      RollConventions.DAY_20,
      true,
      StubConvention.SHORT_INITIAL,
      1,
      3);

  //-------------------------------------------------------------------------
  private final String name;
  private final Currency currency;
  private final DayCount dayCount;
  private final BusinessDayAdjustment businessDayAdjustment;
  private final Frequency paymentFrequency;
  private final RollConvention rollConvention;
  private final boolean payAccruedOnDefault;
  private final StubConvention stubConvention;
  private final int stepIn;
  private final int settleLag;

  // creates an instance
  private StandardCdsConventions(
      String name,
      Currency currency,
      DayCount dayCount,
      BusinessDayAdjustment businessDayAdjustment,
      Frequency paymentFrequency,
      RollConvention rollConvention,
      boolean payAccruedOnDefault,
      StubConvention stubConvention,
      int stepIn,
      int settleLag) {

    this.name = name;
    this.currency = currency;
    this.dayCount = dayCount;
    this.businessDayAdjustment = businessDayAdjustment;
    this.paymentFrequency = paymentFrequency;
    this.rollConvention = rollConvention;
    this.payAccruedOnDefault = payAccruedOnDefault;
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
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
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
  public boolean getPayAccruedOnDefault() {
    return payAccruedOnDefault;
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
