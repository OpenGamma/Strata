/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketData;

@Test
public class MarketDataFxRateProviderTest {

  public void fxRate() {
    double eurUsdRate = provider().fxRate(Currency.EUR, Currency.USD);
    assertThat(eurUsdRate).isEqualTo(1.1d);
  }

  public void sameCurrencies() {
    double eurRate = provider().fxRate(Currency.EUR, Currency.EUR);
    assertThat(eurRate).isEqualTo(1d);
  }

  private static FxRateProvider provider() {
    Map<FxRateKey, FxRate> marketDataMap =
        ImmutableMap.of(FxRateKey.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, 1.1));
    MarketData marketData = MarketData.of(marketDataMap);
    return new MarketDataFxRateProvider(marketData);
  }
}
