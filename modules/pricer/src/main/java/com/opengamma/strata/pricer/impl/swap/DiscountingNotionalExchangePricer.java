/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentEventPricer;
import com.opengamma.strata.product.swap.NotionalExchange;

/**
 * Pricer implementation for the exchange of notionals.
 * <p>
 * The notional exchange is priced by discounting the value of the exchange.
 */
public class DiscountingNotionalExchangePricer
    implements SwapPaymentEventPricer<NotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingNotionalExchangePricer DEFAULT = new DiscountingNotionalExchangePricer(
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public DiscountingNotionalExchangePricer(DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(NotionalExchange event, RatesProvider provider) {
    return paymentPricer.presentValueAmount(event.getPayment(), provider);
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(NotionalExchange event, RatesProvider provider) {
    return paymentPricer.presentValueSensitivity(event.getPayment(), provider);
  }

  //-------------------------------------------------------------------------
  @Override
  public double forecastValue(NotionalExchange event, RatesProvider provider) {
    return paymentPricer.forecastValueAmount(event.getPayment(), provider);
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(NotionalExchange event, RatesProvider provider) {
    return PointSensitivityBuilder.none();
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(NotionalExchange event, RatesProvider provider, ExplainMapBuilder builder) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();

    builder.put(ExplainKey.ENTRY_TYPE, "NotionalExchange");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.TRADE_NOTIONAL, event.getPaymentAmount());
    if (paymentDate.isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(event, provider)));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.of(currency, presentValue(event, provider)));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(NotionalExchange event, RatesProvider provider) {
    return paymentPricer.currencyExposure(event.getPayment(), provider);
  }

  @Override
  public double currentCash(NotionalExchange event, RatesProvider provider) {
    return paymentPricer.currentCash(event.getPayment(), provider).getAmount();
  }

}
