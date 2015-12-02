/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.PaymentPeriodPricer;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;

/**
 * Pricer implementation for swap payment periods based on a known amount.
 * <p>
 * This pricer performs discounting of the known amount.
 */
public class DiscountingKnownAmountPaymentPeriodPricer
    implements PaymentPeriodPricer<KnownAmountPaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DiscountingKnownAmountPaymentPeriodPricer DEFAULT = new DiscountingKnownAmountPaymentPeriodPricer(
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Payment pricer.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the payment pricer
   */
  public DiscountingKnownAmountPaymentPeriodPricer(DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(KnownAmountPaymentPeriod period, RatesProvider provider) {
    return paymentPricer.presentValue(period.getPayment(), provider).getAmount();
  }

  @Override
  public double forecastValue(KnownAmountPaymentPeriod period, RatesProvider provider) {
    if (period.getPaymentDate().isBefore(provider.getValuationDate())) {
      return 0;
    }
    return period.getPayment().getAmount();
  }

  @Override
  public double presentValueCashFlowEquivalent(KnownAmountPaymentPeriod period, RatesProvider provider) {
    return presentValue(period, provider);
  }

  @Override
  public double accruedInterest(KnownAmountPaymentPeriod period, RatesProvider provider) {
    // no day count available, so return the simple day-based fraction
    LocalDate valDate = provider.getValuationDate();
    if (valDate.compareTo(period.getStartDate()) <= 0 || valDate.compareTo(period.getEndDate()) > 0) {
      return 0d;
    }
    double fv = forecastValue(period, provider);
    double totalDays = period.getStartDate().until(period.getEndDate(), DAYS);
    double partialDays = period.getStartDate().until(valDate, DAYS);
    return fv * (partialDays / totalDays);
  }

  @Override
  public double pvbp(KnownAmountPaymentPeriod period, RatesProvider provider) {
    throw new UnsupportedOperationException("Unable to calculate PVBP for KnownAmountPaymentPeriod");
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder presentValueSensitivity(KnownAmountPaymentPeriod period, RatesProvider provider) {
    return paymentPricer.presentValueSensitivity(period.getPayment(), provider);
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(KnownAmountPaymentPeriod period, RatesProvider provider) {
    return PointSensitivityBuilder.none();
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivityCashFlowEquivalent(
      KnownAmountPaymentPeriod period,
      RatesProvider provider) {
    return paymentPricer.presentValueSensitivity(period.getPayment(), provider);
  }

  @Override
  public PointSensitivityBuilder pvbpSensitivity(KnownAmountPaymentPeriod period, RatesProvider provider) {
    throw new UnsupportedOperationException("Unable to calculate PVBP for KnownAmountPaymentPeriod");
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(KnownAmountPaymentPeriod period, RatesProvider provider, ExplainMapBuilder builder) {
    Currency currency = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();

    builder.put(ExplainKey.ENTRY_TYPE, "KnownAmountPaymentPeriod");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.START_DATE, period.getStartDate());
    builder.put(ExplainKey.UNADJUSTED_START_DATE, period.getUnadjustedStartDate());
    builder.put(ExplainKey.END_DATE, period.getEndDate());
    builder.put(ExplainKey.ACCRUAL_DAYS, (int) DAYS.between(period.getStartDate(), period.getEndDate()));
    builder.put(ExplainKey.UNADJUSTED_END_DATE, period.getUnadjustedEndDate());
    if (paymentDate.isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(period, provider)));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.of(currency, presentValue(period, provider)));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(KnownAmountPaymentPeriod period, RatesProvider provider) {
    return MultiCurrencyAmount.of(CurrencyAmount.of(period.getCurrency(), presentValue(period, provider)));
  }

  @Override
  public double currentCash(KnownAmountPaymentPeriod period, RatesProvider provider) {
    if (provider.getValuationDate().isEqual(period.getPaymentDate())) {
      return forecastValue(period, provider);
    }
    return 0d;
  }
}
