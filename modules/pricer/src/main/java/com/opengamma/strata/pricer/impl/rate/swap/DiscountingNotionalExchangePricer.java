/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;

/**
 * Pricer implementation for the exchange of notionals.
 * <p>
 * The notional exchange is priced by discounting the value of the exchange.
 */
public class DiscountingNotionalExchangePricer
    implements PaymentEventPricer<NotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingNotionalExchangePricer DEFAULT = new DiscountingNotionalExchangePricer();

  /**
   * Creates an instance.
   */
  public DiscountingNotionalExchangePricer() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(NotionalExchange event, RatesProvider provider) {
    // futureValue * discountFactor
    double df = provider.discountFactor(event.getPaymentAmount().getCurrency(), event.getPaymentDate());
    return futureValue(event, provider) * df;
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(NotionalExchange event, RatesProvider provider) {
    DiscountFactors discountFactors = provider.discountFactors(event.getCurrency());
    return discountFactors.pointSensitivity(event.getPaymentDate())
        .multipliedBy(event.getPaymentAmount().getAmount());
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(NotionalExchange event, RatesProvider provider) {
    // paymentAmount
    return event.getPaymentAmount().getAmount();
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(NotionalExchange event, RatesProvider provider) {
    return PointSensitivityBuilder.none();
  }

}
