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
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
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
  public double presentValue(RatePaymentPeriod period, RatesProvider provider) {
    // futureValue * discountFactor
    double df = provider.discountFactor(period.getCurrency(), period.getPaymentDate());
    return futureValue(period, provider) * df;
  }

  @Override
  public double futureValue(RatePaymentPeriod period, RatesProvider provider) {
    // notional * fxRate
    // fxRate is 1 if no FX conversion
    double notional = period.getNotional() * fxRate(period, provider);
    // handle simple case and more complex compounding for whole payment period
    if (period.getAccrualPeriods().size() == 1) {
      RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
      return unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider) * notional;
    }
    return accrueCompounded(period, notional, provider);
  }

  //-------------------------------------------------------------------------
  // resolve the FX rate from the FX reset, returning an FX rate of 1 if not applicable
  private double fxRate(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    // inefficient to use Optional.orElse because double primitive type would be boxed
    if (paymentPeriod.getFxReset().isPresent()) {
      FxReset fxReset = paymentPeriod.getFxReset().get();
      return provider.fxIndexRate(fxReset.getIndex(), fxReset.getReferenceCurrency(), fxReset.getFixingDate());
    } else {
      return 1d;
    }
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(RateAccrualPeriod accrualPeriod, double spread, RatesProvider provider) {
    double rawRate = rawRate(accrualPeriod, provider);
    return unitNotionalAccrualRaw(accrualPeriod, rawRate, spread);
  }

  // calculate the accrual for a unit notional from the raw rate
  private double unitNotionalAccrualRaw(RateAccrualPeriod accrualPeriod, double rawRate, double spread) {
    double treatedRate = rawRate * accrualPeriod.getGearing() + spread;
    return accrualPeriod.getNegativeRateMethod().adjust(treatedRate * accrualPeriod.getYearFraction());
  }

  // finds the raw rate for the accrual period
  // the raw rate is the rate before gearing, spread and negative checks are applied
  private double rawRate(RateAccrualPeriod accrualPeriod, RatesProvider provider) {
    return rateObservationFn.rate(
        accrualPeriod.getRateObservation(),
        accrualPeriod.getStartDate(),
        accrualPeriod.getEndDate(),
        provider);
  }

  //-------------------------------------------------------------------------
  // apply compounding
  private double accrueCompounded(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraight(paymentPeriod, notional, provider);
      case FLAT:
        return compoundedFlat(paymentPeriod, notional, provider);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusive(paymentPeriod, notional, provider);
      case NONE:
      default:
        return compoundingNone(paymentPeriod, notional, provider);
    }
  }

  // straight compounding
  private double compoundedStraight(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider);
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // flat compounding
  private double compoundedFlat(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double cpaAccumulated = 0d;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double rate = rawRate(accrualPeriod, provider);
      cpaAccumulated += cpaAccumulated * unitNotionalAccrualRaw(accrualPeriod, rate, 0) +
          unitNotionalAccrualRaw(accrualPeriod, rate, accrualPeriod.getSpread());
    }
    return cpaAccumulated * notional;
  }

  // spread exclusive compounding
  private double compoundedSpreadExclusive(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    double notionalAccrued = notional;
    double spreadAccrued = 0;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, 0, provider);
      notionalAccrued *= investFactor;
      spreadAccrued += notional * accrualPeriod.getSpread() * accrualPeriod.getYearFraction();
    }
    return (notionalAccrued - notional + spreadAccrued);
  }

  // no compounding, just sum each accrual period
  private double compoundingNone(RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    return paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider) * notional)
        .sum();
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder presentValueSensitivity(RatePaymentPeriod period, RatesProvider provider) {
    Currency ccy = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();
    double df = provider.discountFactor(period.getCurrency(), paymentDate);
    PointSensitivityBuilder fwdSensitivity = futureValueSensitivity(period, provider);
    fwdSensitivity = fwdSensitivity.multipliedBy(df);
    double futureValue = futureValue(period, provider);
    PointSensitivityBuilder dscSensitivity = provider.discountFactorZeroRateSensitivity(ccy, paymentDate);
    dscSensitivity = dscSensitivity.multipliedBy(futureValue);
    return fwdSensitivity.combinedWith(dscSensitivity);
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(RatePaymentPeriod period, RatesProvider provider) {
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
      unitAccrual = accrueCompoundedSensitivity(period, notional, provider);
    } else {
      unitAccrual = unitNotionalSensiNoCompounding(period, provider);
    }
    return unitAccrual.multipliedBy(notional);
  }

  // computes the sensitivity of the payment period to the rate observations (not to the discount factors)
  private PointSensitivityBuilder unitNotionalSensiNoCompounding(RatePaymentPeriod period, RatesProvider provider) {
    Currency ccy = period.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      sensi = sensi.combinedWith(unitNotionalSensiAccrual(accrualPeriod, ccy, provider));
    }
    return sensi;
  }

  // computes the sensitivity of the accrual period to the rate observations (not to discount factors)
  private PointSensitivityBuilder unitNotionalSensiAccrual(
      RateAccrualPeriod period,
      Currency ccy,
      RatesProvider provider) {

    PointSensitivityBuilder sensi = rateObservationFn.rateSensitivity(
        period.getRateObservation(), period.getStartDate(), period.getEndDate(), provider);
    return sensi.multipliedBy(period.getGearing() * period.getYearFraction());
  }

  //-------------------------------------------------------------------------
  // apply compounding - sensitivity
  private PointSensitivityBuilder accrueCompoundedSensitivity(
      RatePaymentPeriod paymentPeriod, double notional, RatesProvider provider) {
    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraightSensitivity(paymentPeriod, provider);
      case FLAT:
        return compoundedFlatSensitivity(paymentPeriod, notional, provider);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusiveSensitivity(paymentPeriod, provider);
      case NONE:
      default:
        return unitNotionalSensiNoCompounding(paymentPeriod, provider);
    }
  }

  // straight compounding
  private PointSensitivityBuilder compoundedStraightSensitivity(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    double notionalAccrued = 1.0d;
    Currency ccy = paymentPeriod.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1.0d + unitNotionalAccrual(accrualPeriod, accrualPeriod.getSpread(), provider);
      notionalAccrued *= investFactor;
      PointSensitivityBuilder investFactorSensi =
          unitNotionalSensiAccrual(accrualPeriod, ccy, provider).multipliedBy(1.0d / investFactor);
      sensi = sensi.combinedWith(investFactorSensi);
    }
    return sensi.multipliedBy(notionalAccrued);
  }

  // flat compounding
  private PointSensitivityBuilder compoundedFlatSensitivity(RatePaymentPeriod paymentPeriod, double notional,
      RatesProvider provider) {
    //TODO implementation
    //    double cpaAccumulated = 0d;
    //    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
    //      double rate = rawRate(accrualPeriod, provider);
    //      cpaAccumulated += cpaAccumulated * unitNotionalAccrualRaw(accrualPeriod, rate, 0) +
    //          unitNotionalAccrualRaw(accrualPeriod, rate, accrualPeriod.getSpread());
    //    }
    //    return cpaAccumulated * notional;
    return null;
  }

  // spread exclusive compounding
  private PointSensitivityBuilder compoundedSpreadExclusiveSensitivity(
      RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    double notionalAccrued = 1.0;
    Currency ccy = paymentPeriod.getCurrency();
    PointSensitivityBuilder sensi = PointSensitivityBuilder.none();
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(accrualPeriod, 0, provider);
      notionalAccrued *= investFactor;
      PointSensitivityBuilder investFactorSensi =
          unitNotionalSensiAccrual(accrualPeriod, ccy, provider).multipliedBy(1.0d / investFactor);
      sensi = sensi.combinedWith(investFactorSensi);
    }
    return sensi.multipliedBy(notionalAccrued);
  }

}
