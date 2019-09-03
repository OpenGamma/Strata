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
 * Test {@link MoneynessType}.
 */
public class MoneynessTypeTest {

  @Test
  public void test_basics() {
    assertThat(MoneynessType.of("Price")).isEqualTo(MoneynessType.PRICE);
    assertThat(MoneynessType.of("Rates")).isEqualTo(MoneynessType.RATES);
    assertThat(MoneynessType.PRICE.toString()).isEqualTo("Price");
    assertThat(MoneynessType.RATES.toString()).isEqualTo("Rates");
  }

  @Test
  public void coverage() {
    coverEnum(MoneynessType.class);
  }

}
