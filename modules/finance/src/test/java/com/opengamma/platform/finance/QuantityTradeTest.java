/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.LinkResolutionException;
import com.opengamma.collect.id.LinkResolver;
import com.opengamma.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class QuantityTradeTest {

  Security<MockSimpleProduct> SECURITY1 = UnitSecurity.builder(MockSimpleProduct.MOCK1)
      .standardId(StandardId.of("OG-Ticker", "1"))
      .build();

  public void test_builder() {
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .attributes(ImmutableMap.of("A", "B"))
        .quantity(100)
        .securityLink(SecurityLink.resolved(SECURITY1))
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getAttributes(), ImmutableMap.of("A", "B"));
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getQuantity(), 100);
    assertEquals(test.getPremium(), Optional.empty());
    assertEquals(test.getSecurityLink(), SecurityLink.resolved(SECURITY1));
    assertEquals(test.getSecurity(), SECURITY1);
  }

  public void test_builder_withProduct() {
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(SECURITY1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .attributes(ImmutableMap.of("A", "B"))
        .quantity(100)
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getAttributes(), ImmutableMap.of("A", "B"));
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getQuantity(), 100);
    assertEquals(test.getPremium(), Optional.empty());
    assertEquals(test.getSecurityLink(), SecurityLink.resolved(SECURITY1));
    assertEquals(test.getSecurity(), SECURITY1);
  }

  //-------------------------------------------------------------------------
  public void test_getSecurity_notResolved() {
    SecurityLink<MockSimpleProduct> link =
        SecurityLink.resolvable(SECURITY1.getStandardId(), MockSimpleProduct.class);
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(link)
        .standardId(StandardId.of("OG-Trade", "2"))
        .quantity(200)
        .build();
    assertThrows(() -> test.getSecurity(), LinkResolutionException.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_notResolvable() {
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(SECURITY1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .quantity(100)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because MockNonResolvableProduct is not resolvable
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  public void test_resolveLinks_resolvable() {
    SecurityLink<MockSimpleProduct> link =
        SecurityLink.resolvable(SECURITY1.getStandardId(), MockSimpleProduct.class);
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(link)
        .standardId(StandardId.of("OG-Trade", "2"))
        .quantity(200)
        .build();
    QuantityTrade<MockSimpleProduct> expected = QuantityTrade.builder(SECURITY1)
        .standardId(StandardId.of("OG-Trade", "2"))
        .quantity(200)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        return (T) SECURITY1;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(SECURITY1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .attributes(ImmutableMap.of("A", "B"))
        .quantity(100)
        .build();
    coverImmutableBean(test);
    SecurityLink<MockSimpleProduct> link =
        SecurityLink.resolvable(SECURITY1.getStandardId(), MockSimpleProduct.class);
    QuantityTrade<MockSimpleProduct> test2 = QuantityTrade.builder(link)
        .standardId(StandardId.of("OG-Trade", "2"))
        .attributes(ImmutableMap.of())
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .quantity(200)
        .premium(CurrencyAmount.of(Currency.GBP, 1000))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    QuantityTrade<MockSimpleProduct> test = QuantityTrade.builder(SECURITY1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .attributes(ImmutableMap.of("A", "B"))
        .quantity(100)
        .build();
    assertSerialization(test);
  }

}
