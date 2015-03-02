/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;


import com.opengamma.basics.currency.Currency;
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
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    return futureValue(env, fra).multipliedBy(df);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, ExpandedFra fra) {
    double notional = fra.getNotional();
    Currency currency = fra.getCurrency();
    double unitAmount;
    switch (fra.getDiscounting()) {
      case ISDA:
        unitAmount = unitAmountIsda(env, fra);
        break;
      case AFMA:
        unitAmount = unitAmountAfma(env, fra);
        break;
      case NONE:
        unitAmount = unitAmountNone(env, fra);
        break;
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
    return MultiCurrencyAmount.of(currency, notional * unitAmount);
  }

  //-------------------------------------------------------------------------
  // ISDA discounting method
  private double unitAmountIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return yearFraction * (forwardRate - fixedRate)  / (1.0 + yearFraction * forwardRate);
  }

  // NONE discounting method
  private double unitAmountNone(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return yearFraction * (forwardRate - fixedRate);
  }

  // AFMA discounting method
  private double unitAmountAfma(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return 1.0 / (1.0 + fixedRate * yearFraction) - 1.0 / (1.0 + forwardRate * yearFraction);
  }

  // query the forward rate
  private double forwardRate(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

}
