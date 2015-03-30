/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.fra;

import com.opengamma.platform.finance.rate.RateObservation;
import com.opengamma.platform.finance.rate.fra.ExpandedFra;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.rate.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.rate.RateObservationFn;
import com.opengamma.platform.pricer.rate.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.sensitivity.PointSensitivities;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Pricer implementation for forward rate agreements.
 * <p>
 * The forward rate agreement is priced by examining the forward rate agreement legs.
 */
public class DiscountingExpandedFraPricerFn
    implements FraProductPricerFn<ExpandedFra> {

  /**
   * Default implementation.
   */
  public static final DiscountingExpandedFraPricerFn DEFAULT = new DiscountingExpandedFraPricerFn(
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
  public DiscountingExpandedFraPricerFn(
      RateObservationFn<RateObservation> rateObservationFn) {
    this.rateObservationFn = ArgChecker.notNull(rateObservationFn, "rateObservationFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, ExpandedFra fra) {
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double pv = notional * unitAmount * df;
    return CurrencyAmount.of(fra.getCurrency(), pv);
  }

  @Override
  public PointSensitivities presentValueSensitivity(PricingEnvironment env, ExpandedFra fra) {
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double derivative = derivative(env, fra);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(env, fra)
        .multipliedBy(derivative * df * notional);
    PointSensitivityBuilder discSens = env.discountFactorZeroRateSensitivity(fra.getCurrency(), fra.getPaymentDate())
        .multipliedBy(unitAmount * notional);
    return iborSens.withCurrency(fra.getCurrency()).combinedWith(discSens).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount futureValue(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double fv = notional * unitAmount;
    return CurrencyAmount.of(fra.getCurrency(), fv);
  }

  @Override
  public PointSensitivities futureValueSensitivity(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double derivative = derivative(env, fra);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(env, fra)
        .multipliedBy(derivative * notional);
    return iborSens.withCurrency(fra.getCurrency()).build();
  }

  //-------------------------------------------------------------------------
  // unit amount in various discounting methods
  private double unitAmount(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case NONE:
        return unitAmountNone(env, fra);
      case ISDA:
        return unitAmountIsda(env, fra);
      case AFMA:
        return unitAmountAfma(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double unitAmountNone(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (forwardRate - fixedRate) * yearFraction;
  }

  // ISDA discounting method
  private double unitAmountIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return ((forwardRate - fixedRate) / (1.0 + forwardRate * yearFraction)) * yearFraction;
  }

  // AFMA discounting method
  private double unitAmountAfma(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (1.0 / (1.0 + fixedRate * yearFraction)) - (1.0 / (1.0 + forwardRate * yearFraction));
  }

  //-------------------------------------------------------------------------
  private double derivative(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case NONE:
        return derivativeNone(env, fra);
      case ISDA:
        return derivativeIsda(env, fra);
      case AFMA:
        return derivativeAfma(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double derivativeNone(PricingEnvironment env, ExpandedFra fra) {
    return fra.getYearFraction();
  }

  // ISDA discounting method
  private double derivativeIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return (1.0 + fixedRate * yearFraction) * yearFraction * dsc * dsc;
  }

  // AFMA discounting method
  private double derivativeAfma(PricingEnvironment env, ExpandedFra fra) {
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return yearFraction * dsc * dsc;
  }

  //-------------------------------------------------------------------------
  // query the forward rate
  private double forwardRate(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

  // query the sensitivity
  private PointSensitivityBuilder forwardRateSensitivity(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rateSensitivity(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

}
