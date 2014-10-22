/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.structs;

import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 * Class describing a generic coupon.
 */
public abstract class CouponSoA extends PaymentSoA {

  /**
   * The payment period year fraction (or accrual factor).
   */
  private final double[] _paymentAccrualFactor;
  /**
   * The coupon notional.
   */
  private final double[] _notional;

  /**
   * Constructor of a generic coupon from details.
   * @param currency The payment currency.
   * @param paymentTime Time (in years) up to the payment.
   * @param paymentAccrualFactor The accrual factor (or year fraction) for the coupon payment.
   * @param notional Coupon notional.
   */
  public CouponSoA(final Currency currency, double[] paymentTime, double[] paymentAccrualFactor, double[] notional) {
    super(currency, paymentTime);
    ArgumentChecker.notNull(paymentAccrualFactor, "paymentAccrualFactor");
    for (int k = 0; k < paymentAccrualFactor.length; k++)
    {
    	ArgumentChecker.isTrue(paymentAccrualFactor[k] >= 0, "year fraction < 0");
    }
    
    _paymentAccrualFactor = paymentAccrualFactor.clone();
    _notional = notional.clone();
  }

  /**
   * Gets the payment year fraction (or accrual factor).
   * @return The payment year fraction.
   */
  public double[] getPaymentYearFraction() {
    return _paymentAccrualFactor;
  }

  /**
   * Gets the coupon notional.
   * @return The notional.
   */
  public double[] getNotional() {
    return _notional;
  }


  @Override
  public double[] getReferenceAmount() {
    return _notional;
  }

  @Override
  public String toString() {
    return super.toString() + ", year fraction = " + getPaymentYearFraction() + ", notional = " + _notional;
  }



}
