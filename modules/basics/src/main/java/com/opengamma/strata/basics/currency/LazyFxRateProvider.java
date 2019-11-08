/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An {@code FxRateProvider} that delays fetching its underlying provider
 * until actually necessary.
 * <p>
 * This is typically useful where you <em>may</em> need a {@code MarketDataFxRateProvider}
 * but want to delay loading market data to construct the provider until you are sure you actually do need it.
 */
class LazyFxRateProvider implements FxRateProvider {

  /**
   * The supplier of the underlying provider.
   */
  private final Supplier<FxRateProvider> target;

  /**
   * Package-scoped constructor.
   *
   * @param target  the supplier of the underlying provider
   */
  LazyFxRateProvider(Supplier<FxRateProvider> target) {
    ArgChecker.notNull(target, "target");
    this.target = Suppliers.memoize(target::get);
  }

  @Override
  public double convert(double amount, Currency fromCurrency, Currency toCurrency) {
    if (fromCurrency.equals(toCurrency)) {
      return amount;
    }
    return target.get().convert(amount, fromCurrency, toCurrency);
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    return fxRate(CurrencyPair.of(baseCurrency, counterCurrency));
  }

  @Override
  public double fxRate(CurrencyPair currencyPair) {
    if (currencyPair.getBase().equals(currencyPair.getCounter())) {
      return 1;
    }
    return target.get().fxRate(currencyPair);
  }
}
