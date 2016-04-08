/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;

@Test
public class MarketDataFxRateProviderTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final double EUR_USD = 1.10; 
  private static final double GBP_USD = 1.50; 
  private static final Currency BEF = Currency.of("BEF");
  private static final double EUR_BEF = 40.3399; 

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
        ImmutableMap.of(FxRateKey.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, EUR_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    return new MarketDataFxRateProvider(marketData);
  }

  public void missingCurrencies() {
    assertThrowsIllegalArg(() -> provider().fxRate(Currency.EUR, Currency.GBP));
  }

  public void cross_base() {
    Map<FxRateKey, FxRate> marketDataMap =
        ImmutableMap.of(FxRateKey.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, EUR_USD),
            FxRateKey.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, GBP_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx =  new MarketDataFxRateProvider(marketData);
    assertEquals(fx.fxRate(Currency.GBP, Currency.EUR), GBP_USD / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(Currency.EUR, Currency.GBP), EUR_USD / GBP_USD, 1.0E-10);
  }

  public void cross_counter() {
    Map<FxRateKey, FxRate> marketDataMap =
        ImmutableMap.of(FxRateKey.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, EUR_USD),
            FxRateKey.of(Currency.EUR, BEF), FxRate.of(Currency.EUR, BEF, EUR_BEF));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx =  new MarketDataFxRateProvider(marketData);
    assertEquals(fx.fxRate(Currency.USD, BEF), EUR_BEF / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(BEF, Currency.USD), EUR_USD / EUR_BEF, 1.0E-10);
  }

  public void cross_double_triangle() {
    Map<FxRateKey, FxRate> marketDataMap =
        ImmutableMap.of(FxRateKey.of(Currency.EUR, Currency.USD), FxRate.of(Currency.EUR, Currency.USD, EUR_USD),
            FxRateKey.of(Currency.EUR, BEF), FxRate.of(Currency.EUR, BEF, EUR_BEF),
            FxRateKey.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, GBP_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx =  new MarketDataFxRateProvider(marketData);
    assertEquals(fx.fxRate(Currency.GBP, BEF), GBP_USD * EUR_BEF / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(BEF, Currency.GBP), EUR_USD / EUR_BEF / GBP_USD, 1.0E-10);
  }

}
