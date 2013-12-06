/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.Period;

import com.opengamma.util.time.LocalDateRange;

/**
 * Translates a {@link Period} into a {@link LocalDateRange} which ends at the current valuation date.
 * The valuation date is included in the range.
 */
public interface DateRangeCalculator {

  /**
   * Returns a date range of length {@code period} ending at the valuation date.
   * @param period A period
   * @return A date range of length {@code period} ending at the valuation date.
   */
  LocalDateRange calculateDateRange(Period period);
}
