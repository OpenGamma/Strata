/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.Rate;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.rate.StandardRateProviderFn;
import com.opengamma.platform.pricer.rate.RateProviderFn;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

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
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(env, valuationDate, accrualPeriod, accrualPeriod.getSpread()))
        .sum();
  }

  // apply compounding
  private double unitNotionalCompounded(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    switch (period.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraight(env, valuationDate, period);
      case FLAT:
        return compoundedFlat(env, valuationDate, period);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusive(env, valuationDate, period);
      case NONE:
      default:
        // NONE is handled in unitNotionalNoCompounding()
        throw new IllegalArgumentException("Unknown CompoundingMethod");
    }
  }

  // straight compounding
  private double compoundedStraight(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    double notional = 1d;
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      double unitAccrual = unitNotionalAccrual(env, valuationDate, accrualPeriod, accrualPeriod.getSpread());
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // flat compounding
  private double compoundedFlat(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    // TODO: this is not the correct algorithm
    double notional = 1d;
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      if (accrualPeriod.getSpread() != 0) {
        throw new UnsupportedOperationException();
      }
      double unitAccrual = unitNotionalAccrual(env, valuationDate, accrualPeriod, accrualPeriod.getSpread());
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // spread exclusive compounding
  private double compoundedSpreadExclusive(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    double notional = 1d;
    double notionalAccrued = notional;
    double spreadAccrued = 0;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      double unitAccrual = unitNotionalAccrual(env, valuationDate, accrualPeriod, 0);
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
      spreadAccrued += accrualPeriod.getSpread() * accrualPeriod.getYearFraction();
    }
    return (notionalAccrued - notional) + spreadAccrued;
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(
      PricingEnvironment env,
      LocalDate valuationDate,
      RateAccrualPeriod period,
      double spread) {
    double rate = rateProviderFn.rate(env, valuationDate, period.getRate(), period.getStartDate(), period.getEndDate());
    double treatedRate = rate * period.getGearing() + spread;
    return period.getNegativeRateMethod().adjust(treatedRate * period.getYearFraction());
  }

}
