/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer implementation for the exchange of FX reset notionals.
 * <p>
 * The FX reset notional exchange is priced by discounting the value of the exchange.
 * The value of the exchange is calculated by performing an FX conversion on the amount.
 */
public class DiscountingFxResetNotionalExchangePricer
    implements PaymentEventPricer<FxResetNotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingFxResetNotionalExchangePricer DEFAULT =
      new DiscountingFxResetNotionalExchangePricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxResetNotionalExchangePricer() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(RatesProvider provider, FxResetNotionalExchange event) {
    // futureValue * discountFactor
    double df = provider.discountFactor(event.getCurrency(), event.getPaymentDate());
    return futureValue(provider, event) * df;
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(RatesProvider provider, FxResetNotionalExchange event) {
    throw new UnsupportedOperationException();  // TODO
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(RatesProvider provider, FxResetNotionalExchange event) {
    // notional * fxRate
    double fxRate = provider.fxIndexRate(event.getIndex(), event.getReferenceCurrency(), event.getFixingDate());
    return event.getNotional() * fxRate;
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(RatesProvider provider, FxResetNotionalExchange event) {
    throw new UnsupportedOperationException();  // TODO
  }

}
