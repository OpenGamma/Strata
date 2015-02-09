/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.FxResetNotionalExchange;
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
      DiscountingNotionalExchangePricerFn.DEFAULT,
      DiscountingFxResetNotionalExchangePricerFn.DEFAULT);

  /**
   * Pricer for {@link NotionalExchange}.
   */
  private final PaymentEventPricerFn<NotionalExchange> notionalExchangePricerFn;
  /**
   * Pricer for {@link FxResetNotionalExchange}.
   */
  private final PaymentEventPricerFn<FxResetNotionalExchange> fxResetNotionalExchangePricerFn;

  /**
   * Creates an instance.
   * 
   * @param notionalExchangePricerFn  the pricer for {@link NotionalExchange}
   * @param fxResetNotionalExchangePricerFn  the pricer for {@link FxResetNotionalExchange}
   */
  public DispatchingPaymentEventPricerFn(
      PaymentEventPricerFn<NotionalExchange> notionalExchangePricerFn,
      PaymentEventPricerFn<FxResetNotionalExchange> fxResetNotionalExchangePricerFn) {
    this.notionalExchangePricerFn = ArgChecker.notNull(notionalExchangePricerFn, "notionalExchangePricerFn");
    this.fxResetNotionalExchangePricerFn =
        ArgChecker.notNull(fxResetNotionalExchangePricerFn, "fxResetNotionalExchangePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricerFn.presentValue(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricerFn.presentValue(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricerFn.futureValue(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricerFn.futureValue(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

}
