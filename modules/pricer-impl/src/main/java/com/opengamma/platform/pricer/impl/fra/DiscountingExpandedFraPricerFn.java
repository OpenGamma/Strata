/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;

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
  public MultiCurrencyAmount presentValue(PricingEnvironment env, ExpandedFra fra) {
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double pv = notional * unitAmount * df;
    return MultiCurrencyAmount.of(fra.getCurrency(), pv);
  }

  public MulticurveSensitivity3LD presentValueCurveSensitivity3LD(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double[] unitAmountAndDerivative = unitAmountAndDerivative(env, fra);
    double dfDerivaitive = -df * env.relativeTime(fra.getPaymentDate());

    Pair<Double, MulticurveSensitivity3LD> rateAndSensitivity = rateObservationFn
        .rateMulticurveSensitivity3LD(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    MulticurveSensitivity3LD sensitivity = rateAndSensitivity.getSecond().multipliedBy(
        unitAmountAndDerivative[1] * df * notional);

    List<ZeroRateSensitivityLD> zeroSensitivity = new ArrayList<>();
    zeroSensitivity.add(new ZeroRateSensitivityLD(fra.getCurrency(), fra.getPaymentDate(), dfDerivaitive *
        unitAmountAndDerivative[0] * notional, fra.getCurrency())); // two currencies are the same temporarily.  
    MulticurveSensitivity3LD dscSensitivity = MulticurveSensitivity3LD.ofZeroRate(zeroSensitivity);

    sensitivity.add(dscSensitivity);
    return sensitivity;
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double fv = notional * unitAmount;
    return MultiCurrencyAmount.of(fra.getCurrency(), fv);
  }

  public MulticurveSensitivity3LD futureValueCurveSensitivity3LD(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double[] unitAmountAndDerivative = unitAmountAndDerivative(env, fra);

    Pair<Double, MulticurveSensitivity3LD> rateAndSensitivity = rateObservationFn
        .rateMulticurveSensitivity3LD(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    MulticurveSensitivity3LD sensitivity = rateAndSensitivity.getSecond().multipliedBy(
        unitAmountAndDerivative[1] * notional);
    return sensitivity;
  }

  //-------------------------------------------------------------------------
  // unit amount in various discounting methods
  private double unitAmount(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case ISDA:
        return unitAmountIsda(env, fra);
      case AFMA:
        return unitAmountAfma(env, fra);
      case NONE:
        return unitAmountNone(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  private double[] unitAmountAndDerivative(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case ISDA:
        return unitAmountAndDerivativeIsda(env, fra);
      case AFMA:
        return unitAmountAndDerivativeAfma(env, fra);
      case NONE:
        return unitAmountAndDerivativeNone(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // ISDA discounting method
  private double unitAmountIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return ((forwardRate - fixedRate) / (1.0 + forwardRate * yearFraction)) * yearFraction;
  }

  private double[] unitAmountAndDerivativeIsda(PricingEnvironment env, ExpandedFra fra) {
    double[] res = new double[2];
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    res[0] = (forwardRate - fixedRate) * dsc * yearFraction;
    res[1] = (1.0 + fixedRate * yearFraction) * yearFraction * dsc * dsc;
    return res;
  }

  // NONE discounting method
  private double unitAmountNone(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (forwardRate - fixedRate) * yearFraction;
  }

  private double[] unitAmountAndDerivativeNone(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double[] res = new double[2];
    res[0] = (forwardRate - fixedRate) * yearFraction;
    res[1] = yearFraction;
    return res;
  }

  // AFMA discounting method
  private double unitAmountAfma(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (1.0 / (1.0 + fixedRate * yearFraction)) - (1.0 / (1.0 + forwardRate * yearFraction));
  }

  private double[] unitAmountAndDerivativeAfma(PricingEnvironment env, ExpandedFra fra) {
    double[] res = new double[2];
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    res[0] = 1.0 / (1.0 + fixedRate * yearFraction) - dsc;
    res[1] = yearFraction * dsc * dsc;
    return res;
  }

  // query the forward rate
  private double forwardRate(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

}
