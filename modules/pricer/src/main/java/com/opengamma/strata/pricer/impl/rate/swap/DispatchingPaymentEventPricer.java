/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer implementation for payment events using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingPaymentEventPricer
    implements PaymentEventPricer<PaymentEvent> {

  /**
   * Default implementation.
   */
  public static final DispatchingPaymentEventPricer DEFAULT = new DispatchingPaymentEventPricer(
      DiscountingNotionalExchangePricer.DEFAULT,
      DiscountingFxResetNotionalExchangePricer.DEFAULT);

  /**
   * Pricer for {@link NotionalExchange}.
   */
  private final PaymentEventPricer<NotionalExchange> notionalExchangePricer;
  /**
   * Pricer for {@link FxResetNotionalExchange}.
   */
  private final PaymentEventPricer<FxResetNotionalExchange> fxResetNotionalExchangePricer;

  /**
   * Creates an instance.
   * 
   * @param notionalExchangePricer  the pricer for {@link NotionalExchange}
   * @param fxResetNotionalExchangePricer  the pricer for {@link FxResetNotionalExchange}
   */
  public DispatchingPaymentEventPricer(
      PaymentEventPricer<NotionalExchange> notionalExchangePricer,
      PaymentEventPricer<FxResetNotionalExchange> fxResetNotionalExchangePricer) {
    this.notionalExchangePricer = ArgChecker.notNull(notionalExchangePricer, "notionalExchangePricer");
    this.fxResetNotionalExchangePricer =
        ArgChecker.notNull(fxResetNotionalExchangePricer, "fxResetNotionalExchangePricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.presentValue(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.presentValue(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.presentValueSensitivity(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.presentValueSensitivity(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.futureValue(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.futureValue(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(PricingEnvironment env, PaymentEvent paymentEvent) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.futureValueSensitivity(env, (NotionalExchange) paymentEvent);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.futureValueSensitivity(env, (FxResetNotionalExchange) paymentEvent);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

}
