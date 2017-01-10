/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentPeriodPricer;
import com.opengamma.strata.product.swap.KnownAmountSwapPaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Pricer implementation for payment periods using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingSwapPaymentPeriodPricer
    implements SwapPaymentPeriodPricer<SwapPaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DispatchingSwapPaymentPeriodPricer DEFAULT = new DispatchingSwapPaymentPeriodPricer(
      DiscountingRatePaymentPeriodPricer.DEFAULT,
      DiscountingKnownAmountPaymentPeriodPricer.DEFAULT);

  /**
   * Pricer for {@link RatePaymentPeriod}.
   */
  private final SwapPaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer;
  /**
   * Pricer for {@link KnownAmountSwapPaymentPeriod}.
   */
  private final SwapPaymentPeriodPricer<KnownAmountSwapPaymentPeriod> knownAmountPaymentPeriodPricer;

  /**
   * Creates an instance.
   * 
   * @param ratePaymentPeriodPricer  the pricer for {@link RatePaymentPeriod}
   * @param knownAmountPaymentPeriodPricer  the pricer for {@link KnownAmountSwapPaymentPeriod}
   */
  public DispatchingSwapPaymentPeriodPricer(
      SwapPaymentPeriodPricer<RatePaymentPeriod> ratePaymentPeriodPricer,
      SwapPaymentPeriodPricer<KnownAmountSwapPaymentPeriod> knownAmountPaymentPeriodPricer) {
    this.ratePaymentPeriodPricer = ArgChecker.notNull(ratePaymentPeriodPricer, "ratePaymentPeriodPricer");
    this.knownAmountPaymentPeriodPricer = ArgChecker.notNull(knownAmountPaymentPeriodPricer, "knownAmountPaymentPeriodPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.presentValue((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.presentValue((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(SwapPaymentPeriod paymentPeriod,
      RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.presentValueSensitivity((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.presentValueSensitivity((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double forecastValue(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.forecastValue((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.forecastValue((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(SwapPaymentPeriod paymentPeriod,
      RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.forecastValueSensitivity((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.forecastValueSensitivity((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double pvbp(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.pvbp((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.pvbp((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public PointSensitivityBuilder pvbpSensitivity(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.pvbpSensitivity((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.pvbpSensitivity((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public double accruedInterest(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.accruedInterest((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.accruedInterest((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(SwapPaymentPeriod paymentPeriod, RatesProvider provider, ExplainMapBuilder builder) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      ratePaymentPeriodPricer.explainPresentValue((RatePaymentPeriod) paymentPeriod, provider, builder);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      knownAmountPaymentPeriodPricer.explainPresentValue((KnownAmountSwapPaymentPeriod) paymentPeriod, provider, builder);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.currencyExposure((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.currencyExposure((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public double currentCash(SwapPaymentPeriod paymentPeriod, RatesProvider provider) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricer.currentCash((RatePaymentPeriod) paymentPeriod, provider);
    } else if (paymentPeriod instanceof KnownAmountSwapPaymentPeriod) {
      return knownAmountPaymentPeriodPricer.currentCash((KnownAmountSwapPaymentPeriod) paymentPeriod, provider);
    } else {
      throw new IllegalArgumentException("Unknown PaymentEvent type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

}
