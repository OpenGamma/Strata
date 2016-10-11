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
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
* Rate computation implementation for an Ibor index.
* <p>
* The implementation simply returns the rate from the {@code RatesProvider}.
*/
public class ForwardIborRateComputationFn
    implements RateComputationFn<IborRateComputation> {

  /**
   * Default implementation.
   */
  public static final ForwardIborRateComputationFn DEFAULT = new ForwardIborRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      IborRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
    return rates.rate(computation.getObservation());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
    return rates.ratePointSensitivity(computation.getObservation());
  }

  @Override
  public double explainRate(
      IborRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
    double rate = rates.explainRate(computation.getObservation(), builder, child -> {});
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
