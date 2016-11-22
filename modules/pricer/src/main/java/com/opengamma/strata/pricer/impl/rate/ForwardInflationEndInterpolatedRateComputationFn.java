/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;

/**
 * Rate computation implementation for rate based on the weighted average of fixings 
 * of a single price index.
 * <p>
 * The rate computed by this instance is based on fixed start index value
 * and two observations relative to the end date  of the period.
 * The start index is given by {@code InflationEndInterpolatedRateComputation}.
 * The end index is the weighted average of the index values associated with the two reference dates.
 * Then the pay-off for a unit notional is {@code IndexEnd / IndexStart}. 
 */
public class ForwardInflationEndInterpolatedRateComputationFn
    implements RateComputationFn<InflationEndInterpolatedRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationEndInterpolatedRateComputationFn DEFAULT =
      new ForwardInflationEndInterpolatedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationEndInterpolatedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationEndInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double indexStart = computation.getStartIndexValue();
    double indexEnd = interpolateEnd(computation, values);
    return indexEnd / indexStart - 1d;
  }

  // interpolate the computations at the end
  private double interpolateEnd(InflationEndInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    double indexValue1 = values.value(computation.getEndObservation());
    double indexValue2 = values.value(computation.getEndSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationEndInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    PointSensitivityBuilder sensi = endSensitivity(computation, values);
    return sensi.multipliedBy(1d / computation.getStartIndexValue());
  }

  // interpolate the observations at the end
  private PointSensitivityBuilder endSensitivity(InflationEndInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(computation.getEndObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(computation.getEndSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationEndInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double w1 = computation.getWeight();
    double w2 = 1d - w1;
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getEndObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, computation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(computation.getEndObservation()))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getEndSecondObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, computation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(computation.getEndSecondObservation()))
        .put(ExplainKey.WEIGHT, w2));
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
