/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;
import com.opengamma.platform.pricer.swap.SwapPricerFn;

/**
 * Pricer for swaps.
 */
public class DefaultSwapPricerFn implements SwapPricerFn {

  /**
   * Default implementation.
   */
  public static final DefaultSwapPricerFn DEFAULT = new DefaultSwapPricerFn(
      DefaultPaymentPeriodPricerFn.DEFAULT);

  /**
   * Payment period pricer.
   */
  private final PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricerFn  the pricer for {@link PaymentPeriod}
   */
  public DefaultSwapPricerFn(
      PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn) {
    this.paymentPeriodPricerFn = ArgChecker.notNull(paymentPeriodPricerFn, "paymentPeriodPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, LocalDate valuationDate, Swap swap) {
    return swap.getLegs().stream()
      .flatMap(leg -> leg.toExpanded().getPaymentPeriods().stream())
      .map(p -> CurrencyAmount.of(p.getCurrency(), paymentPeriodPricerFn.presentValue(env, valuationDate, p)))
      .reduce(MultiCurrencyAmount.of(), MultiCurrencyAmount::plus, MultiCurrencyAmount::plus);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, LocalDate valuationDate, Swap swap) {
    return swap.getLegs().stream()
      .flatMap(leg -> leg.toExpanded().getPaymentPeriods().stream())
      .map(p -> CurrencyAmount.of(p.getCurrency(), paymentPeriodPricerFn.futureValue(env, valuationDate, p)))
      .reduce(MultiCurrencyAmount.of(), MultiCurrencyAmount::plus, MultiCurrencyAmount::plus);
  }

}
