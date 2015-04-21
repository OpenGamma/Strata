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
import com.opengamma.strata.pricer.PricingEnvironment;
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
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @param currency  the currency to convert to
   * @return the present value of the swap leg in the specified currency
   */
  public CurrencyAmount presentValue(PricingEnvironment env, SwapLeg leg, Currency currency) {
    double pv = legValue(env, leg.expand(), paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
    return CurrencyAmount.of(currency, (pv * env.fxRate(leg.getCurrency(), currency)));
  }

  /**
   * Calculates the present value of the swap leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted future value.
   * The result is returned using the payment currency of the leg.
   * 
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @return the present value of the swap leg
   */
  public CurrencyAmount presentValue(PricingEnvironment env, SwapLeg leg) {
    double val = legValue(env, leg.expand(), paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
    return CurrencyAmount.of(leg.getCurrency(), val);
  }

  /**
   * Calculates the future value of the swap leg.
   * <p>
   * The future value of the leg is the value on the valuation date without present value discounting.
   * The result is returned using the payment currency of the leg.
   * 
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @return the future value of the swap leg
   */
  public CurrencyAmount futureValue(PricingEnvironment env, SwapLeg leg) {
    double val = legValue(env, leg.expand(), paymentPeriodPricer::futureValue, paymentEventPricer::futureValue);
    return CurrencyAmount.of(leg.getCurrency(), val);
  }
  
  /**
   * Computes the Present Value of a Basis Point for a fixed swap leg. 
   * <p>
   * The Present Value of a Basis Point is the value of the leg when the rate is equal to 1. A better name would
   * be "Present Value of 1". The quantity is also known as "physical annuity" or "level".
   * 
   * All the payments in the fixed leg should be fixed payments with a unique accrual period (no compounding) and no FX reset.
   * 
   * @param env  the pricing environment
   * @param fixedLeg  the swap fixed leg
   * @return the par rate
   */
  public static double pvbp(PricingEnvironment env, SwapLeg fixedLeg) {
    double pvbpFixedLeg = 0.0;
    for(PaymentPeriod p: fixedLeg.expand().getPaymentPeriods()) {
      RatePaymentPeriod rp = (RatePaymentPeriod) p;
      pvbpFixedLeg += pvbpPayment(env, rp);
    }
    return pvbpFixedLeg;    
  }
  
  // computes Present Value of a Basis Point for fixed payment with a unique accrual period (no compounding) and 
  // no FX reset.
  private static double pvbpPayment(PricingEnvironment env, RatePaymentPeriod paymentPeriod) {
    double df = env.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
    ArgChecker.isTrue(!paymentPeriod.getFxReset().isPresent());
    double notional = paymentPeriod.getNotional();
    ArgChecker.isTrue(paymentPeriod.getAccrualPeriods().size() == 1);
    RateAccrualPeriod accrualPeriod = paymentPeriod.getAccrualPeriods().get(0);
    ArgChecker.isTrue(accrualPeriod.getRateObservation() instanceof FixedRateObservation);
    return df * accrualPeriod.getYearFraction() * notional;
  }

  //-------------------------------------------------------------------------
  // calculate present or future value for a leg
  static double legValue(
      PricingEnvironment env,
      ExpandedSwapLeg leg,
      ToDoubleBiFunction<PricingEnvironment, PaymentPeriod> periodFn,
      ToDoubleBiFunction<PricingEnvironment, PaymentEvent> eventFn) {

    double valuePeriods = leg.getPaymentPeriods().stream()
        .filter(p -> !p.getPaymentDate().isBefore(env.getValuationDate()))
        .mapToDouble(p -> periodFn.applyAsDouble(env, p))
        .sum();
    double valueEvents = leg.getPaymentEvents().stream()
        .filter(p -> !p.getPaymentDate().isBefore(env.getValuationDate()))
        .mapToDouble(e -> eventFn.applyAsDouble(env, e))
        .sum();
    return valuePeriods + valueEvents;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @return the present value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder presentValueSensitivity(PricingEnvironment env, SwapLeg leg) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        env,
        expanded,
        paymentPeriodPricer::presentValueSensitivity,
        paymentEventPricer::presentValueSensitivity);
  }

  /**
   * Calculates the future value sensitivity of the swap leg.
   * <p>
   * The future value sensitivity of the leg is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @return the future value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder futureValueSensitivity(PricingEnvironment env, SwapLeg leg) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        env,
        expanded,
        paymentPeriodPricer::futureValueSensitivity,
        paymentEventPricer::futureValueSensitivity);
  }

  // calculate present or future value sensitivity for a leg
  static PointSensitivityBuilder legValueSensitivity(
      PricingEnvironment env,
      ExpandedSwapLeg leg,
      BiFunction<PricingEnvironment, PaymentPeriod, PointSensitivityBuilder> periodFn,
      BiFunction<PricingEnvironment, PaymentEvent, PointSensitivityBuilder> eventFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(env.getValuationDate())) {
        builder = builder.combinedWith(periodFn.apply(env, period));
      }
    }
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(env.getValuationDate())) {
        builder = builder.combinedWith(eventFn.apply(env, event));
      }
    }
    return builder;
  }

}
