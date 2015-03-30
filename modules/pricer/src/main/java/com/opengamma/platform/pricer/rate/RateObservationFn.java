/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.rate;

import java.time.LocalDate;

import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.finance.rate.RateObservation;

/**
 * Observes a rate from an index.
 * <p>
 * This function provides the ability to observe a rate defined by {@link RateObservation}.
 * The rate will be based on known historic data and forward curves.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of rate to be observed
 */
public interface RateObservationFn<T extends RateObservation> {

  /**
   * Determines the applicable rate for the observation.
   * <p>
   * Each type of rate has specific rules, encapsulated in {@code RateObservation}.
   * <p>
   * The start date and end date refer to the accrual period.
   * In many cases, this information is not necessary, however it does enable some
   * implementations that would not otherwise be possible.
   * 
   * @param env  the pricing environment
   * @param observation  the rate to be observed
   * @param startDate  the start date of the accrual period
   * @param endDate  the end date of the accrual period
   * @return the applicable rate
   */
  public abstract double rate(PricingEnvironment env, T observation, LocalDate startDate, LocalDate endDate);

  /**
   * Determines the point sensitivity for the rate observation.
   * <p>
   * This returns a sensitivity instance referring to the curves used to determine
   * each forward rate.
   * 
   * @param env  the pricing environment
   * @param observation  the rate that the sensitivity is for
   * @param startDate  the start date of the accrual period
   * @param endDate  the end date of the accrual period
   * @return the point sensitivity
   */
  public abstract PointSensitivityBuilder rateSensitivity(
      PricingEnvironment env,
      T observation,
      LocalDate startDate,
      LocalDate endDate);

}
