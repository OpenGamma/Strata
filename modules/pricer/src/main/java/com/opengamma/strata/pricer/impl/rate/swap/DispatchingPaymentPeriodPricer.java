/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;

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
      DiscountingRatePaymentPeriodPricer.DEFAULT,
      DiscountingKnownAmountPaymentPeriodPricer.DEFAULT);

  /**
   * Pricer for {@link RatePaymentPeriod}.
   */
  private final PaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer;
  /**
   * Pricer for {@link KnownAmountPaymentPeriod}.
   */
  private final PaymentPeriodPricer<KnownAmountPaymentPeriod> knownAmountPaymentPeriodPricer;

  /**
   * Creates an instance.
   * 
   * @param ratePaymentPeriodPricer  the pricer for {@link RatePaymentPeriod}
   * @param knownAmountPaymentPeriodPricer  the pricer for {@link KnownAmountPaymentPeriod}
   */
  public DispatchingPaymentPeriodPricer(
      PaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer,
      PaymentPeriodPricer<KnownAmountPaymentPeriod> knownAmountPaymentPeriodPricer) {
    this.ratePaymentPeriodPricer = ArgChecker.notNull(ratePaymentPeriodPricer, "ratePaymentPeriodPricer");
    this.knownAmountPaymentPeriodPricer = ArgChecker.notNull(knownAmountPaymentPeriodPricer, "knownAmountPaymentPeriodPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.presentValue((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.presentValue((KnownAmountPaymentPeriod) paymentPeriod, provider);
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
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.presentValueSensitivity((KnownAmountPaymentPeriod) paymentPeriod, provider);
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
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.futureValue((KnownAmountPaymentPeriod) paymentPeriod, provider);
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
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.futureValueSensitivity((KnownAmountPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double accruedInterest(PaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.accruedInterest((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.accruedInterest((KnownAmountPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(PaymentPeriod paymentPeriod, RatesProvider provider, ExplainMapBuilder builder) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      ratePaymentPeriodPricer.explainPresentValue((RatePaymentPeriod) paymentPeriod, provider, builder);
    } else if (paymentPeriod instanceof KnownAmountPaymentPeriod) {
      knownAmountPaymentPeriodPricer.explainPresentValue((KnownAmountPaymentPeriod) paymentPeriod, provider, builder);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

}
