/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer for for rate swap legs.
 * <p>
 * This function provides the ability to price a {@link SwapLeg}.
 * The product is priced by pricing each period and event.
 */
public class DiscountingSwapLegPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapLegPricer DEFAULT = new DiscountingSwapLegPricer(
      PaymentPeriodPricer.instance(),
      PaymentEventPricer.instance());

  /**
   * Pricer for {@link PaymentPeriod}.
   */
  private final PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer;
  /**
   * Pricer for {@link PaymentEvent}.
   */
  private final PaymentEventPricer<PaymentEvent> paymentEventPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricer  the pricer for {@link PaymentPeriod}
   * @param paymentEventPricer  the pricer for {@link PaymentEvent}
   */
  public DiscountingSwapLegPricer(
      PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer,
      PaymentEventPricer<PaymentEvent> paymentEventPricer) {
    this.paymentPeriodPricer = ArgChecker.notNull(paymentPeriodPricer, "paymentPeriodPricer");
    this.paymentEventPricer = ArgChecker.notNull(paymentEventPricer, "paymentEventPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap leg, converted to the specified currency.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param leg  the leg to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap leg in the specified currency
   */
  public CurrencyAmount presentValue(SwapLeg leg, Currency currency, RatesProvider provider) {
    double pv = legValue(leg.expand(), provider, paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
    return CurrencyAmount.of(currency, (pv * provider.fxRate(leg.getCurrency(), currency)));
  }

  /**
   * Calculates the present value of the swap leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted future value.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the present value of the swap leg
   */
  public CurrencyAmount presentValue(SwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), presentValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  double presentValueInternal(SwapLeg leg, RatesProvider provider) {
    return legValue(leg.expand(), provider, paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
  }

  /**
   * Calculates the future value of the swap leg.
   * <p>
   * The future value of the leg is the value on the valuation date without present value discounting.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the future value of the swap leg
   */
  public CurrencyAmount futureValue(SwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), futureValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  double futureValueInternal(SwapLeg leg, RatesProvider provider) {
    return legValue(leg.expand(), provider, paymentPeriodPricer::futureValue, paymentEventPricer::futureValue);
  }

  // calculate present or future value for a leg
  private double legValue(
      ExpandedSwapLeg leg,
      RatesProvider provider,
      ToDoubleBiFunction<PaymentPeriod, RatesProvider> periodFn,
      ToDoubleBiFunction<PaymentEvent, RatesProvider> eventFn) {

    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += periodFn.applyAsDouble(period, provider);
      }
    }
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += eventFn.applyAsDouble(event, provider);
      }
    }
    return total;
  }

  // calculates the present value in the currency of the swap leg
  double presentValueEventsInternal(SwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentEvent event : leg.expand().getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.presentValue(event, provider);
      }
    }
    return total;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the Present Value of a Basis Point for a fixed swap leg. 
   * <p>
   * The Present Value of a Basis Point is the value of the leg when the rate is equal to 1.
   * A better name would be "Present Value of 1".
   * The quantity is also known as "physical annuity" or "level".
   * <p>
   * All the payments periods must be of type {@link RatePaymentPeriod}.
   * Each period must have a fixed rate, no FX reset and no compounding.
   * 
   * @param fixedLeg  the swap fixed leg
   * @param provider  the rates provider
   * @return the Present Value of a Basis Point
   */
  public double pvbp(SwapLeg fixedLeg, RatesProvider provider) {
    double pvbpFixedLeg = 0.0;
    for (PaymentPeriod period : fixedLeg.expand().getPaymentPeriods()) {
      ArgChecker.isTrue(period instanceof RatePaymentPeriod, "PaymentPeriod must be instance of RatePaymentPeriod");
      pvbpFixedLeg += pvbpPayment((RatePaymentPeriod) period, provider);
    }
    return pvbpFixedLeg;
  }

  // computes Present Value of a Basis Point for fixed payment with a unique accrual period (no compounding) and 
  // no FX reset.
  private double pvbpPayment(RatePaymentPeriod paymentPeriod, RatesProvider provider) {
    ArgChecker.isTrue(!paymentPeriod.getFxReset().isPresent(), "FX reset is not supported");
    ArgChecker.isTrue(paymentPeriod.getAccrualPeriods().size() == 1, "Compounding is not supported");
    RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
    ArgChecker.isTrue(
        accrualPeriod.getRateObservation() instanceof FixedRateObservation,
        "RateObservation must be instance of FixedRateObservation");
    double df = provider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
    return df * accrualPeriod.getYearFraction() * paymentPeriod.getNotional();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder presentValueSensitivity(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        expanded,
        provider,
        paymentPeriodPricer::presentValueSensitivity,
        paymentEventPricer::presentValueSensitivity);
  }

  /**
   * Calculates the future value sensitivity of the swap leg.
   * <p>
   * The future value sensitivity of the leg is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the future value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder futureValueSensitivity(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        expanded,
        provider,
        paymentPeriodPricer::futureValueSensitivity,
        paymentEventPricer::futureValueSensitivity);
  }

  // calculate present or future value sensitivity for a leg
  private PointSensitivityBuilder legValueSensitivity(
      ExpandedSwapLeg leg,
      RatesProvider provider,
      BiFunction<PaymentPeriod, RatesProvider, PointSensitivityBuilder> periodFn,
      BiFunction<PaymentEvent, RatesProvider, PointSensitivityBuilder> eventFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(periodFn.apply(period, provider));
      }
    }
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(eventFn.apply(event, provider));
      }
    }
    return builder;
  }

}
