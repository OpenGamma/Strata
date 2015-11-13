/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.future;

import static com.opengamma.strata.basics.PutCall.CALL;
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
 * Test {@link GenericFutureOptionTrade}.
 */
@Test
public class GenericFutureOptionTradeTest {

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId FUTURE_ID = StandardId.of("OG-Ticker", "Future1");
  private static final StandardId OPTION_ID = StandardId.of("OG-Ticker", "Option1");

  private static final GenericFuture FUTURE = GenericFuture.builder()
      .productId(StandardId.of("Exchange", "Sym01"))
      .expiryMonth(YearMonth.of(2015, 6))
      .expiryDate(date(2015, 6, 15))
      .tickSize(0.0001)
      .tickValue(CurrencyAmount.of(USD, 10))
      .build();
  private static final Security<GenericFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE)
      .standardId(FUTURE_ID)
      .build();
  private static final GenericFutureOption RESOLVABLE_OPTION = GenericFutureOption.builder()
      .productId(StandardId.of("Exchange", "Sym02"))
      .putCall(CALL)
      .strikePrice(1.51)
      .expiryMonth(YearMonth.of(2015, 6))
      .expiryDate(date(2015, 6, 15))
      .tickSize(0.0001)
      .tickValue(CurrencyAmount.of(USD, 10))
      .underlyingLink(SecurityLink.resolvable(FUTURE_ID, GenericFuture.class))
      .build();
  private static final GenericFutureOption RESOLVED_OPTION = GenericFutureOption.builder()
      .productId(StandardId.of("Exchange", "Sym02"))
      .putCall(CALL)
      .strikePrice(1.51)
      .expiryMonth(YearMonth.of(2015, 6))
      .expiryDate(date(2015, 6, 15))
      .tickSize(0.0001)
      .tickValue(CurrencyAmount.of(USD, 10))
      .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY))
      .build();
  private static final Security<GenericFutureOption> OPTION_SECURITY = UnitSecurity.builder(RESOLVABLE_OPTION)
      .standardId(OPTION_ID)
      .build();
  private static final Security<GenericFutureOption> RESOLVED_OPTION_SECURITY = UnitSecurity.builder(RESOLVED_OPTION)
      .standardId(OPTION_ID)
      .build();
  private static final SecurityLink<GenericFutureOption> RESOLVABLE_OPTION_LINK =
      SecurityLink.resolvable(OPTION_ID, GenericFutureOption.class);
//  private static final SecurityLink<GenericFutureOption> RESOLVED_OPTION_LINK =
//      SecurityLink.resolved(OPTION_SECURITY);
  private static final SecurityLink<GenericFutureOption> PARTLY_RESOLVED_OPTION_LINK =
      SecurityLink.resolved(OPTION_SECURITY);
  private static final SecurityLink<GenericFutureOption> FULLY_RESOLVED_OPTION_LINK =
      SecurityLink.resolved(RESOLVED_OPTION_SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVABLE_OPTION_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), PARTLY_RESOLVED_OPTION_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
    assertEquals(test.getSecurity(), OPTION_SECURITY);
    assertEquals(test.getProduct(), RESOLVABLE_OPTION);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .securityLink(RESOLVABLE_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    GenericFutureOptionTrade expected = GenericFutureOptionTrade.builder()
        .securityLink(FULLY_RESOLVED_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        if (identifier.equals(OPTION_ID)) {
          return (T) OPTION_SECURITY;
        }
        assertEquals(identifier, FUTURE_ID);
        return (T) FUTURE_SECURITY;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_partlyResolved() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    GenericFutureOptionTrade expected = GenericFutureOptionTrade.builder()
        .securityLink(FULLY_RESOLVED_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        assertEquals(identifier, FUTURE_ID);
        return (T) FUTURE_SECURITY;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_fullyResolved() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .securityLink(FULLY_RESOLVED_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
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
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    coverImmutableBean(test);
    GenericFutureOptionTrade test2 = GenericFutureOptionTrade.builder()
        .securityLink(RESOLVABLE_OPTION_LINK)
        .initialPrice(1.1)
        .quantity(100)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    GenericFutureOptionTrade test = GenericFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertSerialization(test);
  }

}
