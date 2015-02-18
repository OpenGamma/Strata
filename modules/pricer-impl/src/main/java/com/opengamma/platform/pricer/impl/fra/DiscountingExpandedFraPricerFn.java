/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import java.time.temporal.ChronoUnit;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.finance.fra.FraDiscountingMethod;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.observation.RateObservationFn;

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
    // futureValue * discountFactor
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    return futureValue(env, fra).multipliedBy(df);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    double unitFutureValue = 0.0;
    if (fra.getDiscounting() == FraDiscountingMethod.AFMA) {
      unitFutureValue = unitFutureValueAFMA(env, fra);
    } else {
      unitFutureValue = unitFutureValueDefault(env, fra);
    }
    return MultiCurrencyAmount.of(fra.getCurrency(), notional * unitFutureValue);
  }

  private double unitFutureValueDefault(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    double yearFraction = fra.getYearFraction();
    return (forwardRate - fixedRate) * yearFraction / (1 + forwardRate * yearFraction);
  }

  private double unitFutureValueAFMA(PricingEnvironment env, ExpandedFra fra) {
    // TODO check formula
    double fixedRate = fra.getFixedRate();
    double forwardRate = rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    double yearFraction = ChronoUnit.DAYS.between(fra.getStartDate(), fra.getEndDate()) / 365.0;
    return 1.0 / (1.0 + fixedRate * yearFraction) - 1.0 / (1.0 + forwardRate * yearFraction);
  }

}
