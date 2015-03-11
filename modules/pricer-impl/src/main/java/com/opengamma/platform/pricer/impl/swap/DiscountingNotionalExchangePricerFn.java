/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.basics.currency.Currency;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.NotionalExchange;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;

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

  @Override
  public Pair<Double, MulticurveSensitivity3LD> presentValueCurveSensitivity3LD(PricingEnvironment env,
      NotionalExchange event) {
    Currency ccy = event.getCurrency();
    double paymentTime = env.relativeTime(event.getPaymentDate());
    double df = env.discountFactor(ccy, event.getPaymentDate());
    List<ZeroRateSensitivityLD> listDiscounting = new ArrayList<>();
    listDiscounting.add(new ZeroRateSensitivityLD(ccy, event.getPaymentDate(), -paymentTime * df *
        event.getPaymentAmount().getAmount(), ccy));
    MulticurveSensitivity3LD sensi = MulticurveSensitivity3LD.ofZeroRate(listDiscounting);
    return Pair.of(event.getPaymentAmount().getAmount() * df, sensi);
  }

}
