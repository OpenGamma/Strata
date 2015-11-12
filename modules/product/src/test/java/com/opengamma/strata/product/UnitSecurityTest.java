/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class UnitSecurityTest {

  public void test_builder() {
    UnitSecurity<MockSimpleProduct> test = UnitSecurity.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .attributes(ImmutableMap.of("A", "B"))
        .name("SecurityName")
        .product(MockSimpleProduct.MOCK1)
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Ticker", "OG"));
    assertEquals(test.getAttributes(), ImmutableMap.of("A", "B"));
    assertEquals(test.getName(), "SecurityName");
    assertEquals(test.getProduct(), MockSimpleProduct.MOCK1);
  }

  public void test_builder_withProduct_nameDefaulted() {
    UnitSecurity<MockSimpleProduct> test = UnitSecurity.builder(MockSimpleProduct.MOCK1)
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Ticker", "OG"));
    assertEquals(test.getAttributes(), ImmutableMap.of());
    assertEquals(test.getName(), "");
    assertEquals(test.getProduct(), MockSimpleProduct.MOCK1);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_notResolvable() {
    UnitSecurity<MockSimpleProduct> test = UnitSecurity.builder(MockSimpleProduct.MOCK1)
        .standardId(StandardId.of("OG-Ticker", "OG"))
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
    UnitSecurity<MockResolvableProduct> test = UnitSecurity.builder(MockResolvableProduct.MOCK1)
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .build();
    UnitSecurity<MockResolvableProduct> expected = UnitSecurity.builder(MockResolvableProduct.MOCK2)
        .standardId(StandardId.of("OG-Ticker", "OG"))
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
    UnitSecurity<MockSimpleProduct> test = UnitSecurity.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .product(MockSimpleProduct.MOCK1)
        .build();
    coverImmutableBean(test);
    UnitSecurity<MockSimpleProduct> test2 = UnitSecurity.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Ticker", "OG2"))
        .attributes(ImmutableMap.of("A", "B"))
        .name("SecurityName")
        .product(MockSimpleProduct.MOCK2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    UnitSecurity<MockSimpleProduct> test = UnitSecurity.<MockSimpleProduct>builder()
        .standardId(StandardId.of("OG-Ticker", "OG"))
        .product(MockSimpleProduct.MOCK1)
        .build();
    assertSerialization(test);
  }

}
