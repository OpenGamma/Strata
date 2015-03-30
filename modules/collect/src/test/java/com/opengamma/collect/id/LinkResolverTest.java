/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

import static com.opengamma.collect.id.MockIdentifiable.MOCK1;
import static com.opengamma.collect.id.MockIdentifiable.MOCK1_RESOLVED_MOCK2;
import static com.opengamma.collect.id.MockIdentifiable.MOCK2;
import static com.opengamma.collect.id.MockIdentifiable.STANDARD_ID_2;
import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

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

  public void test_resolve_Class() {
    assertThat(RESOLVER.resolve(STANDARD_ID_2, MockIdentifiable.class)).isEqualTo(MOCK2);
  }

  public void test_resolve_TypeToken() {
    TypeToken<MockIdentifiable> token = TypeToken.of(MockIdentifiable.class);
    assertThat(RESOLVER.resolve(STANDARD_ID_2, token)).isSameAs(MOCK2);
  }

  public void test_resolveAll_notResolvable() {
    assertThat(RESOLVER.resolveLinksIn(MOCK1, "TargetIsNotResolvable", r -> r)).isSameAs(MOCK1);
  }

  public void test_resolveAll_targetIsResolvableAndNeedsResolving() {
    MockIdentifiable test = MockIdentifiable.MOCK1_LINKED_MOCK2.resolveLinks(RESOLVER);
    assertThat(test).isEqualTo(MOCK1_RESOLVED_MOCK2);
  }

  public void test_resolveAll_targetIsResolvableAndDoesNotNeedResolving() {
    MockIdentifiable test = MOCK1_RESOLVED_MOCK2.resolveLinks(RESOLVER);
    assertThat(test).isSameAs(MOCK1_RESOLVED_MOCK2);
  }

}
