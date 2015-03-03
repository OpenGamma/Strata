/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.LinkResolver;
import com.opengamma.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class OtcTradeTest {

  public void test_builder() {
    OtcTrade<MockSimpleProduct> test = OtcTrade.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .attributes(ImmutableMap.of("A", "B"))
        .product(MockSimpleProduct.MOCK1)
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getAttributes(), ImmutableMap.of("A", "B"));
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), MockSimpleProduct.MOCK1);
  }

  public void test_builder_withProduct() {
    OtcTrade<MockSimpleProduct> test = OtcTrade.builder(MockSimpleProduct.MOCK1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), MockSimpleProduct.MOCK1);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_notResolvable() {
    OtcTrade<MockSimpleProduct> test = OtcTrade.builder(MockSimpleProduct.MOCK1)
        .standardId(StandardId.of("OG-Trade", "1"))
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
    OtcTrade<MockResolvableProduct> test = OtcTrade.builder(MockResolvableProduct.MOCK1)
        .standardId(StandardId.of("OG-Trade", "1"))
        .build();
    OtcTrade<MockResolvableProduct> expected = OtcTrade.builder(MockResolvableProduct.MOCK2)
        .standardId(StandardId.of("OG-Trade", "1"))
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because MockResolvableProduct resolves itself (for testing)
        return null;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OtcTrade<MockSimpleProduct> test = OtcTrade.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .product(MockSimpleProduct.MOCK1)
        .build();
    coverImmutableBean(test);
    OtcTrade<MockSimpleProduct> test2 = OtcTrade.<MockSimpleProduct>builder()
        .setString(OtcTrade.meta().standardId().name(), "OG-Trade~OG2")
        .attributes(ImmutableMap.of("A", "B"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(MockSimpleProduct.MOCK2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OtcTrade<MockSimpleProduct> test = OtcTrade.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Trade", "OG"))
        .product(MockSimpleProduct.MOCK1)
        .build();
    assertSerialization(test);
  }

}
