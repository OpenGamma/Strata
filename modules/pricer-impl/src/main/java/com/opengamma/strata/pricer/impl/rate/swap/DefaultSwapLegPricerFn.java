/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapLegPricerFn;

/**
 * Pricer implementation for swap legs.
 * <p>
 * The swap leg is priced by examining the periods and events.
 */
public class DefaultSwapLegPricerFn
    implements SwapLegPricerFn<SwapLeg> {

  /**
   * Default implementation.
   */
  public static final DefaultSwapLegPricerFn DEFAULT = new DefaultSwapLegPricerFn(
      PaymentPeriodPricerFn.instance(),
      PaymentEventPricerFn.instance());

  /**
   * Pricer for {@link PaymentPeriod}.
   */
  private final PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn;
  /**
   * Pricer for {@link PaymentEvent}.
   */
  private final PaymentEventPricerFn<PaymentEvent> paymentEventPricerFn;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricerFn  the pricer for {@link PaymentPeriod}
   * @param paymentEventPricerFn  the pricer for {@link PaymentEvent}
   */
  public DefaultSwapLegPricerFn(
      PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn,
      PaymentEventPricerFn<PaymentEvent> paymentEventPricerFn) {
    this.paymentPeriodPricerFn = ArgChecker.notNull(paymentPeriodPricerFn, "paymentPeriodPricerFn");
    this.paymentEventPricerFn = ArgChecker.notNull(paymentEventPricerFn, "paymentEventPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, SwapLeg leg, Currency currency) {
    double pv = legValue(env, leg.expand(), paymentPeriodPricerFn::presentValue, paymentEventPricerFn::presentValue);
    return CurrencyAmount.of(currency, (pv * env.fxRate(leg.getCurrency(), currency)));
  }

  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, SwapLeg leg) {
    double val = legValue(env, leg.expand(), paymentPeriodPricerFn::presentValue, paymentEventPricerFn::presentValue);
    return CurrencyAmount.of(leg.getCurrency(), val);
  }

  @Override
  public CurrencyAmount futureValue(PricingEnvironment env, SwapLeg leg) {
    double val = legValue(env, leg.expand(), paymentPeriodPricerFn::futureValue, paymentEventPricerFn::futureValue);
    return CurrencyAmount.of(leg.getCurrency(), val);
  }

  // calculate present or future value for a leg
  static double legValue(
      PricingEnvironment env,
      ExpandedSwapLeg leg,
      ToDoubleBiFunction<PricingEnvironment, PaymentPeriod> periodFn,
      ToDoubleBiFunction<PricingEnvironment, PaymentEvent> eventFn) {

    double valuePeriods = leg.getPaymentPeriods().stream()
        .filter(p -> !p.getPaymentDate().isBefore(env.getValuationDate()))
        .mapToDouble(p -> periodFn.applyAsDouble(env, p))
        .sum();
    double valueEvents = leg.getPaymentEvents().stream()
        .filter(p -> !p.getPaymentDate().isBefore(env.getValuationDate()))
        .mapToDouble(e -> eventFn.applyAsDouble(env, e))
        .sum();
    return valuePeriods + valueEvents;
  }

}
