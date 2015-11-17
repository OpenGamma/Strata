/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Test IborFutureOptionSecurityTrade.
 */
@Test
public class IborFutureOptionTradeTest {

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double INITIAL_PRICE = 0.015;
  private static final StandardId FUTURE_ID = StandardId.of("OG-Ticker", "Future1");
  private static final StandardId OPTION_ID = StandardId.of("OG-Ticker", "Option1");

  private static final IborFuture FUTURE = IborFuture.builder()
      .currency(Currency.USD)
      .notional(1_000_000d)
      .lastTradeDate(date(2015, 3, 16))
      .index(IborIndices.USD_LIBOR_3M)
      .build();
  private static final Security<IborFuture> FUTURE_SECURITY = UnitSecurity.builder(FUTURE)
      .standardId(FUTURE_ID)
      .build();
  private static final IborFutureOption RESOLVABLE_OPTION = IborFutureOption.builder()
      .putCall(CALL)
      .strikePrice(12)
      .expiryDate(date(2014, 6, 30))
      .expiryTime(LocalTime.of(11, 0))
      .expiryZone(ZoneId.of("Europe/London"))
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingLink(SecurityLink.resolvable(FUTURE_ID, IborFuture.class))
      .build();
  private static final IborFutureOption RESOLVED_OPTION = IborFutureOption.builder()
      .putCall(CALL)
      .strikePrice(12)
      .expiryDate(date(2014, 6, 30))
      .expiryTime(LocalTime.of(11, 0))
      .expiryZone(ZoneId.of("Europe/London"))
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingLink(SecurityLink.resolved(FUTURE_SECURITY))
      .build();
  private static final Security<IborFutureOption> OPTION_SECURITY = UnitSecurity.builder(RESOLVABLE_OPTION)
      .standardId(OPTION_ID)
      .build();
  private static final Security<IborFutureOption> RESOLVED_OPTION_SECURITY = UnitSecurity.builder(RESOLVED_OPTION)
      .standardId(OPTION_ID)
      .build();
  private static final SecurityLink<IborFutureOption> RESOLVABLE_OPTION_LINK =
      SecurityLink.resolvable(OPTION_ID, IborFutureOption.class);
  private static final SecurityLink<IborFutureOption> PARTLY_RESOLVED_OPTION_LINK =
      SecurityLink.resolved(OPTION_SECURITY);
  private static final SecurityLink<IborFutureOption> FULLY_RESOLVED_OPTION_LINK =
      SecurityLink.resolved(RESOLVED_OPTION_SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVABLE_OPTION_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), OptionalDouble.of(INITIAL_PRICE));
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), PARTLY_RESOLVED_OPTION_LINK);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getInitialPrice(), OptionalDouble.empty());
    assertEquals(test.getSecurity(), OPTION_SECURITY);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .securityLink(RESOLVABLE_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    IborFutureOptionTrade expected = IborFutureOptionTrade.builder()
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
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(100)
        .initialPrice(INITIAL_PRICE)
        .build();
    IborFutureOptionTrade expected = IborFutureOptionTrade.builder()
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
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
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
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    coverImmutableBean(test);
    IborFutureOptionTrade test2 = IborFutureOptionTrade.builder()
        .securityLink(RESOLVABLE_OPTION_LINK)
        .quantity(100)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureOptionTrade test = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(PARTLY_RESOLVED_OPTION_LINK)
        .quantity(QUANTITY)
        .initialPrice(INITIAL_PRICE)
        .build();
    assertSerialization(test);
  }

}
