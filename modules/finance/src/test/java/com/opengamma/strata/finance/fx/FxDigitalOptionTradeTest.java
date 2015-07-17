/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Test {@link FxDigitalOptionTrade}.
 */
@Test
public class FxDigitalOptionTradeTest {
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LongShort LONG = LongShort.LONG;
  private static final PutCall CALL = PutCall.CALL;
  private static final FxRate STRIKE = FxRate.of(EUR, USD, 1.3);
  private static final double NOTIONAL = 1.0e6;
  private static final FxIndex INDEX = FxIndices.ECB_EUR_USD;
  FxDigitalOption FX_OPTION = FxDigitalOption.builder()
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .index(INDEX)
      .longShort(LONG)
      .notional(NOTIONAL)
      .payoffCurrency(USD)
      .putCall(CALL)
      .strike(STRIKE)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 11, 12)).build();

  public void test_builder() {
    FxDigitalOptionTrade test = FxDigitalOptionTrade.builder()
        .product(FX_OPTION)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), FX_OPTION);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void coverage() {
    FxDigitalOptionTrade test1 = FxDigitalOptionTrade.builder()
        .product(FX_OPTION)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    FxDigitalOption option = FxDigitalOption.builder()
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .index(INDEX)
        .longShort(LongShort.SHORT)
        .notional(NOTIONAL)
        .payoffCurrency(EUR)
        .putCall(PutCall.PUT)
        .strike(STRIKE)
        .build();
    FxDigitalOptionTrade test2 = FxDigitalOptionTrade.builder()
        .product(option)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxDigitalOptionTrade test = FxDigitalOptionTrade.builder()
        .product(FX_OPTION)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }
}
