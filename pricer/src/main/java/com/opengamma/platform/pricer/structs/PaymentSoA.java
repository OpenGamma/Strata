/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.structs;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 * Class describing a generic payment.
 */
public abstract class PaymentSoA implements InstrumentDerivative {

  /**
   * The index currency.
   */
  private final Currency _currency;
  /**
   * The payment time.
   */
  private final double[] _paymentTime;
  /**
   * The funding curve name used in pricing.
   */
  private final String _fundingCurveName;

  /**
   * Constructor for a Payment.
   * @param currency The payment currency.
   * @param paymentTime Time (in years) up to the payment.
   */
  public PaymentSoA(Currency currency, double[] paymentTime) {
    ArgumentChecker.notNull(currency, "currency");
    ArgumentChecker.notNull(paymentTime, "paymentTime");
    for (int k = 0; k < paymentTime.length; k++)
    {
    	ArgumentChecker.isTrue(paymentTime[k] >= 0.0, "payment time < 0");
    }
    _currency = currency;
    _paymentTime = paymentTime.clone();
    _fundingCurveName = null;
  }

  /**
   * Gets the _paymentTime field.
   * @return the payment time
   */
  public double[] getPaymentTime() {
    return _paymentTime;
  }

  /**
   * Gets the _currency field.
   * @return The currency
   */
  public Currency getCurrency() {
    return _currency;
  }

  /**
   * Return a reference amount. For coupon it is the notional, for simple payments it is the paid amount. Used mainly to assess if the amount is paid or received.
   * @return The amount.
   */
  public abstract double[] getReferenceAmount();

  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Currency=");
    sb.append(_currency);
    sb.append(", payment time=");
    sb.append(_paymentTime);
    if (_fundingCurveName != null) {
      sb.append(", funding curve=");
      sb.append(_fundingCurveName);
    }
    return sb.toString();
  }

}
