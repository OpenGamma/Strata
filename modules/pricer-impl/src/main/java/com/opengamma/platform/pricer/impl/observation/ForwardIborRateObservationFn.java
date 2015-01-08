/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
* Rate observation implementation for an IBOR-like index.
* <p>
* The rate observation simply return the Ibor rate from the PricingEnvironment.
*/
public class ForwardIborRateObservationFn 
    implements RateObservationFn<IborRateObservation> {
  
  /**
   * Default instance.
   */
  public static final ForwardIborRateObservationFn DEFAULT = new ForwardIborRateObservationFn();

  @Override
  public double rate(
      PricingEnvironment env, 
      IborRateObservation observation, 
      LocalDate startDate, 
      LocalDate endDate) {
    return env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
  }

}
