/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.NotionalExchange;
import com.opengamma.platform.finance.swap.PaymentEvent;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;

/**
 * Pricer implementation for payment events using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingPaymentEventPricerFn
    implements PaymentEventPricerFn<PaymentEvent> {

  /**
   * Default implementation.
   */
  public static final DispatchingPaymentEventPricerFn DEFAULT = new DispatchingPaymentEventPricerFn(
      DiscountingNotionalExchangePricerFn.DEFAULT);

  /**
   * Pricer for {@link NotionalExchange}.
   */
  private final PaymentEventPricerFn<NotionalExchange> notionalExchangePricerFn;

  /**
   * Creates an instance.
   * 
   * @param notionalExchangePricerFn  the pricer for {@link NotionalExchange}
   */
  public DispatchingPaymentEventPricerFn(
      PaymentEventPricerFn<NotionalExchange> notionalExchangePricerFn) {
    this.notionalExchangePricerFn = ArgChecker.notNull(notionalExchangePricerFn, "notionalExchangePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricerFn.presentValue(env, (NotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricerFn.futureValue(env, (NotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

}
