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
import com.opengamma.platform.source.id.StandardId;

/**
 * Simple tests for a resolvable link.
 */
@Test
public class ResolvableLinkTest {

  public void linkConstructionDisallowsNulls() {
    assertThrowsIllegalArg(() -> Link.resolvable(null, TesterIdentifiable.class));
    assertThrowsIllegalArg(() -> Link.resolvable(StandardId.of("some_scheme", "1234"), null));
  }

  public void successfulConstruction() {
    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);
    assertThat(link).isNotNull();
  }

  public void resolverMustNotBeNull() {
    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);
    assertThrowsIllegalArg(() -> link.resolve(null));
  }

  public void coverage() {
    coverImmutableBean(new ResolvableLink<>(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class));
  }

}
