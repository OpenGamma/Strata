/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.pricer.impl.rate.swap.DefaultSwapLegPricerFn.legValue;

import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapProductPricerFn;

/**
 * Pricer implementation for swap products.
 * <p>
 * The swap is priced by examining the swap legs.
 */
public class DefaultSwapProductPricerFn
    implements SwapProductPricerFn<SwapProduct> {

  /**
   * Default implementation.
   */
  public static final DefaultSwapProductPricerFn DEFAULT = new DefaultSwapProductPricerFn(
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
  public DefaultSwapProductPricerFn(
      PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn,
      PaymentEventPricerFn<PaymentEvent> paymentEventPricerFn) {
    this.paymentPeriodPricerFn = ArgChecker.notNull(paymentPeriodPricerFn, "paymentPeriodPricerFn");
    this.paymentEventPricerFn = ArgChecker.notNull(paymentEventPricerFn, "paymentEventPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, SwapProduct swap, Currency currency) {
    double totalPv = 0;
    for (ExpandedSwapLeg leg : swap.expand().getLegs()) {
      double pv = legValue(env, leg, paymentPeriodPricerFn::presentValue, paymentEventPricerFn::presentValue);
      totalPv += (pv * env.fxRate(leg.getCurrency(), currency));
    }
    return CurrencyAmount.of(currency, totalPv);
  }

  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapProduct swap) {
    return swapValue(env, swap.expand(), paymentPeriodPricerFn::presentValue, paymentEventPricerFn::presentValue);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapProduct swap) {
    return swapValue(env, swap.expand(), paymentPeriodPricerFn::futureValue, paymentEventPricerFn::futureValue);
  }

  //-------------------------------------------------------------------------
  // calculate present or future value for the swap
  private static MultiCurrencyAmount swapValue(
      PricingEnvironment env,
      ExpandedSwap swap,
      ToDoubleBiFunction<PricingEnvironment, PaymentPeriod> periodFn,
      ToDoubleBiFunction<PricingEnvironment, PaymentEvent> eventFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legValue(env, leg, periodFn, eventFn)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double pv = swap.getLegs().stream()
          .mapToDouble(leg -> legValue(env, leg, periodFn, eventFn))
          .sum();
      return MultiCurrencyAmount.of(currency, pv);
    }
  }

}
