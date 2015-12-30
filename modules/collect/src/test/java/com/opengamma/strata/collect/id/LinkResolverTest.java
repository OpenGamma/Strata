/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK1;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK1_LINKED_MOCK2;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK1_RESOLVED_MOCK2;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK2;
import static com.opengamma.strata.collect.id.MockIdentifiable.STANDARD_ID_2;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

/**
 * Tests for a link resolver.
 */
@Test
public class LinkResolverTest {

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertThat(identifier).isEqualTo(STANDARD_ID_2);
      assertThat(targetType).isEqualTo(TypeToken.of(MockIdentifiable.class));
      return (T) MOCK2;
    }
  };

  //-------------------------------------------------------------------------
  public void test_none() {
    assertThrows(() -> LinkResolver.none().resolve(STANDARD_ID_2, MockIdentifiable.class), LinkResolutionException.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolve_Class() {
    assertThat(RESOLVER.resolve(STANDARD_ID_2, MockIdentifiable.class)).isEqualTo(MOCK2);
  }

  public void test_resolve_TypeToken() {
    TypeToken<MockIdentifiable> token = TypeToken.of(MockIdentifiable.class);
    assertThat(RESOLVER.resolve(STANDARD_ID_2, token)).isSameAs(MOCK2);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinksIn_beanList() {
    Object bean = new Object();
    assertThat(RESOLVER.resolveLinksIn(ImmutableList.of(bean, MOCK1_LINKED_MOCK2)))
        .isEqualTo(ImmutableList.of(bean, MOCK1_RESOLVED_MOCK2));
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinksIn_bean_notResolvable() {
    Object bean = new Object();
    assertThat(RESOLVER.resolveLinksIn(bean)).isSameAs(bean);
  }

  public void test_resolveLinksIn_bean_resolvable() {
    assertThat(RESOLVER.resolveLinksIn(MOCK1_LINKED_MOCK2)).isEqualTo(MOCK1_RESOLVED_MOCK2);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinksIn_property_notResolvable() {
    assertThat(RESOLVER.resolveLinksIn(MOCK1, "TargetIsNotResolvable", r -> r)).isSameAs(MOCK1);
  }

  public void test_resolveLinksIn_property_targetIsResolvableAndNeedsResolving() {
    MockIdentifiable test = MOCK1_LINKED_MOCK2.resolveLinks(RESOLVER);
    assertThat(test).isEqualTo(MOCK1_RESOLVED_MOCK2);
  }

  public void test_resolveLinksIn_property_targetIsResolvableAndDoesNotNeedResolving() {
    MockIdentifiable test = MOCK1_RESOLVED_MOCK2.resolveLinks(RESOLVER);
    assertThat(test).isSameAs(MOCK1_RESOLVED_MOCK2);
  }

}
