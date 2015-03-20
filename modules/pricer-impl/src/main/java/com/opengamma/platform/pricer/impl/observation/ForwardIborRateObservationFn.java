/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.platform.finance.rate.IborRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;

/**
* Rate observation implementation for an IBOR-like index.
* <p>
* The implementation simply returns the rate from the {@code PricingEnvironment}.
*/
public class ForwardIborRateObservationFn
    implements RateObservationFn<IborRateObservation> {

  /**
   * Default implementation.
   */
  public static final ForwardIborRateObservationFn DEFAULT = new ForwardIborRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return env.iborIndexRateSensitivity(observation.getIndex(), observation.getFixingDate());
  }

}
