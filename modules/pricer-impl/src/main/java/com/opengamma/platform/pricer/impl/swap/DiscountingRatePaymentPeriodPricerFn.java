/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.opengamma.basics.currency.Currency;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.finance.swap.FxReset;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

/**
 * Pricer implementation for swap payment periods based on a rate.
 * <p>
 * The value of a payment period is calculated by combining the value of each accrual period.
 * Where necessary, the accrual periods are compounded.
 */
public class DiscountingRatePaymentPeriodPricerFn
    implements PaymentPeriodPricerFn<RatePaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DiscountingRatePaymentPeriodPricerFn DEFAULT = new DiscountingRatePaymentPeriodPricerFn(
      DispatchingRateObservationFn.DEFAULT);

  /**
   * Rate observation.
   */
  private final RateObservationFn<RateObservation> rateObservationFn;

  /**
   * Creates an instance.
   * 
   * @param rateObservationFn  the rate observation function
   */
  public DiscountingRatePaymentPeriodPricerFn(
      RateObservationFn<RateObservation> rateObservationFn) {
    this.rateObservationFn = ArgChecker.notNull(rateObservationFn, "rateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, RatePaymentPeriod period) {
    // futureValue * discountFactor
    double df = env.discountFactor(period.getCurrency(), period.getPaymentDate());
    return futureValue(env, period) * df;
  }

  @Override
  public double futureValue(PricingEnvironment env, RatePaymentPeriod period) {
    // historic payments have zero pv
    if (period.getPaymentDate().isBefore(env.getValuationDate())) {
      return 0;
    }
    // notional * fxRate
    // fxRate is 1 if no FX conversion
    double notional = period.getNotional() * fxRate(env, period);
    // handle simple case and more complex compounding for whole payment period
    if (period.getAccrualPeriods().size() == 1) {
      RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
      return unitNotionalAccrual(env, accrualPeriod, accrualPeriod.getSpread()) * notional;
    }
    return accrueCompounded(env, period, notional);
  }

  //-------------------------------------------------------------------------
  // resolve the FX rate from the FX reset, returning an FX rate of 1 if not applicable
  private double fxRate(PricingEnvironment env, RatePaymentPeriod paymentPeriod) {
    // inefficient to use Optional.orElse because double primitive type would be boxed
    if (paymentPeriod.getFxReset().isPresent()) {
      FxReset fxReset = paymentPeriod.getFxReset().get();
      return env.fxIndexRate(fxReset.getIndex(), fxReset.getReferenceCurrency(), fxReset.getFixingDate());
    } else {
      return 1d;
    }
  }

  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(PricingEnvironment env, RateAccrualPeriod accrualPeriod, double spread) {
    double rawRate = rawRate(env, accrualPeriod);
    return unitNotionalAccrualRaw(rawRate, accrualPeriod, spread);
  }

  // calculate the accrual for a unit notional from the raw rate
  private double unitNotionalAccrualRaw(double rawRate, RateAccrualPeriod accrualPeriod, double spread) {
    double treatedRate = rawRate * accrualPeriod.getGearing() + spread;
    return accrualPeriod.getNegativeRateMethod().adjust(treatedRate * accrualPeriod.getYearFraction());
  }

  // finds the raw rate for the accrual period
  // the raw rate is the rate before gearing, spread and negative checks are applied
  private double rawRate(PricingEnvironment env, RateAccrualPeriod accrualPeriod) {
    return rateObservationFn.rate(
        env,
        accrualPeriod.getRateObservation(),
        accrualPeriod.getStartDate(),
        accrualPeriod.getEndDate());
  }

  //-------------------------------------------------------------------------
  // apply compounding
  private double accrueCompounded(PricingEnvironment env, RatePaymentPeriod paymentPeriod, double notional) {
    switch (paymentPeriod.getCompoundingMethod()) {
      case STRAIGHT:
        return compoundedStraight(env, paymentPeriod, notional);
      case FLAT:
        return compoundedFlat(env, paymentPeriod, notional);
      case SPREAD_EXCLUSIVE:
        return compoundedSpreadExclusive(env, paymentPeriod, notional);
      case NONE:
      default:
        return compoundingNone(env, paymentPeriod, notional);
    }
  }

  // straight compounding
  private double compoundedStraight(PricingEnvironment env, RatePaymentPeriod paymentPeriod, double notional) {
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(env, accrualPeriod, accrualPeriod.getSpread());
      notionalAccrued *= investFactor;
    }
    return (notionalAccrued - notional);
  }

  // flat compounding
  private double compoundedFlat(PricingEnvironment env, RatePaymentPeriod paymentPeriod, double notional) {
    double cpaAccumulated = 0d;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double rate = rawRate(env, accrualPeriod);
      cpaAccumulated += cpaAccumulated * unitNotionalAccrualRaw(rate, accrualPeriod, 0) +
          unitNotionalAccrualRaw(rate, accrualPeriod, accrualPeriod.getSpread());
    }
    return cpaAccumulated * notional;
  }

  // spread exclusive compounding
  private double compoundedSpreadExclusive(PricingEnvironment env, RatePaymentPeriod paymentPeriod, double notional) {
    double notionalAccrued = notional;
    double spreadAccrued = 0;
    for (RateAccrualPeriod accrualPeriod : paymentPeriod.getAccrualPeriods()) {
      double investFactor = 1 + unitNotionalAccrual(env, accrualPeriod, 0);
      notionalAccrued *= investFactor;
      spreadAccrued += notional * accrualPeriod.getSpread() * accrualPeriod.getYearFraction();
    }
    return (notionalAccrued - notional + spreadAccrued);
  }

  // no compounding, just sum each accrual period
  private double compoundingNone(PricingEnvironment env, RatePaymentPeriod paymentPeriod, double notional) {
    return paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(env, accrualPeriod, accrualPeriod.getSpread()) * notional)
        .sum();
  }

  @Override
  public Pair<Double, MulticurveSensitivity3LD> presentValueCurveSensitivity3LD(
      PricingEnvironment env, RatePaymentPeriod period) {
    Currency ccy = period.getCurrency();
    double paymentTime = env.relativeTime(period.getPaymentDate());
    double df = env.discountFactor(period.getCurrency(), period.getPaymentDate());
    Pair<Double, MulticurveSensitivity3LD> fvSensitivity = futureValueCurveSensitivity3LD(env, period);
    double pv = fvSensitivity.getFirst() * df;
    final List<ZeroRateSensitivityLD> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(new ZeroRateSensitivityLD(ccy, period.getPaymentDate(), -paymentTime * df *
        fvSensitivity.getFirst(), ccy));
    MulticurveSensitivity3LD sensi = MulticurveSensitivity3LD.ofZeroRate(zeroRateSensi);
    sensi.add(fvSensitivity.getSecond().multipliedBy(df));
    return Pair.of(pv, sensi);
  }

  public Pair<Double, MulticurveSensitivity3LD> futureValueCurveSensitivity3LD(PricingEnvironment env,
      RatePaymentPeriod period) {
    // historic payments have zero sensi
    if (period.getPaymentDate().isBefore(env.getValuationDate())) {
      return Pair.of(0.0D, new MulticurveSensitivity3LD());
    }
    // TODO find FX rate, using 1 if no FX reset occurs
    double fxRate = 1d;
    if (period.getFxReset().isPresent()) {
      throw new NotImplementedException("FX Reset not yet implemented for futureValueCurveSensitivity");
    }
    double notional = period.getNotional() * fxRate;
    // TODO handle compounding
    Pair<Double, MulticurveSensitivity3LD> unitAccrual;
    if (period.isCompoundingApplicable()) {
      throw new NotImplementedException("compounding not yet implemented for futureValueCurveSensitivity");
    } else {
      unitAccrual = unitNotionalSensiNoCompounding3LD(env, period);
    }
    return Pair.of(notional * unitAccrual.getFirst(), unitAccrual.getSecond().multipliedBy(notional));
  }

  private Pair<Double, MulticurveSensitivity3LD> unitNotionalSensiNoCompounding3LD(PricingEnvironment env,
      RatePaymentPeriod period) {
    Currency ccy = period.getCurrency();
    double un = 0.0d;
    MulticurveSensitivity3LD sensi = new MulticurveSensitivity3LD();
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
      Pair<Double, MulticurveSensitivity3LD> pair = unitNotionalSensiAccrual3LD(env, accrualPeriod,
          accrualPeriod.getSpread(), ccy);
      un += pair.getFirst();
      sensi.add(pair.getSecond());
    }
    return Pair.of(un, sensi);
  }

  private Pair<Double, MulticurveSensitivity3LD> unitNotionalSensiAccrual3LD(PricingEnvironment env,
      RateAccrualPeriod period, double spread, Currency ccy) {
    Pair<Double, MulticurveSensitivity3LD> pair = rateObservationFn.rateMulticurveSensitivity3LD(
        env, period.getRateObservation(), period.getStartDate(), period.getEndDate());
    double treatedRate = pair.getFirst() * period.getGearing() + spread;
    double unitNotionalAccrual = period.getNegativeRateMethod().adjust(treatedRate * period.getYearFraction());
    //Backward sweep.
    //FIXME: need to adjust also the sensitivity
    return Pair.of(unitNotionalAccrual, pair.getSecond().multipliedBy(period.getGearing() * period.getYearFraction()));
  }
}
