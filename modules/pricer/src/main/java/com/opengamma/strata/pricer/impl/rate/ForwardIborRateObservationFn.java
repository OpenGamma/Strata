/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
* Rate observation implementation for an IBOR-like index.
* <p>
* The implementation simply returns the rate from the {@code RatesProvider}.
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
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    return provider.iborIndexRate(observation.getIndex(), observation.getFixingDate());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());
    return rates.pointSensitivity(observation.getFixingDate());
  }

}
