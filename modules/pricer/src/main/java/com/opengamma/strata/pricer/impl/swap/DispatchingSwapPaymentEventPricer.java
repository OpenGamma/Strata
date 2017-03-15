/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentEventPricer;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.SwapPaymentEvent;

/**
 * Pricer implementation for payment events using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingSwapPaymentEventPricer
    implements SwapPaymentEventPricer<SwapPaymentEvent> {

  /**
   * Default implementation.
   */
  public static final DispatchingSwapPaymentEventPricer DEFAULT = new DispatchingSwapPaymentEventPricer(
      DiscountingNotionalExchangePricer.DEFAULT,
      DiscountingFxResetNotionalExchangePricer.DEFAULT);

  /**
   * Pricer for {@link NotionalExchange}.
   */
  private final SwapPaymentEventPricer<NotionalExchange> notionalExchangePricer;
  /**
   * Pricer for {@link FxResetNotionalExchange}.
   */
  private final SwapPaymentEventPricer<FxResetNotionalExchange> fxResetNotionalExchangePricer;

  /**
   * Creates an instance.
   * 
   * @param notionalExchangePricer  the pricer for {@link NotionalExchange}
   * @param fxResetNotionalExchangePricer  the pricer for {@link FxResetNotionalExchange}
   */
  public DispatchingSwapPaymentEventPricer(
      SwapPaymentEventPricer<NotionalExchange> notionalExchangePricer,
      SwapPaymentEventPricer<FxResetNotionalExchange> fxResetNotionalExchangePricer) {
    this.notionalExchangePricer = ArgChecker.notNull(notionalExchangePricer, "notionalExchangePricer");
    this.fxResetNotionalExchangePricer =
        ArgChecker.notNull(fxResetNotionalExchangePricer, "fxResetNotionalExchangePricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(SwapPaymentEvent paymentEvent, RatesProvider provider) {
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
  public PointSensitivityBuilder presentValueSensitivity(SwapPaymentEvent paymentEvent, RatesProvider provider) {
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
  public double forecastValue(SwapPaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.forecastValue((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.forecastValue((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(SwapPaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.forecastValueSensitivity((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.forecastValueSensitivity((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(SwapPaymentEvent paymentEvent, RatesProvider provider, ExplainMapBuilder builder) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      notionalExchangePricer.explainPresentValue((NotionalExchange) paymentEvent, provider, builder);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      fxResetNotionalExchangePricer.explainPresentValue((FxResetNotionalExchange) paymentEvent, provider, builder);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(SwapPaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.currencyExposure((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.currencyExposure((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

  @Override
  public double currentCash(SwapPaymentEvent paymentEvent, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentEvent instanceof NotionalExchange) {
      return notionalExchangePricer.currentCash((NotionalExchange) paymentEvent, provider);
    } else if (paymentEvent instanceof FxResetNotionalExchange) {
      return fxResetNotionalExchangePricer.currentCash((FxResetNotionalExchange) paymentEvent, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentEvent.getClass().getSimpleName());
    }
  }

}
