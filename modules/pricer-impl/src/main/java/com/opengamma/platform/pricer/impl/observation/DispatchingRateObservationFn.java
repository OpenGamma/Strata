/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.observation.FixedRateObservation;
import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
 * Rate observation implementation using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingRateObservationFn
    implements RateObservationFn<RateObservation> {

  /**
   * Default implementation.
   */
  public static final DispatchingRateObservationFn DEFAULT = new DispatchingRateObservationFn(
      ForwardIborRateObservationFn.DEFAULT);

  /**
   * Rate provider for {@link IborRateObservation}.
   */
  private final RateObservationFn<IborRateObservation> iborRateObservationFn;

  /**
   * Creates an instance.
   *
   * @param iborRateObservationFn  the rate provider for {@link IborRateObservation}
   */
  public DispatchingRateObservationFn(
      RateObservationFn<IborRateObservation> iborRateObservationFn) {
    this.iborRateObservationFn = ArgChecker.notNull(iborRateObservationFn, "iborRateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      RateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    // dispatch by runtime type
    if (observation instanceof FixedRateObservation) {
      // inline code (performance) avoiding need for FixedRateObservationFn implementation
      return ((FixedRateObservation) observation).getRate();
    } else if (observation instanceof IborRateObservation) {
      return iborRateObservationFn.rate(env, (IborRateObservation) observation, startDate, endDate);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + observation.getClass().getSimpleName());
    }
  }

}
