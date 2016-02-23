/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.InflationEndMonthRateObservation;

/**
 * Rate observation implementation for a price index. 
 * <p>
 * The pay-off for a unit notional is {@code (IndexEnd / IndexStart)}, where
 * the start index value is given by  {@code InflationEndMonthRateObservation}
 * and the end index value is returned by {@code RatesProvider}.
 */
public class ForwardInflationEndMonthRateObservationFn
    implements RateObservationFn<InflationEndMonthRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationEndMonthRateObservationFn DEFAULT =
      new ForwardInflationEndMonthRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationEndMonthRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationEndMonthRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexEnd = values.value(observation.getReferenceEndMonth());
    return indexEnd / observation.getStartIndexValue() - 1;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationEndMonthRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    return values.valuePointSensitivity(observation.getReferenceEndMonth())
        .multipliedBy(1d / observation.getStartIndexValue());
  }

  @Override
  public double explainRate(
      InflationEndMonthRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexEnd = values.value(observation.getReferenceEndMonth());
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
