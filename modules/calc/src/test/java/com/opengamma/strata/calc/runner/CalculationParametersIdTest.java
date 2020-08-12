/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CalculationParametersId}.
 */
public class CalculationParametersIdTest {

  @Test
  public void test_name() {
    CalculationParametersId test = CalculationParametersId.of("foo");
    assertThat(test.getName()).isEqualTo("foo");
    assertThat(test.getMarketDataType()).isEqualTo(CalculationParameters.class);
  }

  @Test
  public void coverage() {
    CalculationParametersId test = CalculationParametersId.of("foo");
    coverImmutableBean(test);
    CalculationParametersId test2 = CalculationParametersId.of("bar");
    coverBeanEquals(test, test2);
  }

}
