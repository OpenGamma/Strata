/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.fra.ExpandedFra;
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
    return MultiCurrencyAmount.of(fra.getCurrency(), futureValue0(env, fra) * df);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, ExpandedFra fra) {
    return MultiCurrencyAmount.of(fra.getCurrency(), futureValue0(env, fra));
  }

  private double futureValue0(PricingEnvironment env, ExpandedFra fra) {
    // TODO: FRA discounting method
    // TODO: remove BuySell from ExpandedFra and alter sign of notional in Fra.expand()
    double fixedRate = fra.getFixedRate();
    double forwardRate = rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    double yearFraction = fra.getYearFraction();
    double notional = fra.getNotional();
    return notional * ((forwardRate - fixedRate) * yearFraction) / (1 + forwardRate * yearFraction);
  }

}
