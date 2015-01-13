/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.platform.finance.observation.OvernightAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
* Rate observation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate observation retrieve the rate at each fixing date in the period 
* from the {@link PricingEnvironment} and average them. 
*/
public class ForwardOvernightAveragedRateObservationFn
    implements RateObservationFn<OvernightAveragedRateObservation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightAveragedRateObservationFn DEFAULT = new ForwardOvernightAveragedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightAveragedRateObservationFn() {
  }

  @Override
  public double rate(
      PricingEnvironment env,
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    OvernightIndex index = observation.getIndex();
    int publicationOffset = index.getPublicationDateOffset(); // Publication offset is 0 or 1 day.
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1; 
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate endFixingDate = observation.getEndDate();
    List<LocalDate> fixingDates = new ArrayList<>(); // Dates on which the fixing take place
    List<LocalDate> publicationDates = new ArrayList<>(); // Dates on which the fixing is published
    List<Double> accrualFactors = new ArrayList<>(); // AF related to the accrual period
    LocalDate currentFixing = startFixingDate;
    double accrualFactorTotal = 0.0d;
    while (currentFixing.isBefore(endFixingDate)) { // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the fixing period end-date. 
      // When the fixing period end-date is not a good business day in the index calendar, 
      // the last fixing end date will be after the fixing end-date.
      LocalDate currentOnRateStart = index.calculateEffectiveFromFixing(currentFixing);
      LocalDate currentOnRateEnd = index.calculateMaturityFromEffective(currentOnRateStart);
      fixingDates.add(currentFixing);
      publicationDates.add(publicationOffset == 0 ? currentFixing : index.getFixingCalendar().next(currentFixing));
      double accrualFactor = index.getDayCount().yearFraction(currentOnRateStart, currentOnRateEnd);
      accrualFactors.add(accrualFactor);
      currentFixing = index.getFixingCalendar().next(currentFixing);
      accrualFactorTotal += accrualFactor;
    }
    // Dealing with Rate cutoff: replace the duplicated periods.
    int nbPeriods = accrualFactors.size();
    for (int i = 0; i < cutoffOffset - 1; i++) {
      fixingDates.set(nbPeriods - 1 - i, fixingDates.get(nbPeriods - cutoffOffset));
      publicationDates.set(nbPeriods - 1 - i, publicationDates.get(nbPeriods - cutoffOffset));
    }
    double accruedUnitNotional = 0d;
    // individual overnight rates accumulation
    for (int i = 0; i < nbPeriods; i++) {
      double forwardRate = env.overnightIndexRate(index, fixingDates.get(i));
      accruedUnitNotional += accrualFactors.get(i) * forwardRate;
    }
    // final rate
    return accruedUnitNotional / accrualFactorTotal;
  }

}
