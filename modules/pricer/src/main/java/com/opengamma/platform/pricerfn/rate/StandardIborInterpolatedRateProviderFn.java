/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.rate;

import java.time.LocalDate;

import com.opengamma.basics.index.IborIndex;
import com.opengamma.platform.finance.rate.IborInterpolatedRate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Rate provider implementation for an IBOR-like index.
 * <p>
 * The rate provider examines the historic time-series of known rates and the
 * forward curve to determine the effective annualized rate.
 */
public class StandardIborInterpolatedRateProviderFn
    implements RateProviderFn<IborInterpolatedRate> {

  /**
   * Default implementation.
   */
  public static final StandardIborInterpolatedRateProviderFn DEFAULT = new StandardIborInterpolatedRateProviderFn();

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      IborInterpolatedRate rate,
      LocalDate startDate,
      LocalDate endDate) {

    LocalDate fixingDate = rate.getFixingDate();
    IborIndex index1 = rate.getShortIndex();
    IborIndex index2 = rate.getLongIndex();
    LocalDate fixingStartDate1 = index1.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate1 = index1.calculateMaturityFromEffective(fixingStartDate1);
    LocalDate fixingStartDate2 = index2.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate2 = index2.calculateMaturityFromEffective(fixingStartDate2);
    double rate1 = env.indexRate(index1, valuationDate, fixingDate);
    double rate2 = env.indexRate(index2, valuationDate, fixingDate);
    long fixingEpochDay = fixingDate.toEpochDay();
    double days1 = fixingEndDate1.toEpochDay() - fixingEpochDay;
    double days2 = fixingEndDate2.toEpochDay() - fixingEpochDay;
    double daysN = endDate.toEpochDay() - fixingEpochDay;
    double weight1 = (days2 - daysN) / (days2 - days1);
    double weight2 = (daysN - days1) / (days2 - days1);
    return ((rate1 * weight1) + (rate2 * weight2)) / (weight1 + weight2);
  }

}
