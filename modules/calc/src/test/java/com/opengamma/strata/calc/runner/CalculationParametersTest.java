/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link CalculationParameters}.
 */
@Test
public class CalculationParametersTest {

  private static final CalculationParameter PARAM = new TestParameter();
  private static final CalculationParameter PARAM2 = new TestParameter2();

  //-------------------------------------------------------------------------
  public void of() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    assertEquals(test.getParameters().size(), 1);
    assertEquals(test.findParameter(TestParameter.class), Optional.of(PARAM));
  }

  public void of_empty() {
    CalculationParameters test = CalculationParameters.of();
    assertEquals(test.getParameters().size(), 0);
  }

  public void of_list() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(PARAM));
    assertEquals(test.getParameters().size(), 1);
    assertEquals(test.findParameter(TestParameter.class), Optional.of(PARAM));
  }

  public void of_list_empty() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of());
    assertEquals(test.getParameters().size(), 0);
  }

  public void getParameter() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(PARAM));
    assertEquals(test.getParameter(TestParameter.class), PARAM);
    assertThrowsIllegalArg(() -> test.getParameter(TestParameter2.class));
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    CalculationParameters test1 = CalculationParameters.of(PARAM);
    CalculationParameters test2 = CalculationParameters.of(ImmutableList.of());

    assertEquals(test1.combinedWith(test2).getParameters().size(), 1);
    assertEquals(test1.combinedWith(test2).getParameters().get(TestParameter.class), PARAM);

    assertEquals(test2.combinedWith(test1).getParameters().size(), 1);
    assertEquals(test2.combinedWith(test1).getParameters().get(TestParameter.class), PARAM);

    assertEquals(test1.combinedWith(test1).getParameters().size(), 1);
    assertEquals(test1.combinedWith(test1).getParameters().get(TestParameter.class), PARAM);
  }

  public void test_with_add() {
    CalculationParameters test = CalculationParameters.of(PARAM).with(PARAM2);
    assertEquals(test.getParameters().size(), 2);
  }

  public void test_with_replace() {
    CalculationParameters test = CalculationParameters.of(PARAM).with(PARAM);
    assertEquals(test.getParameters().size(), 1);
  }

  public void test_without_typeFound() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    CalculationParameters filtered1 = test.without(TestParameter.class);
    assertEquals(filtered1.getParameters().size(), 0);
  }

  public void test_without_typeNotFound() {
    CalculationParameters test = CalculationParameters.empty();
    CalculationParameters filtered1 = test.without(TestParameter.class);
    assertEquals(filtered1.getParameters().size(), 0);
  }

  public void test_filter() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    TestTarget target = new TestTarget();

    CalculationParameters filtered1 = test.filter(target, TestingMeasures.PRESENT_VALUE);
    assertEquals(filtered1.getParameters().size(), 1);
    assertEquals(filtered1.getParameters().get(TestParameter.class), PARAM);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    coverImmutableBean(test);
    CalculationParameters test2 = CalculationParameters.empty();
    coverBeanEquals(test, test2);
    assertNotNull(CalculationParameters.meta());
  }

}
