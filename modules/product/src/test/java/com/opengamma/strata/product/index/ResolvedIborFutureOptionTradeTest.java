/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link ResolvedIborFutureOptionTrade}.
 */
@Test
public class ResolvedIborFutureOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double PRICE = 0.015;
  private static final StandardId OPTION_ID = StandardId.of("OG-Ticker", "Option1");
  private static final StandardId OPTION_ID2 = StandardId.of("OG-Ticker", "Option2");

  private static final ResolvedIborFuture FUTURE = ResolvedIborFuture.builder()
      .currency(Currency.USD)
      .notional(1_000_000d)
      .iborRate(IborRateObservation.of(USD_LIBOR_3M, date(2015, 3, 16), REF_DATA))
      .build();
  private static final ResolvedIborFutureOption OPTION = ResolvedIborFutureOption.builder()
      .putCall(CALL)
      .strikePrice(12)
      .expiry(ZonedDateTime.of(2014, 6, 30, 11, 0, 0, 0, ZoneId.of("Europe/London")))
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlying(FUTURE)
      .build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureOptionTrade test = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(OPTION)
        .securityStandardId(OPTION_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getProduct(), OPTION);
    assertEquals(test.getSecurityStandardId(), OPTION_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), OptionalDouble.of(PRICE));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborFutureOptionTrade test = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(OPTION)
        .securityStandardId(OPTION_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    coverImmutableBean(test);
    ResolvedIborFutureOptionTrade test2 = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(OPTION)
        .securityStandardId(OPTION_ID2)
        .quantity(QUANTITY + 1)
        .price(PRICE + 0.1)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedIborFutureOptionTrade test = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .product(OPTION)
        .securityStandardId(OPTION_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertSerialization(test);
  }

}
