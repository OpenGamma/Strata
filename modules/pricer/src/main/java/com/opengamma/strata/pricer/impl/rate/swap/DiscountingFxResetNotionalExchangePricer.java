/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;

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
  public double presentValue(FxResetNotionalExchange event, RatesProvider provider) {
    // futureValue * discountFactor
    double df = provider.discountFactor(event.getCurrency(), event.getPaymentDate());
    return futureValue(event, provider) * df;
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(FxResetNotionalExchange event, RatesProvider provider) {
    DiscountFactors discountFactors = provider.discountFactors(event.getCurrency());
    PointSensitivityBuilder sensiDsc = discountFactors.pointSensitivity(event.getPaymentDate());
    sensiDsc = sensiDsc.multipliedBy(futureValue(event, provider));
    PointSensitivityBuilder sensiFx = futureValueSensitivity(event, provider);
    sensiFx = sensiFx.multipliedBy(discountFactors.discountFactor(event.getPaymentDate()));
    return sensiDsc.combinedWith(sensiFx);
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(FxResetNotionalExchange event, RatesProvider provider) {
    // notional * fxRate
    FxIndexRates rates = provider.fxIndexRates(event.getIndex());
    double fxRate = rates.rate(event.getReferenceCurrency(), event.getFixingDate());
    return event.getNotional() * fxRate;
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(FxResetNotionalExchange event, RatesProvider provider) {
    FxIndexRates rates = provider.fxIndexRates(event.getIndex());
    return rates.pointSensitivity(event.getReferenceCurrency(), event.getFixingDate())
        .multipliedBy(event.getNotional());
  }

}
