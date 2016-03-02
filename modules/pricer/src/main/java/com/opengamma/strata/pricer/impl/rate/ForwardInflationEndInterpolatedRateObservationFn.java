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
import com.opengamma.strata.market.view.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateObservation;

/**
 * Rate observation implementation for rate based on the weighted average of fixings 
 * of a single price index. 
 * <p>
 * The rate computed by this instance is based on fixed start index value
 * and two observations relative to the end date  of the period. 
 * The start index is given by {@code InflationEndInterpolatedRateObservation}.
 * The end index is the weighted average of the index values associated with the two reference dates. 
 * Then the pay-off for a unit notional is {@code IndexEnd / IndexStart}. 
 */
public class ForwardInflationEndInterpolatedRateObservationFn
    implements RateObservationFn<InflationEndInterpolatedRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationEndInterpolatedRateObservationFn DEFAULT =
      new ForwardInflationEndInterpolatedRateObservationFn();
  
  /**
   * Creates an instance.
   */
  public ForwardInflationEndInterpolatedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationEndInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    double indexStart = observation.getStartIndexValue();
    double indexEnd = interpolateEnd(observation, values);
    return indexEnd / indexStart - 1d;
  }

  // interpolate the observations at the end
  private double interpolateEnd(InflationEndInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    double indexValue1 = values.value(observation.getEndObservation());
    double indexValue2 = values.value(observation.getEndSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationEndInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    PointSensitivityBuilder sensi = endSensitivity(observation, values);
    return sensi.multipliedBy(1d / observation.getStartIndexValue());
  }

  // interpolate the observations at the end
  private PointSensitivityBuilder endSensitivity(InflationEndInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(observation.getEndObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(observation.getEndSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationEndInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getEndObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, observation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(observation.getEndObservation()))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getEndSecondObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, observation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(observation.getEndSecondObservation()))
        .put(ExplainKey.WEIGHT, w2));
    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
