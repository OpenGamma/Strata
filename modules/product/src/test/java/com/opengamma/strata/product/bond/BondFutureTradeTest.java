/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

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
 * Test {@link BondFutureTrade}.
 */
@Test
public class BondFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE_PRODUCT = BondFutureTest.sut();
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND-FUTURE");
  private static final Security<BondFuture> FUTURE_SECURITY =
      UnitSecurity.builder(FUTURE_PRODUCT).standardId(FUTURE_SECURITY_ID).build();
  private static final SecurityLink<BondFuture> FUTURE_SECURITY_LINK_RESOLVED = SecurityLink.resolved(FUTURE_SECURITY);
  private static final SecurityLink<BondFuture> FUTURE_SECURITY_LINK_RESOLVABLE = SecurityLink.resolvable(FUTURE_SECURITY_ID, BondFuture.class);
  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, FUTURE_SECURITY_ID);
      return (T) FUTURE_SECURITY;
    }
  };
  // trade
  private static final LocalDate TRADE_DATE = LocalDate.of(2011, 6, 20);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  private static final long QUANTITY = 1234L;
  private static final double PRICE = 1.2345;

  //-------------------------------------------------------------------------
  public void test_of_resolved() {
    BondFutureTrade test = sut();
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getProduct(), FUTURE_PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), FUTURE_SECURITY);
    assertEquals(test.getSecurityLink(), FUTURE_SECURITY_LINK_RESOLVED);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void test_of_resolvable() {
    BondFutureTrade test = BondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVABLE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), FUTURE_SECURITY_LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_resolveLinks_resolved() {
    BondFutureTrade base = sut();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    BondFutureTrade base = BondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVABLE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), sut());
  }

  public void test_resolve() {
    ResolvedBondFutureTrade expected = ResolvedBondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(FUTURE_PRODUCT.resolve(REF_DATA))
        .securityStandardId(FUTURE_SECURITY_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static BondFutureTrade sut() {
    return BondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(FUTURE_SECURITY_LINK_RESOLVED)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static BondFutureTrade sut2() {
    return BondFutureTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(BondFutureTest.sut2())
            .standardId(StandardId.of("Ticker", "GOV1-BND-FUT"))
            .build()))
        .quantity(100L)
        .build();
  }

}
