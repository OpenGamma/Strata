/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.rate;

import java.time.LocalDate;

import com.opengamma.platform.finance.rate.Rate;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Provides a rate for a {@code Rate}.
 * <p>
 * The rate will be based on known historic data and forward curves.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of rate to be provided
 */
public interface RateProviderFn<T extends Rate> {

  /**
   * Determines the applicable rate for the specified period.
   * <p>
   * Each type of rate has specific rules, encapsulated in {@code Rate}.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param rate  the rate to provide
   * @param startDate  the start date of the period that the rate is applicable for
   * @param endDate  the end date of the period that the rate is applicable for
   * @return the applicable rate
   */
  public abstract double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      T rate,
      LocalDate startDate,
      LocalDate endDate);

}
