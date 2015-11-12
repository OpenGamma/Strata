/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.util.Optional;

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
 * Test.
 */
@Test
public class EquityTradeTest {

  Equity PRODUCT = Equity.builder().currency(GBP).build();
  Security<Equity> SECURITY = UnitSecurity.builder(PRODUCT)
      .standardId(StandardId.of("OG-Ticker", "1"))
      .build();
  SecurityLink<Equity> RESOLVABLE_LINK = SecurityLink.resolvable(SECURITY.getStandardId(), Equity.class);
  SecurityLink<Equity> RESOLVED_LINK = SecurityLink.resolved(SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    EquityTrade test = EquityTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getSecurityLink(), RESOLVABLE_LINK);
    assertEquals(test.getQuantity(), 100);
    assertEquals(test.getPremium(), Optional.empty());
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    EquityTrade test = EquityTrade.builder()
        .securityLink(RESOLVED_LINK)
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .quantity(100)
        .premium(CurrencyAmount.of(GBP, 1200))
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(date(2014, 6, 30)).build());
    assertEquals(test.getSecurityLink(), RESOLVED_LINK);
    assertEquals(test.getQuantity(), 100);
    assertEquals(test.getPremium(), Optional.of(CurrencyAmount.of(GBP, 1200)));
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    EquityTrade test = EquityTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    EquityTrade expected = EquityTrade.builder()
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
    EquityTrade test = EquityTrade.builder()
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
    EquityTrade test = EquityTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    coverImmutableBean(test);
    EquityTrade test2 = EquityTrade.builder()
        .securityLink(RESOLVED_LINK)
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .quantity(200)
        .premium(CurrencyAmount.of(GBP, 1200))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    EquityTrade test = EquityTrade.builder()
        .securityLink(RESOLVABLE_LINK)
        .quantity(100)
        .build();
    assertSerialization(test);
  }

}
