/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestFunction;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link CalculationFunctions}.
 */
@Test
public class CalculationFunctionsTest {

  private static final CalculationFunction<TestTarget> TARGET = new TestFunction();

  public void empty() {
    CalculationFunctions test = CalculationFunctions.empty();
    assertEquals(test.getFunction(new TestTarget()).supportedMeasures().size(), 0);
    assertEquals(test.findFunction(new TestTarget()), Optional.empty());
  }

  public void of_array() {
    CalculationFunctions test = CalculationFunctions.of(TARGET);
    assertEquals(test.getFunction(new TestTarget()), TARGET);
    assertEquals(test.findFunction(new TestTarget()), Optional.of(TARGET));
  }

  public void of_list() {
    CalculationFunctions test = CalculationFunctions.of(ImmutableList.of(TARGET));
    assertEquals(test.getFunction(new TestTarget()), TARGET);
    assertEquals(test.findFunction(new TestTarget()), Optional.of(TARGET));
  }

  public void of_map() {
    CalculationFunctions test = CalculationFunctions.of(ImmutableMap.of(TestTarget.class, TARGET));
    assertEquals(test.getFunction(new TestTarget()), TARGET);
    assertEquals(test.findFunction(new TestTarget()), Optional.of(TARGET));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultCalculationFunctions test = DefaultCalculationFunctions.of(ImmutableMap.of(TestTarget.class, TARGET));
    coverImmutableBean(test);
    DefaultCalculationFunctions test2 = DefaultCalculationFunctions.EMPTY;
    coverBeanEquals(test, test2);
    assertNotNull(DefaultCalculationFunctions.meta());
  }

}
