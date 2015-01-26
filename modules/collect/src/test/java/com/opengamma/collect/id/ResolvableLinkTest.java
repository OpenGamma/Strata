/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

/**
 * Simple tests for a resolvable link.
 */
@Test
public class ResolvableLinkTest {

  public void test_link_factory() {
    Link<MockIdentifiable> link = Link.resolvable(StandardId.of("A", "B"), MockIdentifiable.class);
    assertThat(link).isNotNull();
  }

  public void test_link_factory_null() {
    assertThrowsIllegalArg(() -> Link.resolvable(null, MockIdentifiable.class));
    assertThrowsIllegalArg(() -> Link.resolvable(StandardId.of("A", "B"), null));
  }

  public void test_constructor() {
    StandardId id = StandardId.of("A", "B");
    ResolvableLink<MockIdentifiable> test = new ResolvableLink<>(id, MockIdentifiable.class);
    assertThat(test.getIdentifier()).isEqualTo(id);
    assertThat(test.getTargetType()).isEqualTo(MockIdentifiable.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    StandardId id = StandardId.of("A", "B");
    Link<MockIdentifiable> link = Link.resolvable(id, MockIdentifiable.class);
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(ResolvableLink<T> link) {
        return (T) MockIdentifiable.MOCK1;
      }
    };
    assertThat(link.resolve(resolver)).isEqualTo(MockIdentifiable.MOCK1);
  }

  public void test_resolve_null() {
    Link<MockIdentifiable> link = Link.resolvable(StandardId.of("A", "B"), MockIdentifiable.class);
    assertThrowsIllegalArg(() -> link.resolve(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvableLink<MockIdentifiable> test = new ResolvableLink<>(StandardId.of("A", "B"), MockIdentifiable.class);
    coverImmutableBean(test);
    ResolvableLink<MockIdentifiable> test2 = new ResolvableLink<>(StandardId.of("A", "C"), MockIdentifiable.class);
    coverBeanEquals(test, test2);
  }

  public void test_serializable() {
    assertSerialization(new ResolvableLink<>(StandardId.of("A", "B"), MockIdentifiable.class));
  }

}
