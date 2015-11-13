/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborRateObservation;

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

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());
    return rates.rate(observation.getFixingDate());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());
    return rates.ratePointSensitivity(observation.getFixingDate());
  }

  @Override
  public double explainRate(
      IborRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rate = rate(observation, startDate, endDate, provider);
    LocalDate fixingDate = observation.getFixingDate();
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "IborIndexObservation")
        .put(ExplainKey.FIXING_DATE, fixingDate)
        .put(ExplainKey.INDEX, observation.getIndex())
        .put(ExplainKey.INDEX_VALUE, rate));
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
