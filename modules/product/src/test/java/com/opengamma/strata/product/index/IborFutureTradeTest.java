/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;

/**
 * Test {@link IborFutureTrade}.
 */
@Test
public class IborFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 3, 18)).build();
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long QUANTITY = 35;
  private static final double PRICE = 1.015;
  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "OG");
  private static final Security<IborFuture> SECURITY = UnitSecurity.builder(PRODUCT)
      .standardId(SECURITY_ID)
      .build();
  private static final SecurityLink<IborFuture> RESOLVABLE_LINK =
      SecurityLink.resolvable(SECURITY_ID, IborFuture.class);
  private static final SecurityLink<IborFuture> RESOLVED_LINK =
      SecurityLink.resolved(SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    IborFutureTrade test = sut();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVABLE_LINK);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    IborFutureTrade test = IborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVED_LINK)
        .price(PRICE)
        .quantity(QUANTITY)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), RESOLVED_LINK);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    IborFutureTrade test = IborFutureTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    IborFutureTrade expected = IborFutureTrade.builder()
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
    IborFutureTrade test = IborFutureTrade.builder()
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
  public void test_resolve() {
    IborFutureTrade test = IborFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(RESOLVED_LINK)
        .price(PRICE)
        .quantity(QUANTITY)
        .build();
    ResolvedIborFutureTrade resolved = test.resolve(REF_DATA);
    assertEquals(resolved.getTradeInfo(), TRADE_INFO);
    assertEquals(resolved.getProduct(), PRODUCT.resolve(REF_DATA));
    assertEquals(resolved.getSecurityStandardId(), SECURITY_ID);
    assertEquals(resolved.getQuantity(), QUANTITY);
    assertEquals(resolved.getPrice(), PRICE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static IborFutureTrade sut() {
    return IborFutureTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(RESOLVABLE_LINK)
        .price(PRICE)
        .quantity(QUANTITY)
        .build();
  }

  static IborFutureTrade sut2() {
    return IborFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG2"), IborFuture.class))
        .price(1.1)
        .quantity(100)
        .build();
  }

}
