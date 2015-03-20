/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.platform.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;

/**
 * Pricer implementation for the exchange of FX reset notionals.
 * <p>
 * The FX reset notional exchange is priced by discounting the value of the exchange.
 * The value of the exchange is calculated by performing an FX conversion on the amount.
 */
public class DiscountingFxResetNotionalExchangePricerFn
    implements PaymentEventPricerFn<FxResetNotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingFxResetNotionalExchangePricerFn DEFAULT =
      new DiscountingFxResetNotionalExchangePricerFn();

  /**
   * Creates an instance.
   */
  public DiscountingFxResetNotionalExchangePricerFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, FxResetNotionalExchange event) {
    // futureValue * discountFactor
    double df = env.discountFactor(event.getCurrency(), event.getPaymentDate());
    return futureValue(env, event) * df;
  }

  @Override
  public double futureValue(PricingEnvironment env, FxResetNotionalExchange event) {
    // notional * fxRate
    double fxRate = env.fxIndexRate(event.getIndex(), event.getReferenceCurrency(), event.getFixingDate());
    return event.getNotional() * fxRate;
  }

}
