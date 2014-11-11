/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.Rate;
import com.opengamma.platform.finance.swap.NegativeRateMethod;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;
import com.opengamma.platform.pricerfn.rate.StandardRateProviderFn;

/**
 * Pricer implementation for swap payment periods based on a rate.
 * <p>
 * The value of a payment period is calculated by combining the value of each accrual period.
 * Where necessary, the accrual periods are compounded.
 */
public class StandardRatePaymentPeriodPricerFn
    implements PaymentPeriodPricerFn<RatePaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final StandardRatePaymentPeriodPricerFn DEFAULT = new StandardRatePaymentPeriodPricerFn(
      StandardRateProviderFn.DEFAULT);

  /**
   * Rate provider.
   */
  private final RateProviderFn<Rate> rateProviderFn;

  /**
   * Creates an instance.
   * 
   * @param rateProviderFn  the rate provider
   */
  public StandardRatePaymentPeriodPricerFn(
      RateProviderFn<Rate> rateProviderFn) {
    this.rateProviderFn = ArgChecker.notNull(rateProviderFn, "rateProviderFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RatePaymentPeriod period) {
    // futureValue * discountFactor
    double df = env.discountFactor(period.getCurrency(), valuationDate, period.getPaymentDate());
    return futureValue(env, valuationDate, period) * df;
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RatePaymentPeriod period) {
    // historic payments have zero pv
    if (period.getPaymentDate().isBefore(valuationDate)) {
      return 0;
    }
    // find FX rate, using 1 if no FX reset occurs
    double fxRate = 1d;
    if (period.getFxReset() != null) {
      CurrencyPair pair = CurrencyPair.of(period.getFxReset().getReferenceCurrency(), period.getCurrency());
      fxRate = env.fxRate(period.getFxReset().getIndex(), pair, valuationDate, period.getFxReset().getFixingDate());
    }
    double notional = period.getNotional() * fxRate;
    // handle compounding
    double unitAccrual; 
    if (period.isCompounding()) {
      unitAccrual = unitNotionalCompounded(env, valuationDate, period);
    } else {
      unitAccrual = unitNotionalNoCompounding(env, valuationDate, period);
    }
    return notional * unitAccrual;
  }

  //-------------------------------------------------------------------------
  // no compounding needed
  private double unitNotionalNoCompounding(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    return period.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(env, valuationDate, accrualPeriod, period.getNegativeRateMethod()))
        .sum();
  }

  // apply compounding
  private double unitNotionalCompounded(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    // TODO compounding methods
    double notional = 1d;
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      double unitAccrual = unitNotionalAccrual(env, valuationDate, accrualPeriod, period.getNegativeRateMethod());
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(
      PricingEnvironment env,
      LocalDate valuationDate,
      RateAccrualPeriod period,
      NegativeRateMethod negativeRateMethod) {
    double rate = rateProviderFn.rate(env, valuationDate, period.getRate(), period.getStartDate(), period.getEndDate());
    double treatedRate = rate * period.getGearing() + period.getSpread();
    return negativeRateMethod.adjust(treatedRate * period.getYearFraction());
  }

}
