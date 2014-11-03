/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.platform.finance.swap.FixedRateAccrualPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.FixedRateAccrualPeriodPricerFn;

/**
 * Pricer for swap accrual periods.
 */
public class StandardFixedRateAccrualPeriodPricerFn
    implements FixedRateAccrualPeriodPricerFn {

  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      FixedRateAccrualPeriod period,
      LocalDate paymentDate) {
    double df = env.discountFactor(period.getCurrency(), valuationDate, paymentDate);
    return df * futureValue(env, valuationDate, period);
  }

  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      FixedRateAccrualPeriod period) {
    return period.getNotional() * period.getRate() * period.getYearFraction();
  }

}
