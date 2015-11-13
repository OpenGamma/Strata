/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.future;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;

/**
 * Test {@link GenericFutureTrade}.
 */
@Test
public class GenericFutureTradeTest {

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double INITIAL_PRICE = 1.015;

  private static final GenericFuture PRODUCT = GenericFuture.builder()
      .productId(StandardId.of("Exchange", "Sym01"))
      .expiryMonth(YearMonth.of(2015, 6))
      .expiryDate(date(2015, 6, 15))
      .tickSize(0.0001)
      .tickValue(CurrencyAmount.of(USD, 10))
      .build();
  private static final Security<GenericFuture> SECURITY = UnitSecurity.builder(PRODUCT)
      .standardId(StandardId.of("OG-Ticker", "OG"))
      .build();
  private static final SecurityLink<GenericFuture> RESOLVABLE_LINK =
      SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG"), GenericFuture.class);
  private static final SecurityLink<GenericFuture> RESOLVED_LINK =
      SecurityLink.resolved(SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_LINK)
        .initialPrice(INITIAL_PRICE)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVABLE_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVED_LINK)
        .initialPrice(INITIAL_PRICE)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVED_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    GenericFutureTrade expected = GenericFutureTrade.builder()
        .securityLink(RESOLVED_LINK)
        .quantity(100)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        assertEquals(identifier, SECURITY.getStandardId());
        return (T) SECURITY;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_resolved() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .securityLink(RESOLVED_LINK)
        .quantity(100)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because link is already resolved
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_LINK)
        .initialPrice(INITIAL_PRICE)
        .quantity(QUANTITY)
        .build();
    coverImmutableBean(test);
    GenericFutureTrade test2 = GenericFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2015, 3, 18)).build())
        .securityLink(SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG2"), GenericFuture.class))
        .initialPrice(1.1)
        .quantity(100)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    GenericFutureTrade test = GenericFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_LINK)
        .initialPrice(INITIAL_PRICE)
        .quantity(QUANTITY)
        .build();
    assertSerialization(test);
  }

}
