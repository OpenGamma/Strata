/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Tools for working with dates.
 * <p>
 * This package contains data objects and tools for manipulating dates.
 * <p>
 * {@link com.opengamma.strata.basics.date.DayCount DayCount} provides the standard
 * mechanism used to convert dates to fractions of a year.
 * <p>
 * {@link com.opengamma.strata.basics.date.HolidayCalendar HolidayCalendar} provides
 * data on whether a date is a holiday or business day.
 * <p>
 * {@link com.opengamma.strata.basics.date.BusinessDayConvention BusinessDayConvention}
 * provides standard rules for converting a holiday to the nearest business day,
 * such as 'Following', 'ModifiedFollowing' and 'Preceding'.
 * <p>
 * {@link com.opengamma.strata.basics.date.PeriodAdditionConvention PeriodAdditionConvention}
 * provides rules for adding months, such as 'LastDay' and 'LastBusinessDay'.
 * <p>
 * {@link com.opengamma.strata.basics.date.Tenor Tenor} is used to represent
 * the length of time that a financial instrument takes to reach maturity.
 * <p>
 * {@link com.opengamma.strata.basics.date.AdjustableDate AdjustableDate},
 * {@link com.opengamma.strata.basics.date.BusinessDayAdjustment BusinessDayAdjustment},
 * {@link com.opengamma.strata.basics.date.DaysAdjustment DaysAdjustment},
 * {@link com.opengamma.strata.basics.date.DaysAdjustment PeriodAdjustment} and
 * {@link com.opengamma.strata.basics.date.DaysAdjustment TenorAdjustment} provide
 * entity objects to represent different kinds of adjustment.
 */
package com.opengamma.strata.basics.date;
