/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import java.time.LocalDate;

/**
 * CDS Standard model definition for parameters required to bootstrap an ISDA yield curve
 */
public interface IsdaYieldCurveConvention
    extends Named {
  // TODO: better docs
  // TODO: merge business day convention and holiday calendar
  // TODO: Rename MmDayCount

  /**
   * Looks up the convention corresponding to a given name.
   * 
   * @param uniqueName  the unique name of the convention
   * @return the resolved convention
   */
  @FromString
  static IsdaYieldCurveConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum lookup from name to instance.
   * 
   * @return the extended enum lookup
   */
  static ExtendedEnum<IsdaYieldCurveConvention> extendedEnum() {
    return IsdaYieldCurveConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * 
   * @return the currency
   */
  Currency getCurrency();

  /**
   * Gets the day count convention.
   * 
   * @return the day count convention
   */
  DayCount getMmDayCount();

  /**
   * Gets the fixed day count convention.
   * 
   * @return the fixed day count convention
   */
  DayCount getFixedDayCount();

  /**
   * Gets the spot day settlement lag.
   * 
   * @return the number of spot days
   */
  int getSpotDays();

  /**
   * Gets the payment periodic frequency.
   * 
   * @return the frequency
   */
  Frequency getFixedPaymentFrequency();

  /**
   * Gets the applicable business day convention.
   * 
   * @return the business day convention
   */
  BusinessDayConvention getBadDayConvention();

  /**
   * Gets the applicable holiday calendar.
   * 
   * @return the holiday calendar
   */
  HolidayCalendar getHolidayCalendar();

  //-------------------------------------------------------------------------
  /**
   * Apply the spot days settlement lag and adjust using the conventions
   *
   * @param asOfDate base asOfDate
   * @return adjusted spot date
   */
  default LocalDate getSpotDateAsOf(LocalDate asOfDate) {
    DaysAdjustment adjustment = DaysAdjustment.ofBusinessDays(
        getSpotDays(),
        getHolidayCalendar(),
        BusinessDayAdjustment.of(getBadDayConvention(), getHolidayCalendar()));

    return adjustment.adjust(asOfDate);
  }

  @ToString
  @Override
  String getName();

}
