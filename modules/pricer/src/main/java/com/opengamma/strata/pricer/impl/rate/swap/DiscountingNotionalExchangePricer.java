/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer implementation for the exchange of notionals.
 * <p>
 * The notional exchange is priced by discounting the value of the exchange.
 */
public class DiscountingNotionalExchangePricer
    implements PaymentEventPricer<NotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingNotionalExchangePricer DEFAULT = new DiscountingNotionalExchangePricer();

  /**
   * Creates an instance.
   */
  public DiscountingNotionalExchangePricer() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, NotionalExchange event) {
    // futureValue * discountFactor
    double df = env.discountFactor(event.getPaymentAmount().getCurrency(), event.getPaymentDate());
    return futureValue(env, event) * df;
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(PricingEnvironment env, NotionalExchange event) {
    PointSensitivityBuilder sensi = env.discountFactorZeroRateSensitivity(event.getCurrency(), event.getPaymentDate());
    return sensi.multipliedBy(event.getPaymentAmount().getAmount());
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(PricingEnvironment env, NotionalExchange event) {
    // paymentAmount
    return event.getPaymentAmount().getAmount();
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(PricingEnvironment env, NotionalExchange event) {
    return PointSensitivityBuilder.none();
  }

}
