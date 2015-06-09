/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;

public class DiscountingFxPaymentPricer {

  /**
   * Default implementation.
   */
  private static final DiscountingFxPaymentPricer DEFALUT = new DiscountingFxPaymentPricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxPaymentPricer() {
  }

  public CurrencyAmount presentValue(FxPayment payment, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getPaymentDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    double discountFactor = provider.discountFactor(payment.getCurrency(), payment.getPaymentDate());
    return payment.getValue().multipliedBy(discountFactor);
  }

  public PointSensitivityBuilder presentValueSensitivity(FxPayment payment, final RatesProvider provider) {
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return discountFactors.zeroRatePointSensitivity(payment.getPaymentDate())
        .multipliedBy(payment.getAmount());
  }
}
