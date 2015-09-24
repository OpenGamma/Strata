/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value of an {@code BulletPaymentTrade} for each of a set of scenarios.
 */
public class BulletPaymentPvFunction
    extends AbstractBulletPaymentFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(Payment product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
