/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

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
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the present value
   */
  public CurrencyAmount presentValue(Payment payment, BaseProvider provider) {
    // duplicated code to avoid looking up in the provider when not necessary
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    double df = provider.discountFactor(payment.getCurrency(), payment.getDate());
    return payment.getValue().multipliedBy(df);
  }

  /**
   * Computes the present value of the payment by discounting.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * 
   * @param payment  the payment
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
   * Computes the present value of the payment by discounting.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the present value
   */
  public double presentValueAmount(Payment payment, BaseProvider provider) {
    // duplicated code to avoid looking up in the provider when not necessary
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return 0d;
    }
    double df = provider.discountFactor(payment.getCurrency(), payment.getDate());
    return payment.getAmount() * df;
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
   * @param payment  the payment
   * @param discountFactors  the discount factors to price against
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value
   */
  public CurrencyAmount presentValueWithSpread(
      Payment payment,
      DiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    double df = discountFactors.discountFactorWithSpread(payment.getDate(), zSpread, compoundedRateType, periodsPerYear);
    return payment.getValue().multipliedBy(df);
  }

  /**
   * Explains the present value of the payment.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(Payment payment, BaseProvider provider) {
    Currency currency = payment.getCurrency();
    LocalDate paymentDate = payment.getDate();

    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ENTRY_TYPE, "Payment");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    if (paymentDate.isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, forecastValue(payment, provider));
      builder.put(ExplainKey.PRESENT_VALUE, presentValue(payment, provider));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Compute the present value curve sensitivity of the payment.
   * <p>
   * The present value sensitivity of the payment is the sensitivity of the
   * present value to the discount factor curve.
   * There is no sensitivity if the payment date is before the valuation date.
   * 
   * @param payment  the payment
   * @param provider  the provider
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

  /**
   * Compute the present value curve sensitivity of the payment.
   * <p>
   * The present value sensitivity of the payment is the sensitivity of the
   * present value to the discount factor curve.
   * There is no sensitivity if the payment date is before the valuation date.
   * <p>
   * The specified discount factors should be for the payment currency, however this is not validated.
   * 
   * @param payment  the payment
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
   * @param payment  the payment
   * @param discountFactors  the discount factors to price against
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivityWithSpread(
      Payment payment,
      DiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (discountFactors.getValuationDate().isAfter(payment.getDate())) {
      return PointSensitivityBuilder.none();
    }
    ZeroRateSensitivity sensi =
        discountFactors.zeroRatePointSensitivityWithSpread(payment.getDate(), zSpread, compoundedRateType, periodsPerYear);
    return sensi.multipliedBy(payment.getAmount());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the forecast value of the payment.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the forecast value
   */
  public CurrencyAmount forecastValue(Payment payment, BaseProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return CurrencyAmount.zero(payment.getCurrency());
    }
    return payment.getValue();
  }

  /**
   * Computes the forecast value of the payment.
   * <p>
   * The present value is zero if the payment date is before the valuation date.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the forecast value
   */
  public double forecastValueAmount(Payment payment, BaseProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return 0d;
    }
    return payment.getAmount();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flow of the payment.
   * <p>
   * The cash flow is returned, empty if the payment has already occurred.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the cash flow, empty if the payment has occurred
   */
  public CashFlows cashFlows(Payment payment, BaseProvider provider) {
    if (provider.getValuationDate().isAfter(payment.getDate())) {
      return CashFlows.NONE;
    }
    double df = provider.discountFactor(payment.getCurrency(), payment.getDate());
    CashFlow flow = CashFlow.ofForecastValue(payment.getDate(), payment.getCurrency(), payment.getAmount(), df);
    return CashFlows.of(flow);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(Payment payment, BaseProvider provider) {
    return MultiCurrencyAmount.of(presentValue(payment, provider));
  }

  /**
   * Calculates the current cash.
   * 
   * @param payment  the payment
   * @param provider  the provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(Payment payment, BaseProvider provider) {
    if (payment.getDate().isEqual(provider.getValuationDate())) {
      return payment.getValue();
    }
    return CurrencyAmount.zero(payment.getCurrency());
  }

}
