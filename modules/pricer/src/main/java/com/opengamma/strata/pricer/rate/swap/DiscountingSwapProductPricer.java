/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer.legValue;
import static com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer.pvbp;
import static com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer.legValueSensitivity;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer for for rate swap products.
 * <p>
 * This function provides the ability to price a {@link SwapProduct}.
 * The product is priced by pricing each leg.
 */
public class DiscountingSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapProductPricer DEFAULT = new DiscountingSwapProductPricer(
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
  public DiscountingSwapProductPricer(
      PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer,
      PaymentEventPricer<PaymentEvent> paymentEventPricer) {
    this.paymentPeriodPricer = ArgChecker.notNull(paymentPeriodPricer, "paymentPeriodPricer");
    this.paymentEventPricer = ArgChecker.notNull(paymentEventPricer, "paymentEventPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap product, converted to the specified currency.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @return the present value of the swap product in the specified currency
   */
  public CurrencyAmount presentValue(PricingEnvironment env, SwapProduct product, Currency currency) {
    double totalPv = 0;
    for (ExpandedSwapLeg leg : product.expand().getLegs()) {
      double pv = legValue(env, leg, paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
      totalPv += (pv * env.fxRate(leg.getCurrency(), currency));
    }
    return CurrencyAmount.of(currency, totalPv);
  }

  /**
   * Calculates the present value of the swap product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted future value.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value of the swap product
   */
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapProduct product) {
    return swapValue(env, product.expand(), paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
  }

  /**
   * Calculates the future value of the swap product.
   * <p>
   * The future value of the product is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the swap product
   */
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapProduct product) {
    return swapValue(env, product.expand(), paymentPeriodPricer::futureValue, paymentEventPricer::futureValue);
  }

  //-------------------------------------------------------------------------
  // calculate present or future value for the swap
  private static MultiCurrencyAmount swapValue(
      PricingEnvironment env,
      ExpandedSwap swap,
      ToDoubleBiFunction<PricingEnvironment, PaymentPeriod> periodFn,
      ToDoubleBiFunction<PricingEnvironment, PaymentEvent> eventFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legValue(env, leg, periodFn, eventFn)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double pv = swap.getLegs().stream()
          .mapToDouble(leg -> legValue(env, leg, periodFn, eventFn))
          .sum();
      return MultiCurrencyAmount.of(currency, pv);
    }
  }
  
  /**
   * Computes the par rate for swaps with a fixed leg. 
   * <p>
   * The par rate is the common rate on all payments of the fixed leg for which the total swap present value is 0.
   * <p>
   * At least one leg should be a fixed leg. The par rate will be computed with respect to the first fixed leg.
   * All the payments in that leg should be fixed payments with a unique accrual period (no compounding) and no FX reset.
   * 
   * @param env  the pricing environment
   * @param product  the swap product for which the par rate should be computed
   * @return the par rate
   */
  public double parRate(PricingEnvironment env, SwapProduct product) {
    // find fixed leg
    ExpandedSwap swap = product.expand();
    List<ExpandedSwapLeg> legs = swap.getLegs();
    ExpandedSwapLeg fixedLeg = null;
    for(ExpandedSwapLeg leg: legs) {
      if (leg.getType() == SwapLegType.FIXED) {
        fixedLeg = leg;
        break;
      }
    }
    ArgChecker.notNull(fixedLeg, "no fixed leg found");
    @SuppressWarnings("null")
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ExpandedSwapLeg leg: legs) {
      if (leg != fixedLeg) {
        double pvLocal = legValue(env, leg, paymentPeriodPricer::presentValue, paymentEventPricer::presentValue);
        otherLegsConvertedPv += (pvLocal * env.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    double fixedLegEventsPv = fixedLeg.getPaymentEvents().stream()
        .filter(p -> !p.getPaymentDate().isBefore(env.getValuationDate()))
        .mapToDouble(e -> paymentEventPricer.presentValue(env, e))
        .sum();
    // PVBP
    double pvbpFixedLeg = pvbp(env, fixedLeg);
    // Par rate
    return -(otherLegsConvertedPv + fixedLegEventsPv) / pvbpFixedLeg;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivity(PricingEnvironment env, SwapProduct product) {
    return swapValueSensitivity(
        env,
        product.expand(),
        paymentPeriodPricer::presentValueSensitivity,
        paymentEventPricer::presentValueSensitivity);
  }

  /**
   * Calculates the future value sensitivity of the swap product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder futureValueSensitivity(PricingEnvironment env, SwapProduct product) {
    return swapValueSensitivity(
        env,
        product.expand(),
        paymentPeriodPricer::futureValueSensitivity,
        paymentEventPricer::futureValueSensitivity);
  }

  // calculate present or future value sensitivity for the swap
  private static PointSensitivityBuilder swapValueSensitivity(
      PricingEnvironment env,
      ExpandedSwap swap,
      BiFunction<PricingEnvironment, PaymentPeriod, PointSensitivityBuilder> periodFn,
      BiFunction<PricingEnvironment, PaymentEvent, PointSensitivityBuilder> eventFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      builder = builder.combinedWith(legValueSensitivity(env, leg, periodFn, eventFn));
    }
    return builder;
  }

}
