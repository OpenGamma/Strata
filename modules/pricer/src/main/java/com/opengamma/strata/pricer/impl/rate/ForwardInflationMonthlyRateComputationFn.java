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
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;

/**
 * Rate computation implementation for a price index.
 * <p>
 * The pay-off for a unit notional is {@code (IndexEnd / IndexStart - 1)}, where
 * start index value and end index value are simply returned by {@code RatesProvider}.
 */
public class ForwardInflationMonthlyRateComputationFn
    implements RateComputationFn<InflationMonthlyRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardInflationMonthlyRateComputationFn DEFAULT =
      new ForwardInflationMonthlyRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardInflationMonthlyRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      InflationMonthlyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = computation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(computation.getStartObservation());
    double indexEnd = values.value(computation.getEndObservation());
    return indexEnd / indexStart - 1d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationMonthlyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = computation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(computation.getStartObservation());
    double indexEnd = values.value(computation.getEndObservation());
    double indexStartInv = 1d / indexStart;
    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(computation.getStartObservation())
        .multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(computation.getEndObservation())
        .multipliedBy(indexStartInv);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationMonthlyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndex index = computation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    double indexStart = values.value(computation.getStartObservation());
    double indexEnd = values.value(computation.getEndObservation());

    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getStartObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, indexStart));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, computation.getEndObservation().getFixingMonth().atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, indexEnd));
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
