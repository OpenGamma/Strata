/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.time.YearMonth;

import com.opengamma.strata.basics.index.PriceIndex;
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
    double indexReferenceStart1 = values.value(observation.getReferenceEndMonth());
    double indexReferenceStart2 = values.value(observation.getReferenceEndInterpolationMonth());
    double w1 = observation.getWeight();
    double indexEnd = w1 * indexReferenceStart1 + (1d - w1) * indexReferenceStart2;
    return indexEnd / observation.getStartIndexValue() - 1;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationEndInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(observation.getIndex());
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(observation.getReferenceEndMonth())
        .multipliedBy(observation.getWeight());
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(observation.getReferenceEndInterpolationMonth())
        .multipliedBy(1d - observation.getWeight());
    return sensi1.combinedWith(sensi2).multipliedBy(1d / observation.getStartIndexValue());
  }

  @Override
  public double explainRate(
      InflationEndInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, endMonth.atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, values.value(endMonth))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, endInterpolationMonth.atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, values.value(endInterpolationMonth))
        .put(ExplainKey.WEIGHT, w2));
    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
