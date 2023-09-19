/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.type.FxSwapConvention;

/**
 * Utility method to scale a discount factor, used by both {@link DiscountingFxResetNotionalExchangePricer} and
 * {@link DiscountingRatePaymentPeriodPricer}.
 */
public final class FxDfScaler {

  private FxDfScaler() {
    // prevent instantiation
  }

  /**
   * Scale a discount factor by the ratio between base and counter currencies at the spot date.
   *
   * @param provider        the rates provider
   * @param refData         the reference data, used to resolve the spot date from the valuation date
   * @param baseCurrency    the base currency
   * @param counterCurrency the counter currency
   * @param df              the unscaled discount factor
   * @return the discount factor scaled by the ratio between base and counter currency discount factors at the spot date
   */
  static double scaledDf(RatesProvider provider, ReferenceData refData, Currency baseCurrency, Currency counterCurrency, double df) {
    LocalDate valuationDate = provider.getValuationDate();
    LocalDate spotDate = FxSwapConvention.of(CurrencyPair.of(baseCurrency, counterCurrency)).calculateSpotDateFromTradeDate(valuationDate,
        refData);

    double dfCounterSpot = provider.discountFactor(counterCurrency, spotDate);
    double dfReferenceSpot = provider.discountFactor(baseCurrency, spotDate);
    double dfScaled = (df / dfCounterSpot) * dfReferenceSpot;
    return dfScaled;
  }
}
