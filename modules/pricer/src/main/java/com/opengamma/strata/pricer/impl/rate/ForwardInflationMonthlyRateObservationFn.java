/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.InflationMonthlyRateObservation;

/**
 * Rate observation implementation for a price index. 
 * <p>
 * The pay-off for a unit notional is {@code (IndexEnd / IndexStart - 1)}, where
 * start index value and end index value are simply returned by {@code RatesProvider}.
 */
public class ForwardInflationMonthlyRateObservationFn
    implements RateObservationFn<InflationMonthlyRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationMonthlyRateObservationFn DEFAULT =
      new ForwardInflationMonthlyRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationMonthlyRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationMonthlyRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(observation.getReferenceStartMonth());
    double indexEnd = values.value(observation.getReferenceEndMonth());
    return indexEnd / indexStart - 1d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationMonthlyRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(observation.getReferenceStartMonth());
    double indexEnd = values.value(observation.getReferenceEndMonth());
    double indexStartInv = 1d / indexStart;
    PointSensitivityBuilder indexStartSensitivity = values.valuePointSensitivity(observation.getReferenceStartMonth());
    PointSensitivityBuilder indexEndSensitivity = values.valuePointSensitivity(observation.getReferenceEndMonth());
    indexEndSensitivity = indexEndSensitivity.multipliedBy(indexStartInv);
    indexStartSensitivity = indexStartSensitivity.multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    return indexStartSensitivity.combinedWith(indexEndSensitivity);
  }

  @Override
  public double explainRate(
      InflationMonthlyRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(observation.getReferenceStartMonth());
    double indexEnd = values.value(observation.getReferenceEndMonth());

    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getReferenceStartMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, indexStart));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, observation.getReferenceEndMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, indexEnd));
    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
