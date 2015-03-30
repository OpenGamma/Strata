/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.swap;

import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.swap.PaymentEventPricerFn;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;

/**
 * Pricer implementation for the exchange of notionals.
 * <p>
 * The notional exchange is priced by discounting the value of the exchange.
 */
public class DiscountingNotionalExchangePricerFn
    implements PaymentEventPricerFn<NotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingNotionalExchangePricerFn DEFAULT = new DiscountingNotionalExchangePricerFn();

  /**
   * Creates an instance.
   */
  public DiscountingNotionalExchangePricerFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, NotionalExchange event) {
    // futureValue * discountFactor
    double df = env.discountFactor(event.getPaymentAmount().getCurrency(), event.getPaymentDate());
    return futureValue(env, event) * df;
  }

  @Override
  public double futureValue(PricingEnvironment env, NotionalExchange event) {
    // paymentAmount
    return event.getPaymentAmount().getAmount();
  }

}
