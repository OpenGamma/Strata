package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Rate observation implementation for a price index. 
 * <p>
 * Then the pay-off for a unit notional is {@code (Index_End / Index_Start - 1)}, where
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
  public double rate(InflationMonthlyRateObservation observation, LocalDate startDate, LocalDate endDate,
      RatesProvider provider) {
    PriceIndex index = observation.getIndex();
    double indexStart = provider.inflationIndexRate(index, observation.getReferenceStartMonth());
    double indexEnd = provider.inflationIndexRate(index, observation.getReferenceEndMonth());
    return indexEnd / indexStart - 1.0d;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(InflationMonthlyRateObservation observation, LocalDate startDate,
      LocalDate endDate, RatesProvider provider) {
    PriceIndex index = observation.getIndex();
    double indexStartInv = 1.0 / provider.inflationIndexRate(index, observation.getReferenceStartMonth());
    double indexEnd = provider.inflationIndexRate(index, observation.getReferenceEndMonth());
    PointSensitivityBuilder indexStartSensitivity =
        provider.inflationIndexRateSensitivity(index, observation.getReferenceStartMonth());
    PointSensitivityBuilder indexEndSensitivity =
        provider.inflationIndexRateSensitivity(index, observation.getReferenceEndMonth());
    indexEndSensitivity = indexEndSensitivity.multipliedBy(indexStartInv);
    indexStartSensitivity = indexStartSensitivity.multipliedBy(-indexEnd * indexStartInv * indexStartInv);
    return indexStartSensitivity.combinedWith(indexEndSensitivity);
  }

}
