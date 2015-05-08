package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.time.YearMonth;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Rate observation implementation for rate based on the weighted average of fixings 
 * of a single price index. 
 * <p>
 * The rate computed by this instance is based on four observations of the index,
 * two relative to the accrual start date and two relative to the accrual end date. 
 * The start index is the weighted average of the index values associated with the first two reference dates, 
 * and the end index is derived from the index values on the last two reference dates. 
 * Then the pay-off for a unit notional is {@code (Index_End / Index_Start - 1)}. 
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
    double w1 = observation.getWeight();
    double w2 = 1.0d - w1;
    double indexStart = interpolatedIndex(
        index, observation.getReferenceStartMonth(),
        observation.getReferenceStartInterpolationMonth(),
        w1, w2, provider);
    double indexEnd = interpolatedIndex(
        index, observation.getReferenceEndMonth(),
        observation.getReferenceEndInterpolationMonth(),
        w1, w2, provider);
    return indexEnd / indexStart - 1.0d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      InflationInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {
    PriceIndex index = observation.getIndex();
    double w1 = observation.getWeight();
    double w2 = 1.0d - w1;
    double indexStartInv = 1.0d / interpolatedIndex(
        index, observation.getReferenceStartMonth(),
        observation.getReferenceStartInterpolationMonth(),
        w1, w2, provider);
    double indexEnd = interpolatedIndex(
        index, observation.getReferenceEndMonth(),
        observation.getReferenceEndInterpolationMonth(),
        w1, w2, provider);
    PointSensitivityBuilder indexStartSensitivity = interpolatedIndexSensitivity(
        index, observation.getReferenceStartMonth(),
        observation.getReferenceStartInterpolationMonth(),
        w1, w2, provider);
    PointSensitivityBuilder indexEndSensitivity = interpolatedIndexSensitivity(
        index, observation.getReferenceEndMonth(),
        observation.getReferenceEndInterpolationMonth(),
        w1, w2, provider);
    indexEndSensitivity = indexEndSensitivity.multipliedBy(indexStartInv);
    indexStartSensitivity = indexStartSensitivity.multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    return indexStartSensitivity.combinedWith(indexEndSensitivity);
  }

  private double interpolatedIndex(
      PriceIndex index,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2,
      RatesProvider provider) {
    double indexReferenceStart1 = provider.inflationIndexRate(index, month1);
    double indexReferenceStart2 = provider.inflationIndexRate(index, month2);
    return weight1 * indexReferenceStart1 + weight2 * indexReferenceStart2;
  }

  private PointSensitivityBuilder interpolatedIndexSensitivity(
      PriceIndex index,
      YearMonth month1,
      YearMonth month2,
      double weight1,
      double weight2,
      RatesProvider provider) {
    PointSensitivityBuilder sensei1 = provider.inflationIndexRateSensitivity(index, month1);
    sensei1 = sensei1.multipliedBy(weight1);
    PointSensitivityBuilder sensei2 = provider.inflationIndexRateSensitivity(index, month2);
    sensei2 = sensei2.multipliedBy(weight2);
    return sensei1.combinedWith(sensei2);
  }
}
