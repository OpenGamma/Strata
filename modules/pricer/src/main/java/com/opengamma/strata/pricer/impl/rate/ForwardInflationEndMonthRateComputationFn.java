/*
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
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;

/**
 * Rate computation implementation for a price index.
 * <p>
 * The pay-off for a unit notional is {@code (IndexEnd / IndexStart)}, where
 * the start index value is given by  {@code InflationEndMonthRateComputation}
 * and the end index value is returned by {@code RatesProvider}.
 */
public class ForwardInflationEndMonthRateComputationFn
    implements RateComputationFn<InflationEndMonthRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationEndMonthRateComputationFn DEFAULT =
      new ForwardInflationEndMonthRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationEndMonthRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationEndMonthRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double indexStart = computation.getStartIndexValue();
    double indexEnd = values.value(computation.getEndObservation());
    return indexEnd / indexStart - 1;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationEndMonthRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    return values.valuePointSensitivity(computation.getEndObservation())
        .multipliedBy(1d / computation.getStartIndexValue());
  }

  @Override
  public double explainRate(
      InflationEndMonthRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndexValues values = provider.priceIndexValues(computation.getIndex());
    double indexEnd = values.value(computation.getEndObservation());
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getEndObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, computation.getIndex())
        .put(ExplainKey.INDEX_VALUE, indexEnd));
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
