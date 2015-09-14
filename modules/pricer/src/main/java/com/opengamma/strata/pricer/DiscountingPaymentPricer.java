/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;

/**
 * Pricer for simple payments.
 * <p>
 * This function provides the ability to price an {@link Payment}.
 */
public class DiscountingPaymentPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingPaymentPricer DEFAULT = new DiscountingPaymentPricer();

  /**
   * Creates an instance.
   */
  public DiscountingPaymentPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value of the payment by discounting.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * 
   * @param payment  the payment to price
   * @param discountFactors  the discount factors to price against
   * @return the present value
   */
  public CurrencyAmount presentValue(Payment payment, DiscountFactors discountFactors) {
    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    return payment.getValue().multipliedBy(discountFactors.discountFactor(payment.getDate()));
  }

  /**
   * Computes the present value of the payment with z-spread by discounting.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve. 
   * 
   * @param payment  the payment to price
   * @param discountFactors  the discount factors to price against
   * @param zSpread  the z-spread
   * @param periodic  if true, the spread is added to periodic compounded rates,
   *  if false, the spread is added to continuously compounded rates
   * @param periodsPerYear  the number of periods per year
   * @return the present value
   */
  public CurrencyAmount presentValue(
      Payment payment,
      DiscountFactors discountFactors,
      double zSpread,
      boolean periodic,
      int periodsPerYear) {

    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    double df = discountFactors.discountFactorWithSpread(payment.getDate(), zSpread, periodic, periodsPerYear);
    return payment.getValue().multipliedBy(df);
  }

  /**
   * Computes the present value of the payment by discounting.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * 
   * @param payment  the payment to price
   * @param provider  the rates provider
   * @return the present value
   */
  public CurrencyAmount presentValue(Payment payment, BaseProvider provider) {
    // duplicated code to avoid looking up in the provider when not necessary
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return payment.getValue().multipliedBy(discountFactors.discountFactor(payment.getDate()));
  }

  //-------------------------------------------------------------------------
  /**
   * Compute the present value curve sensitivity of the payment.
   * <p>
   * The present value sensitivity of the payment is the sensitivity of the
   * present value to the discount factor curve.
   * There is no sensitivity if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * 
   * @param payment  the payment to price
   * @param discountFactors  the discount factors to price against
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivity(Payment payment, DiscountFactors discountFactors) {
    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return PointSensitivityBuilder.none();
    }
    return discountFactors.zeroRatePointSensitivity(payment.getDate()).multipliedBy(payment.getAmount());
  }

  /**
   * Compute the present value curve sensitivity of the payment with z-spread.
   * <p>
   * The present value sensitivity of the payment is the sensitivity of the
   * present value to the discount factor curve.
   * There is no sensitivity if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve. 
   * 
   * @param payment  the payment to price
   * @param discountFactors  the discount factors to price against
   * @param zSpread  the z-spread
   * @param periodic  if true, the spread is added to periodic compounded rates,
   *  if false, the spread is added to continuously compounded rates
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivity(
      Payment payment, 
      DiscountFactors discountFactors,
      double zSpread,
      boolean periodic,
      int periodsPerYear) {

    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return PointSensitivityBuilder.none();
    }
    ZeroRateSensitivity sensi =
        discountFactors.zeroRatePointSensitivityWithSpread(payment.getDate(), zSpread, periodic, periodsPerYear);
    return sensi.multipliedBy(payment.getAmount());
  }

  /**
   * Compute the present value curve sensitivity of the payment.
   * <p>
   * The present value sensitivity of the payment is the sensitivity of the
   * present value to the discount factor curve.
   * There is no sensitivity if the payment date is before the valuation date.
   * 
   * @param payment  the payment to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivity(Payment payment, BaseProvider provider) {
    // duplicated code to avoid looking up in the provider when not necessary
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return PointSensitivityBuilder.none();
    }
    DiscountFactors discountFactors = provider.discountFactors(payment.getCurrency());
    return discountFactors.zeroRatePointSensitivity(payment.getDate()).multipliedBy(payment.getAmount());
  }

}
