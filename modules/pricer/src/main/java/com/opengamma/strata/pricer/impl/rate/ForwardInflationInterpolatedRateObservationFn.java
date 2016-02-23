/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    YearMonth startMonth = observation.getReferenceStartMonth();
    YearMonth startInterpolationMonth = observation.getReferenceStartInterpolationMonth();
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    double indexStart = interpolatedIndex(values, startMonth, startInterpolationMonth, w1, w2);
    double indexEnd = interpolatedIndex(values, endMonth, endInterpolationMonth, w1, w2);
    return indexEnd / indexStart - 1d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    YearMonth startMonth = observation.getReferenceStartMonth();
    YearMonth startInterpolationMonth = observation.getReferenceStartInterpolationMonth();
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    double indexStartInv = 1d / interpolatedIndex(values, startMonth, startInterpolationMonth, w1, w2);
    double indexEnd = interpolatedIndex(values, endMonth, endInterpolationMonth, w1, w2);
    PointSensitivityBuilder sensi1 = interpolatedSensitivity(values, startMonth, startInterpolationMonth, w1, w2)
        .multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    PointSensitivityBuilder sensi2 = interpolatedSensitivity(values, endMonth, endInterpolationMonth, w1, w2)
        .multipliedBy(indexStartInv);
    return sensi1.combinedWith(sensi2);
  }

  @Override
  public double explainRate(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    PriceIndex index = observation.getIndex();
    PriceIndexValues values = provider.priceIndexValues(index);
    YearMonth startMonth = observation.getReferenceStartMonth();
    YearMonth startInterpolationMonth = observation.getReferenceStartInterpolationMonth();
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;

    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, startMonth.atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, values.value(startMonth))
        .put(ExplainKey.WEIGHT, w1));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "InflationObservation")
        .put(ExplainKey.FIXING_DATE, startInterpolationMonth.atEndOfMonth())
        .put(ExplainKey.INDEX, index)
        .put(ExplainKey.INDEX_VALUE, values.value(startInterpolationMonth))
        .put(ExplainKey.WEIGHT, w2));
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

  //-------------------------------------------------------------------------
  // calculate the interpolates index value
  private double interpolatedIndex(
      PriceIndexValues values,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2) {

    double indexReferenceStart1 = values.value(month1);
    double indexReferenceStart2 = values.value(month2);
    return weight1 * indexReferenceStart1 + weight2 * indexReferenceStart2;
  }

  // calculate the interpolates sensitivity
  private PointSensitivityBuilder interpolatedSensitivity(
      PriceIndexValues values,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2) {

    PointSensitivityBuilder sensi1 = values.valuePointSensitivity(month1);
    sensi1 = sensi1.multipliedBy(weight1);
    PointSensitivityBuilder sensi2 = values.valuePointSensitivity(month2);
    sensi2 = sensi2.multipliedBy(weight2);
    return sensi1.combinedWith(sensi2);
  }

}
