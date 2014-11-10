/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.AccrualPeriodPricerFn;
import com.opengamma.platform.pricerfn.rate.StandardRateProviderFn;

/**
 * Pricer implementation for swap accrual periods based on a rate.
 * <p>
 * The value of an accrual period is calculated by multiplying four elements:
 * <ul>
 * <li>the notional
 * <li>the FX rate, using 1 if there is no FX reset conversion
 * <li>the treated interest rate, which can be calculated in various ways
 * <li>the year fraction, based on the accrual period day count
 * </ul>
 */
public class StandardRateAccrualPeriodPricerFn
    implements AccrualPeriodPricerFn<RateAccrualPeriod> {

  /**
   * Default implementation.
   */
  public static final StandardRateAccrualPeriodPricerFn DEFAULT = new StandardRateAccrualPeriodPricerFn();

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RateAccrualPeriod period,
      LocalDate paymentDate) {
    // futureValue * discountFactor
    double df = env.discountFactor(period.getCurrency(), valuationDate, paymentDate);
    return df * futureValue(env, valuationDate, period);
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RateAccrualPeriod period) {
    // find FX rate, using 1 if no FX reset occurs
    double fxRate = 1d;
    if (period.getFxReset() != null) {
      CurrencyPair pair = CurrencyPair.of(period.getFxReset().getReferenceCurrency(), period.getCurrency());
      fxRate = env.fxRate(period.getFxReset().getIndex(), pair, valuationDate, period.getFxReset().getFixingDate());
    }
    // calculated result
    double rate = StandardRateProviderFn.DEFAULT.rate(
        env, valuationDate, period.getRate(), period.getStartDate(), period.getEndDate());
    double treatedRate = rate * period.getGearing() + period.getSpread();
    return period.getNotional() * fxRate * treatedRate * period.getYearFraction();
  }

}
