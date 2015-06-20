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
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
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
public interface IsdaYieldCurveConvention extends Named {

  @FromString
  static IsdaYieldCurveConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  static ExtendedEnum<IsdaYieldCurveConvention> extendedEnum() {
    return IsdaYieldCurveConventions.ENUM_LOOKUP;
  }

  Currency getCurrency();

  DayCount getMmDayCount();

  DayCount getFixedDayCount();

  int getSpotDays();

  Frequency getFixedPaymentFrequency();

  BusinessDayConvention getBadDayConvention();

  HolidayCalendar getHolidayCalendar();

  /**
   * Apply the spot days settlement lag and adjust using the conventions
   *
   * @param asOfDate base asOfDate
   * @return adjusted spot date
   */
  default LocalDate getSpotDateAsOf(LocalDate asOfDate) {
    BusinessDayAdjustment adjustment = BusinessDayAdjustment.of(
        getBadDayConvention(),
        getHolidayCalendar()
    );
    return adjustment.adjust(asOfDate.plusDays(getSpotDays()));
  }

  @ToString
  @Override
  String getName();
}
