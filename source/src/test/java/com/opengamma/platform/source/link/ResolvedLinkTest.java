/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.link;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.platform.source.TesterIdentifiable;

/**
 * Simple tests for a resolved link.
 */
@Test
public class ResolvedLinkTest {

  public void linkConstructionDisallowsNulls() {
    assertThrowsIllegalArg(() -> Link.resolved(null));
  }

  public void resolveReturnsOriginalBean() {
    TesterIdentifiable bean = TesterIdentifiable.builder().build();
    Link<TesterIdentifiable> link = Link.resolved(bean);
    assertThat(link.resolve(null)).isSameAs(bean);
  }

  public void coverage() {
    coverImmutableBean(new ResolvedLink<>(TesterIdentifiable.builder().build()));
  }
}
