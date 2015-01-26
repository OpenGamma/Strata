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
 * Simple tests for a resolved link.
 */
@Test
public class ResolvedLinkTest {

  public void test_link_factory() {
    Link<MockIdentifiable> test = Link.resolved(MockIdentifiable.MOCK1);
    assertThat(test).isNotNull();
  }

  public void test_link_factory_null() {
    assertThrowsIllegalArg(() -> Link.resolved(null));
  }

  public void test_constructor() {
    ResolvedLink<MockIdentifiable> test = new ResolvedLink<>(MockIdentifiable.MOCK1);
    assertThat(test.getLinkable()).isEqualTo(MockIdentifiable.MOCK1);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    Link<MockIdentifiable> link = Link.resolved(MockIdentifiable.MOCK1);
    assertThat(link.resolve(null)).isSameAs(MockIdentifiable.MOCK1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(new ResolvedLink<>(MockIdentifiable.MOCK1));
    coverBeanEquals(new ResolvedLink<>(MockIdentifiable.MOCK1), new ResolvedLink<>(MockIdentifiable.MOCK2));
  }

  public void test_serializable() {
    assertSerialization(new ResolvedLink<>(MockIdentifiable.MOCK1));
  }

}
