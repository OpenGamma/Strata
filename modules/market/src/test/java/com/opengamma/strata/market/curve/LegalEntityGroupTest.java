/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link LegalEntityGroup}.
 */
public class LegalEntityGroupTest {

  @Test
  public void coverage() {
    LegalEntityGroup test = LegalEntityGroup.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
  }

}
