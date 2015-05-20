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
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;

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
  public double presentValue(PaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.presentValue((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.presentValue((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(PaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.presentValueSensitivity((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.presentValueSensitivity((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(PaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.futureValue((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.futureValue((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(PaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.futureValueSensitivity((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.futureValueSensitivity((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

}
