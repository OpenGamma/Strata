/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.rate.InflationInterpolatedRateObservation;

/**
 * Rate observation implementation for rate based on the weighted average of fixings 
 * of a single price index. 
 * <p>
 * The rate computed by this instance is based on four observations of the index,
 * two relative to the accrual start date and two relative to the accrual end date. 
 * The start index is the weighted average of the index values associated with the first two reference dates, 
 * and the end index is derived from the index values on the last two reference dates. 
 * Then the pay-off for a unit notional is {@code (IndexEnd / IndexStart - 1)}. 
 */
public class ForwardInflationInterpolatedRateObservationFn
    implements RateObservationFn<InflationInterpolatedRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationInterpolatedRateObservationFn DEFAULT =
      new ForwardInflationInterpolatedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationInterpolatedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    double indexStart = interpolateStart(observation, values);
    double indexEnd = interpolateEnd(observation, values);
    return indexEnd / indexStart - 1d;
  }

  // interpolate the observations at the start
  private double interpolateStart(InflationInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    double indexValue1 = values.value(observation.getStartObservation());
    double indexValue2 = values.value(observation.getStartSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  // interpolate the observations at the end
  private double interpolateEnd(InflationInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    double indexValue1 = values.value(observation.getEndObservation());
    double indexValue2 = values.value(observation.getEndSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    double indexStart = interpolateStart(observation, values);
    double indexEnd = interpolateEnd(observation, values);
    double indexStartInv = 1d / indexStart;
    PointSensitivityBuilder sensi1 = startSensitivity(observation, values)
        .multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    PointSensitivityBuilder sensi2 = endSensitivity(observation, values)
        .multipliedBy(indexStartInv);
    return sensi1.combinedWith(sensi2);
  }

  // interpolate the observations at the start
  private PointSensitivityBuilder startSensitivity(InflationInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(observation.getStartObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(observation.getStartSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  // interpolate the observations at the end
  private PointSensitivityBuilder endSensitivity(InflationInterpolatedRateObservation observation, PriceIndexValues values) {
    double weight = observation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(observation.getEndObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(observation.getEndSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getStartObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, observation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(observation.getStartObservation()))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getStartSecondObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, observation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(observation.getStartSecondObservation()))
        .put(ExplainKey.WEIGHT, w2));
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
