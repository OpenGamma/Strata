/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * Test {@link MarketDataFxRateProvider}.
 */
@Test
public class MarketDataFxRateProviderTest {

  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final double EUR_USD = 1.10;
  private static final double GBP_USD = 1.50;
  private static final double EUR_CHF = 1.05;
  private static final double GBP_CHF = 1.41;
  private static final Currency BEF = Currency.of("BEF");
  private static final double EUR_BEF = 40.3399;
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  public void fxRate() {
    double eurUsdRate = provider().fxRate(EUR, USD);
    assertThat(eurUsdRate).isEqualTo(1.1d);
  }

  public void sameCurrencies() {
    double eurRate = provider().fxRate(EUR, EUR);
    assertThat(eurRate).isEqualTo(1d);
  }

  public void missingCurrencies() {
    assertThrows(
        () -> provider().fxRate(EUR, GBP),
        MarketDataNotFoundException.class,
        "No FX rate market data for EUR/GBP using source 'Vendor'");
    assertThrows(
        () -> provider2().fxRate(JPY, GBP),
        MarketDataNotFoundException.class,
        "No FX rate market data for JPY/GBP");
  }

  public void cross_specified() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, CHF), FxRate.of(EUR, CHF, EUR_CHF),
            FxRateId.of(GBP, CHF), FxRate.of(GBP, CHF, GBP_CHF));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx = MarketDataFxRateProvider.of(marketData, ObservableSource.NONE, CHF);
    assertEquals(fx.fxRate(GBP, EUR), GBP_CHF / EUR_CHF, 1.0E-10);
    assertEquals(fx.fxRate(EUR, GBP), EUR_CHF / GBP_CHF, 1.0E-10);
    assertThrows(() -> fx.fxRate(EUR, USD), MarketDataNotFoundException.class);
  }

  public void cross_base() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, USD), FxRate.of(EUR, USD, EUR_USD),
            FxRateId.of(GBP, USD), FxRate.of(GBP, USD, GBP_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx = MarketDataFxRateProvider.of(marketData);
    assertEquals(fx.fxRate(GBP, EUR), GBP_USD / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(EUR, GBP), EUR_USD / GBP_USD, 1.0E-10);
  }

  public void cross_counter() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, USD), FxRate.of(EUR, USD, EUR_USD),
            FxRateId.of(EUR, BEF), FxRate.of(EUR, BEF, EUR_BEF));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx = MarketDataFxRateProvider.of(marketData);
    assertEquals(fx.fxRate(USD, BEF), EUR_BEF / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(BEF, USD), EUR_USD / EUR_BEF, 1.0E-10);
  }

  public void cross_double_triangle() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, USD), FxRate.of(EUR, USD, EUR_USD),
            FxRateId.of(EUR, BEF), FxRate.of(EUR, BEF, EUR_BEF),
            FxRateId.of(GBP, USD), FxRate.of(GBP, USD, GBP_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    FxRateProvider fx = MarketDataFxRateProvider.of(marketData);
    assertEquals(fx.fxRate(GBP, BEF), GBP_USD * EUR_BEF / EUR_USD, 1.0E-10);
    assertEquals(fx.fxRate(BEF, GBP), EUR_USD / EUR_BEF / GBP_USD, 1.0E-10);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    MarketDataFxRateProvider test = provider();
    coverImmutableBean(test);
    MarketDataFxRateProvider test2 = provider2();
    coverBeanEquals(test, test2);
  }

  //-------------------------------------------------------------------------
  private static MarketDataFxRateProvider provider() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, USD, OBS_SOURCE), FxRate.of(EUR, USD, EUR_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    return MarketDataFxRateProvider.of(marketData, OBS_SOURCE, GBP);
  }

  private static MarketDataFxRateProvider provider2() {
    Map<FxRateId, FxRate> marketDataMap =
        ImmutableMap.of(FxRateId.of(EUR, USD), FxRate.of(EUR, USD, EUR_USD),
            FxRateId.of(EUR, BEF), FxRate.of(EUR, BEF, EUR_BEF),
            FxRateId.of(GBP, USD), FxRate.of(GBP, USD, GBP_USD));
    MarketData marketData = ImmutableMarketData.of(VAL_DATE, marketDataMap);
    return MarketDataFxRateProvider.of(marketData, ObservableSource.NONE, GBP);
  }

}
