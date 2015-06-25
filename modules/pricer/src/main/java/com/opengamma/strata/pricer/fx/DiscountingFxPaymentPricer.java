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

/**
 * Pricer for simple payments.
 * <p>
 * This function provides the ability to price an {@link FxPayment}.
 */
public class DiscountingFxPaymentPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxPaymentPricer DEFAULT = new DiscountingFxPaymentPricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxPaymentPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value of the payment by discounting. 
   * 
   * @param payment  the payment to price
   * @param provider  the rates provider
   * @return the present value
   */
  public CurrencyAmount presentValue(FxPayment payment, RatesProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getPaymentDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return payment.getValue().multipliedBy(discountFactors.discountFactor(payment.getPaymentDate()));
  }

  /**
   * Compute the present value curve sensitivity of the payment.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param payment  the payment to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivity(FxPayment payment, final RatesProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return discountFactors.zeroRatePointSensitivity(payment.getPaymentDate()).multipliedBy(payment.getAmount());
  }

}
