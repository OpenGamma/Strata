/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.swap.FxReset;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer implementation for swap payment periods based on a rate.
 * <p>
 * The value of a payment period is calculated by combining the value of each accrual period.
 * Where necessary, the accrual periods are compounded.
 */
public class DiscountingRatePaymentPeriodPricer
    implements PaymentPeriodPricer<RatePaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DiscountingRatePaymentPeriodPricer DEFAULT = new DiscountingRatePaymentPeriodPricer(
      RateObservationFn.instance());

  /**
   * Rate observation.
   */
  private final RateObservationFn<RateObservation> rateObservationFn;

  /**
   * Creates an instance.
   * 
   * @param rateObservationFn  the rate observation function
   */
  public DiscountingRatePaymentPeriodPricer(
      RateObservationFn<RateObservation> rateObservationFn) {
    this.rateObservationFn = ArgChecker.notNull(rateObservationFn, "rateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(RatesProvider provider, RatePaymentPeriod period) {
    // futureValue * discountFactor
    double df = provider.discountFactor(period.getCurrency(), period.getPaymentDate());
    return futureValue(provider, period) * df;
  }

  @Override
  public double futureValue(RatesProvider provider, RatePaymentPeriod period) {
    // notional * fxRate
    // fxRate is 1 if no FX conversion
    double notional = period.getNotional() * fxRate(provider, period);
    // handle simple case and more complex compounding for whole payment period
    if (period.getAccrualPeriods().size() == 1) {
      RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
      return unitNotionalAccrual(provider, accrualPeriod, accrualPeriod.getSpread()) * notional;
    }
    return accrueCompounded(provider, period, notional);
  }

  //-------------------------------------------------------------------------
  // resolve the FX rate from the FX reset, returning an FX rate of 1 if not applicable
  private double fxRate(RatesProvider provider, RatePaymentPeriod paymentPeriod) {
    // inefficient to use Optional.orElse because double primitive type would be boxed
    if (paymentPeriod.getFxReset().isPresent()) {
      FxReset fxReset = paymentPeriod.getFxReset().get();
      return provider.fxIndexRate(fxReset.getIndex(), fxReset.getReferenceCurrency(), fxReset.getFixingDate());
    } else {
      return 1d;
    }
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(RatesProvider provider, RateAccrualPeriod accrualPeriod, double spread) {
    double rawRate = rawRate(provider, accrualPeriod);
    return unitNotionalAccrualRaw(rawRate, accrualPeriod, spread);
  }

  // calculate the accrual for a unit notional from the raw rate
  private double unitNotionalAccrualRaw(double rawRate, RateAccrualPeriod accrualPeriod, double spread) {
    double treatedRate = rawRate * accrualPeriod.getGearing() + spread;
    return accrualPeriod.getNegativeRateMethod().adjust(treatedRate * accrualPeriod.getYearFraction());
  }

  // finds the raw rate for the accrual period
  // the raw rate is the rate before gearing, spread and negative checks are applied
  private double rawRate(RatesProvider provider, RateAccrualPeriod accrualPeriod) {
    return rateObservationFn.rate(
        provider,
        accrualPeriod.getRateObservation(),
        accrualPeriod.getStartDate(),
        accrualPeriod.getEndDate());
  }

  //-------------------------------------------------------------------------
  // apply compounding
  private double accrueCompounded(RatesProvider provider, RatePaymentPeriod paymentPeriod, double notional) {
    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraight(provider, paymentPeriod, notional);
      case FLAT:
        return compoundedFlat(provider, paymentPeriod, notional);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusive(provider, paymentPeriod, notional);
      case NONE:
      default:
        return compoundingNone(provider, paymentPeriod, notional);
    }
  }

  // straight compounding
  private double compoundedStraight(RatesProvider provider, RatePaymentPeriod paymentPeriod, double notional) {
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(provider, accrualPeriod, accrualPeriod.getSpread());
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // flat compounding
  private double compoundedFlat(RatesProvider provider, RatePaymentPeriod paymentPeriod, double notional) {
    double cpaAccumulated = 0d;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double rate = rawRate(provider, accrualPeriod);
      cpaAccumulated += cpaAccumulated * unitNotionalAccrualRaw(rate, accrualPeriod, 0) +
          unitNotionalAccrualRaw(rate, accrualPeriod, accrualPeriod.getSpread());
    }
    return cpaAccumulated * notional;
  }

  // spread exclusive compounding
  private double compoundedSpreadExclusive(RatesProvider provider, RatePaymentPeriod paymentPeriod, double notional) {
    double notionalAccrued = notional;
    double spreadAccrued = 0;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(provider, accrualPeriod, 0);
      notionalAccrued *= investFactor;
      spreadAccrued += notional * accrualPeriod.getSpread() * accrualPeriod.getYearFraction();
    }
    return (notionalAccrued - notional + spreadAccrued);
  }

  // no compounding, just sum each accrual period
  private double compoundingNone(RatesProvider provider, RatePaymentPeriod paymentPeriod, double notional) {
    return paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(provider, accrualPeriod, accrualPeriod.getSpread()) * notional)
        .sum();
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder presentValueSensitivity(RatesProvider provider, RatePaymentPeriod period) {
    Currency ccy = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();
    double df = provider.discountFactor(period.getCurrency(), paymentDate);
    PointSensitivityBuilder fwdSensitivity = futureValueSensitivity(provider, period);
    fwdSensitivity = fwdSensitivity.multipliedBy(df);
    double futureValue = futureValue(provider, period);
    PointSensitivityBuilder dscSensitivity = provider.discountFactorZeroRateSensitivity(ccy, paymentDate);
    dscSensitivity = dscSensitivity.multipliedBy(futureValue);
    return fwdSensitivity.combinedWith(dscSensitivity);
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(RatesProvider provider,
      RatePaymentPeriod period) {
    // historic payments have zero sensi
    if (period.getPaymentDate().isBefore(provider.getValuationDate())) {
      return PointSensitivityBuilder.none();
    }
    double fxRate = 1d;
    if (period.getFxReset().isPresent()) {
      // TODO find FX rate, using 1 if no FX reset occurs
      throw new UnsupportedOperationException("FX Reset not yet implemented for futureValueSensitivity");
    }
    double notional = period.getNotional() * fxRate;
    PointSensitivityBuilder unitAccrual;
    if (period.isCompoundingApplicable()) {
      // TODO handle compounding
      throw new UnsupportedOperationException("compounding not yet implemented for futureValueSensitivity");
    } else {
      unitAccrual = unitNotionalSensiNoCompounding(provider, period);
    }
    return unitAccrual.multipliedBy(notional);
  }

  // computes the sensitivity of the payment period to the rate observations (not to the discount factors)
  private PointSensitivityBuilder unitNotionalSensiNoCompounding(RatesProvider provider, RatePaymentPeriod period) {
    Currency ccy = period.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      sensi = sensi.combinedWith(unitNotionalSensiAccrual(provider, accrualPeriod, ccy));
    }
    return sensi;
  }

  // computes the sensitivity of the accrual period to the rate observations (not to discount factors)
  private PointSensitivityBuilder unitNotionalSensiAccrual(RatesProvider provider,
      RateAccrualPeriod period, Currency ccy) {
    PointSensitivityBuilder sensi = rateObservationFn.rateSensitivity(
        provider, period.getRateObservation(), period.getStartDate(), period.getEndDate());
    return sensi.multipliedBy(period.getGearing() * period.getYearFraction());
  }

}
