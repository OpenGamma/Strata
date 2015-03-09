/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.observation.IborRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;

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
  public Pair<Double, MulticurveSensitivity3LD> rateMulticurveSensitivity3LD(
      PricingEnvironment env,
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    LocalDate fixingDate = observation.getFixingDate();
    IborIndex index = observation.getIndex();
    // historic rate
    if (!fixingDate.isAfter(env.getValuationDate())) {
      OptionalDouble fixedRate = env.timeSeries(index).get(fixingDate);
      if (fixedRate.isPresent()) {
        return Pair.of(fixedRate.getAsDouble(), new MulticurveSensitivity3LD());
      } else if (fixingDate.isBefore(env.getValuationDate())) { // the fixing is required
        throw new OpenGammaRuntimeException("Could not get fixing value for date " + fixingDate);
      }
    }
    // forward rate
    double forwardRate = env.iborIndexRate(observation.getIndex(), observation.getFixingDate());
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(index, fixingDate, 1.0d, index.getCurrency()));
    return Pair.of(forwardRate, MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
  }

}
