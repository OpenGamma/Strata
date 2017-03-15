/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * A convention defining how to adjust a date if it falls on a day other than a business day.
 * <p>
 * The purpose of this convention is to define how to handle non-business days.
 * When processing dates in finance, it is typically intended that non-business days,
 * such as weekends and holidays, are converted to a nearby valid business day.
 * The convention, in conjunction with a {@linkplain HolidayCalendar holiday calendar},
 * defines exactly how the adjustment should be made.
 * <p>
 * The most common implementations are provided in {@link BusinessDayConventions}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface BusinessDayConvention
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the business convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static BusinessDayConvention of(String uniqueName) {
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<BusinessDayConvention> extendedEnum() {
    return BusinessDayConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date as necessary if it is not a business day.
   * <p>
   * If the date is a business day it will be returned unaltered.
   * If the date is not a business day, the convention will be applied.
   * 
   * @param date  the date to adjust
   * @param calendar  the calendar that defines holidays and business days
   * @return the adjusted date
   */
  public abstract LocalDate adjust(LocalDate date, HolidayCalendar calendar);

  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
