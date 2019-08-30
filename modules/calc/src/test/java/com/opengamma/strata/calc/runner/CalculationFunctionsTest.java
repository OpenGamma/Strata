/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestFunction;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link CalculationFunctions}.
 */
public class CalculationFunctionsTest {

  private static final CalculationFunction<TestTarget> TARGET = new TestFunction();

  @Test
  public void empty() {
    CalculationFunctions test = CalculationFunctions.empty();
    assertThat(test.getFunction(new TestTarget()).supportedMeasures()).hasSize(0);
    assertThat(test.findFunction(new TestTarget())).isEqualTo(Optional.empty());
  }

  @Test
  public void of_array() {
    CalculationFunctions test = CalculationFunctions.of(TARGET);
    assertThat(test.getFunction(new TestTarget())).isEqualTo(TARGET);
    assertThat(test.findFunction(new TestTarget())).isEqualTo(Optional.of(TARGET));
  }

  @Test
  public void of_list() {
    CalculationFunctions test = CalculationFunctions.of(ImmutableList.of(TARGET));
    assertThat(test.getFunction(new TestTarget())).isEqualTo(TARGET);
    assertThat(test.findFunction(new TestTarget())).isEqualTo(Optional.of(TARGET));
  }

  @Test
  public void of_map() {
    CalculationFunctions test = CalculationFunctions.of(ImmutableMap.of(TestTarget.class, TARGET));
    assertThat(test.getFunction(new TestTarget())).isEqualTo(TARGET);
    assertThat(test.findFunction(new TestTarget())).isEqualTo(Optional.of(TARGET));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DefaultCalculationFunctions test = DefaultCalculationFunctions.of(ImmutableMap.of(TestTarget.class, TARGET));
    coverImmutableBean(test);
    DefaultCalculationFunctions test2 = DefaultCalculationFunctions.EMPTY;
    coverBeanEquals(test, test2);
    assertThat(DefaultCalculationFunctions.meta()).isNotNull();
  }

}
