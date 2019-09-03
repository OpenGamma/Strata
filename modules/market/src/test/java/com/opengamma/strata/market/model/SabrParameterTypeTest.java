/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.model;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SabrParameterType}.
 */
public class SabrParameterTypeTest {

  @Test
  public void test_basics() {
    assertThat(SabrParameterType.ALPHA.name()).isEqualTo("ALPHA");
    assertThat(SabrParameterType.ALPHA.toString()).isEqualTo("Alpha");
    assertThat(SabrParameterType.BETA.toString()).isEqualTo("Beta");
  }

  @Test
  public void coverage() {
    coverEnum(SabrParameterType.class);
  }

}
