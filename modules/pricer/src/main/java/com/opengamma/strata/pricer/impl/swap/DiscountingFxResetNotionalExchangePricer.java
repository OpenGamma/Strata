/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.SwapPaymentEventPricer;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;

/**
 * Pricer implementation for the exchange of FX reset notionals.
 * <p>
 * The FX reset notional exchange is priced by discounting the value of the exchange.
 * The value of the exchange is calculated by performing an FX conversion on the amount.
 */
public class DiscountingFxResetNotionalExchangePricer
    implements SwapPaymentEventPricer<FxResetNotionalExchange> {

  /**
   * Default implementation.
   */
  public static final DiscountingFxResetNotionalExchangePricer DEFAULT =
      new DiscountingFxResetNotionalExchangePricer();

  /**
   * Creates an instance.
   */
  public DiscountingFxResetNotionalExchangePricer() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(FxResetNotionalExchange event, RatesProvider provider) {
    // forecastValue * discountFactor
    double df = provider.discountFactor(event.getCurrency(), event.getPaymentDate());
    return forecastValue(event, provider) * df;
  }

  @Override
  public PointSensitivityBuilder presentValueSensitivity(FxResetNotionalExchange event, RatesProvider provider) {
    DiscountFactors discountFactors = provider.discountFactors(event.getCurrency());
    PointSensitivityBuilder sensiDsc = discountFactors.zeroRatePointSensitivity(event.getPaymentDate());
    sensiDsc = sensiDsc.multipliedBy(forecastValue(event, provider));
    PointSensitivityBuilder sensiFx = forecastValueSensitivity(event, provider);
    sensiFx = sensiFx.multipliedBy(discountFactors.discountFactor(event.getPaymentDate()));
    return sensiDsc.combinedWith(sensiFx);
  }

  //-------------------------------------------------------------------------
  @Override
  public double forecastValue(FxResetNotionalExchange event, RatesProvider provider) {
    // notional * fxRate
    return event.getNotional() * fxRate(event, provider);
  }

  // obtains the FX rate
  private double fxRate(FxResetNotionalExchange event, RatesProvider provider) {
    FxIndexRates rates = provider.fxIndexRates(event.getObservation().getIndex());
    return rates.rate(event.getObservation(), event.getReferenceCurrency());
  }

  @Override
  public PointSensitivityBuilder forecastValueSensitivity(FxResetNotionalExchange event, RatesProvider provider) {
    FxIndexRates rates = provider.fxIndexRates(event.getObservation().getIndex());
    return rates.ratePointSensitivity(event.getObservation(), event.getReferenceCurrency())
        .multipliedBy(event.getNotional());
  }

  //-------------------------------------------------------------------------
  @Override
  public void explainPresentValue(FxResetNotionalExchange event, RatesProvider provider, ExplainMapBuilder builder) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();

    builder.put(ExplainKey.ENTRY_TYPE, "FxResetNotionalExchange");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.TRADE_NOTIONAL, event.getNotionalAmount());
    if (paymentDate.isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.addListEntry(ExplainKey.OBSERVATIONS, child -> {
        child.put(ExplainKey.ENTRY_TYPE, "FxObservation");
        child.put(ExplainKey.INDEX, event.getObservation().getIndex());
        child.put(ExplainKey.FIXING_DATE, event.getObservation().getFixingDate());
        child.put(ExplainKey.INDEX_VALUE, fxRate(event, provider));
      });
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(event, provider)));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.of(currency, presentValue(event, provider)));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount currencyExposure(FxResetNotionalExchange event, RatesProvider provider) {
    LocalDate fixingDate = event.getObservation().getFixingDate();
    FxIndexRates rates = provider.fxIndexRates(event.getObservation().getIndex());
    double df = provider.discountFactor(event.getCurrency(), event.getPaymentDate());
    if (!fixingDate.isAfter(provider.getValuationDate()) &&
        rates.getFixings().get(fixingDate).isPresent()) {
      double fxRate = rates.rate(event.getObservation(), event.getReferenceCurrency());
      return MultiCurrencyAmount.of(CurrencyAmount.of(event.getCurrency(), event.getNotional() * df * fxRate));
    }
    LocalDate maturityDate = event.getObservation().getMaturityDate();
    double fxRateSpotSensitivity =
        rates.getFxForwardRates().rateFxSpotSensitivity(event.getReferenceCurrency(), maturityDate);
    return MultiCurrencyAmount.of(
        CurrencyAmount.of(event.getReferenceCurrency(), event.getNotional() * df * fxRateSpotSensitivity));
  }

  @Override
  public double currentCash(FxResetNotionalExchange event, RatesProvider provider) {
    if (provider.getValuationDate().isEqual(event.getPaymentDate())) {
      return forecastValue(event, provider);
    }
    return 0d;
  }

}
