/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;

import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.rate.DispatchingRateObservationFn;
import com.opengamma.strata.product.rate.RateObservation;

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
   * Returns a default instance of the function.
   * <p>
   * Use this method to avoid a direct dependency on the implementation.
   * 
   * @return the rate observation function
   */
  public static RateObservationFn<RateObservation> instance() {
    return DispatchingRateObservationFn.DEFAULT;
  }

  //-------------------------------------------------------------------------
  /**
   * Determines the applicable rate for the observation.
   * <p>
   * Each type of rate has specific rules, encapsulated in {@code RateObservation}.
   * <p>
   * The start date and end date refer to the accrual period.
   * In many cases, this information is not necessary, however it does enable some
   * implementations that would not otherwise be possible.
   * 
   * @param observation  the rate to be observed
   * @param startDate  the start date of the accrual period
   * @param endDate  the end date of the accrual period
   * @param provider  the rates provider
   * @return the applicable rate
   */
  public abstract double rate(T observation, LocalDate startDate, LocalDate endDate, RatesProvider provider);

  /**
   * Determines the point sensitivity for the rate observation.
   * <p>
   * This returns a sensitivity instance referring to the curves used to determine
   * each forward rate.
   * 
   * @param observation  the rate that the sensitivity is for
   * @param startDate  the start date of the accrual period
   * @param endDate  the end date of the accrual period
   * @param provider  the rates provider
   * @return the point sensitivity
   */
  public abstract PointSensitivityBuilder rateSensitivity(
      T observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider);

  /**
   * Explains the calculation of the applicable rate.
   * <p>
   * This adds information to the {@link ExplainMapBuilder} to aid understanding of the observation.
   * The actual rate is also returned.
   * 
   * @param observation  the rate to be observed
   * @param startDate  the start date of the accrual period
   * @param endDate  the end date of the accrual period
   * @param provider  the rates provider
   * @param builder  the builder to populate
   * @return the applicable rate
   */
  public abstract double explainRate(
      T observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder);

}
