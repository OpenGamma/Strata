/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer implementation for payment periods using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingPaymentPeriodPricer
    implements PaymentPeriodPricer<PaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DispatchingPaymentPeriodPricer DEFAULT = new DispatchingPaymentPeriodPricer(
      DiscountingRatePaymentPeriodPricer.DEFAULT);

  /**
   * Pricer for {@link RatePaymentPeriod}.
   */
  private final PaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer;

  /**
   * Creates an instance.
   * 
   * @param ratePaymentPeriodPricer  the pricer for {@link RatePaymentPeriod}
   */
  public DispatchingPaymentPeriodPricer(
      PaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer) {
    this.ratePaymentPeriodPricer = ArgChecker.notNull(ratePaymentPeriodPricer, "ratePaymentPeriodPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.presentValue((RatePaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(PaymentPeriod paymentPeriod,
      RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.presentValueSensitivity((RatePaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(PaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.futureValue((RatePaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder futureValueSensitivity(PaymentPeriod paymentPeriod,
      RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.futureValueSensitivity((RatePaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

}
