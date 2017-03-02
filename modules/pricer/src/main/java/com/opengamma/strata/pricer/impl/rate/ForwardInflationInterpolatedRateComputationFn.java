/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;

/**
 * Rate computation implementation for rate based on the weighted average of fixings 
 * of a single price index.
 * <p>
 * The rate computed by this instance is based on four observations of the index,
 * two relative to the accrual start date and two relative to the accrual end date.
 * The start index is the weighted average of the index values associated with the first two reference dates, 
 * and the end index is derived from the index values on the last two reference dates.
 * Then the pay-off for a unit notional is {@code (IndexEnd / IndexStart - 1)}. 
 */
public class ForwardInflationInterpolatedRateComputationFn
    implements RateComputationFn<InflationInterpolatedRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationInterpolatedRateComputationFn DEFAULT =
      new ForwardInflationInterpolatedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationInterpolatedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double indexStart = interpolateStart(computation, values);
    double indexEnd = interpolateEnd(computation, values);
    return indexEnd / indexStart - 1d;
  }

  // interpolate the computations at the start
  private double interpolateStart(InflationInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    double indexValue1 = values.value(computation.getStartObservation());
    double indexValue2 = values.value(computation.getStartSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  // interpolate the observations at the end
  private double interpolateEnd(InflationInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    double indexValue1 = values.value(computation.getEndObservation());
    double indexValue2 = values.value(computation.getEndSecondObservation());
    return weight * indexValue1 + (1d - weight) * indexValue2;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double indexStart = interpolateStart(computation, values);
    double indexEnd = interpolateEnd(computation, values);
    double indexStartInv = 1d / indexStart;
    PointSensitivityBuilder sensi1 = startSensitivity(computation, values)
        .multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    PointSensitivityBuilder sensi2 = endSensitivity(computation, values)
        .multipliedBy(indexStartInv);
    return sensi1.combinedWith(sensi2);
  }

  // interpolate the observations at the start
  private PointSensitivityBuilder startSensitivity(InflationInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(computation.getStartObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(computation.getStartSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  // interpolate the observations at the end
  private PointSensitivityBuilder endSensitivity(InflationInterpolatedRateComputation computation, PriceIndexValues values) {
    double weight = computation.getWeight();
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(computation.getEndObservation())
        .multipliedBy(weight);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(computation.getEndSecondObservation())
        .multipliedBy(1d - weight);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double w1 = computation.getWeight();
    double w2 = 1d - w1;
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getStartObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, computation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(computation.getStartObservation()))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getStartSecondObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, computation.getIndex())
        .put(ExplainKey.INDEX_VALUE, values.value(computation.getStartSecondObservation()))
        .put(ExplainKey.WEIGHT, w2));
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
