/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

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
import com.opengamma.strata.finance.rate.swap.type.FixedIborSwapConvention;

/**
 * CDS Standard model definition for parameters required to bootstrap an ISDA yield curve
 * <p>
 * The ISDA conventions related to CDS Standard model deviate from the standard market
 * conventions {@link FixedIborSwapConvention}.
 * <p>
 * For instance, holiday calendars are generally ignored when building these curves (JPY is
 * an exception) and only the fixed leg conventions are used (the floating leg details are
 * ignored). Additionally, these conventions apply not just to the underlying swap instruments
 * but also the money market instruments used to build the curve. This is why there is both
 * a mmDayCount and a fixedDayCount.
 */
public interface IsdaYieldCurveConvention
    extends Named {
  // TODO: merge business day convention and holiday calendar

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
   * Gets the currency that the yield curve can be used to discount.
   * 
   * @return the currency
   */
  Currency getCurrency();

  /**
   * Gets the day count convention for underlying money market instrument points on the curve.
   * 
   * @return the day count convention
   */
  DayCount getMmDayCount();

  /**
   * Gets the fixed leg day count convention for underlying swap instrument points on the curve.
   * 
   * @return the fixed day count convention
   */
  DayCount getFixedDayCount();

  /**
   * Gets the spot day settlement lag for any underlying swap instruments.
   * 
   * @return the number of spot days
   */
  int getSpotDays();

  /**
   * Gets the payment periodic frequency for the fixed leg of any underlying swap instruments.
   * 
   * @return the frequency
   */
  Frequency getFixedPaymentFrequency();

  /**
   * Gets the applicable business day convention for any underlying instruments.
   * 
   * @return the business day convention
   */
  BusinessDayConvention getBadDayConvention();

  /**
   * Gets the applicable holiday calendar for any instruments.
   * 
   * @return the holiday calendar
   */
  HolidayCalendar getHolidayCalendar();

  //-------------------------------------------------------------------------
  /**
   * Apply the spot days settlement lag and adjust using the conventions
   *
   * @param asOfDate  the base date to adjust
   * @return the adjusted spot date
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
