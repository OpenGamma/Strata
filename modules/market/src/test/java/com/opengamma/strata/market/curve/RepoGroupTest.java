/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link RepoGroup}.
 */
public class RepoGroupTest {

  @Test
  public void coverage() {
    RepoGroup test = RepoGroup.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
  }

}
