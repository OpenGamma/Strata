/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketData;

/**
 * A provider of FX rates that takes its data from a {@link MarketData} instance.
 */
class MarketDataFxRateProvider implements FxRateProvider {

  /** The market data which provides the FX rates. */
  private final MarketData marketData;

  /**
   * Creates a new instance which takes FX rates from the market data.
   *
   * @param marketData  market data used for looking up FX rates
   */
  MarketDataFxRateProvider(MarketData marketData) {
    this.marketData = marketData;
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1;
    }
    FxRate rate = marketData.getValue(FxRateKey.of(baseCurrency, counterCurrency));
    return rate.fxRate(baseCurrency, counterCurrency);
  }
}
