/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.time.YearMonth;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.PriceIndexProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;

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
    YearMonth startMonth = observation.getReferenceStartMonth();
    YearMonth startInterpolationMonth = observation.getReferenceStartInterpolationMonth();
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    double indexStart = interpolatedIndex(index, startMonth, startInterpolationMonth, w1, w2, provider);
    double indexEnd = interpolatedIndex(index, endMonth, endInterpolationMonth, w1, w2, provider);
    return indexEnd / indexStart - 1d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    PriceIndex index = observation.getIndex();
    YearMonth startMonth = observation.getReferenceStartMonth();
    YearMonth startInterpolationMonth = observation.getReferenceStartInterpolationMonth();
    YearMonth endMonth = observation.getReferenceEndMonth();
    YearMonth endInterpolationMonth = observation.getReferenceEndInterpolationMonth();
    double w1 = observation.getWeight();
    double w2 = 1d - w1;
    double indexStartInv = 1d / interpolatedIndex(index, startMonth, startInterpolationMonth, w1, w2, provider);
    double indexEnd = interpolatedIndex(index, endMonth, endInterpolationMonth, w1, w2, provider);
    PointSensitivityBuilder indexStartSensitivity =
        interpolatedIndexSensitivity(index, startMonth, startInterpolationMonth, w1, w2, provider);
    PointSensitivityBuilder indexEndSensitivity =
        interpolatedIndexSensitivity(index, endMonth, endInterpolationMonth, w1, w2, provider);
    indexEndSensitivity = indexEndSensitivity.multipliedBy(indexStartInv);
    indexStartSensitivity = indexStartSensitivity.multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    return indexStartSensitivity.combinedWith(indexEndSensitivity);
  }

  //-------------------------------------------------------------------------
  // calculate the interpolates index value
  private double interpolatedIndex(
      PriceIndex index,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2,
      RatesProvider provider) {

    PriceIndexProvider priceProvider = provider.data(PriceIndexProvider.class);
    double indexReferenceStart1 = priceProvider.inflationIndexRate(index, month1, provider);
    double indexReferenceStart2 = priceProvider.inflationIndexRate(index, month2, provider);
    return weight1 * indexReferenceStart1 + weight2 * indexReferenceStart2;
  }

  // calculate the interpolates sensitivity
  private PointSensitivityBuilder interpolatedIndexSensitivity(
      PriceIndex index,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2,
      RatesProvider provider) {

    PriceIndexProvider priceProvider = provider.data(PriceIndexProvider.class);
    PointSensitivityBuilder sensi1 = priceProvider.inflationIndexRateSensitivity(index, month1, provider);
    sensi1 = sensi1.multipliedBy(weight1);
    PointSensitivityBuilder sensi2 = priceProvider.inflationIndexRateSensitivity(index, month2, provider);
    sensi2 = sensi2.multipliedBy(weight2);
    return sensi1.combinedWith(sensi2);
  }

}
