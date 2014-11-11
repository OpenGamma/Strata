/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;
import com.opengamma.platform.pricer.swap.SwapPricerFn;

/**
 * Pricer for swaps.
 */
public class StandardSwapPricerFn implements SwapPricerFn {

  /**
   * Default implementation.
   */
  public static final StandardSwapPricerFn DEFAULT = new StandardSwapPricerFn(
      StandardPaymentPeriodPricerFn.DEFAULT);

  /**
   * Payment period pricer.
   */
  private final PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricerFn  the pricer for {@link PaymentPeriod}
   */
  public StandardSwapPricerFn(
      PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn) {
    this.paymentPeriodPricerFn = ArgChecker.notNull(paymentPeriodPricerFn, "paymentPeriodPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, LocalDate valuationDate, SwapTrade trade) {
    if (trade.getSwap().isCrossCurrency()) {
      throw new UnsupportedOperationException();
    }
    Currency currency = trade.getSwap().getLeg(0).getCurrency();
    double pv = trade.getSwap().getLegs().stream()
      .flatMap(leg -> leg.toExpanded().getPaymentPeriods().stream())
      .map(p -> (RatePaymentPeriod) p)  // TODO
      .mapToDouble(p -> paymentPeriodPricerFn.presentValue(env, valuationDate, p))
      .sum();
    return CurrencyAmount.of(currency, pv);
  }

}
