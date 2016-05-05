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
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link CalculationParameters}.
 */
@Test
public class CalculationParametersTest {

  public void of() {
    CalculationParameters test = CalculationParameters.of(ReportingCurrency.NATURAL);
    assertEquals(test.getParameters().size(), 1);
    assertEquals(test.findParameter(ReportingCurrency.class), Optional.of(ReportingCurrency.NATURAL));
  }

  public void of_empty() {
    CalculationParameters test = CalculationParameters.of();
    assertEquals(test.getParameters().size(), 0);
  }

  public void of_list() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(ReportingCurrency.NATURAL));
    assertEquals(test.getParameters().size(), 1);
    assertEquals(test.findParameter(ReportingCurrency.class), Optional.of(ReportingCurrency.NATURAL));
  }

  public void of_list_empty() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of());
    assertEquals(test.getParameters().size(), 0);
  }

  public void getParameter() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(ReportingCurrency.NATURAL));
    assertEquals(test.getParameter(ReportingCurrency.class), ReportingCurrency.NATURAL);
    assertThrowsIllegalArg(() -> test.getParameter(TestParameter.class));
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    CalculationParameters test1 = CalculationParameters.of(ReportingCurrency.NATURAL);
    CalculationParameters test2 = CalculationParameters.of(ImmutableList.of());

    assertEquals(test1.combinedWith(test2).getParameters().size(), 1);
    assertEquals(test1.combinedWith(test2).getParameters().get(ReportingCurrency.class), ReportingCurrency.NATURAL);

    assertEquals(test2.combinedWith(test1).getParameters().size(), 1);
    assertEquals(test2.combinedWith(test1).getParameters().get(ReportingCurrency.class), ReportingCurrency.NATURAL);

    assertEquals(test1.combinedWith(test1).getParameters().size(), 1);
    assertEquals(test1.combinedWith(test1).getParameters().get(ReportingCurrency.class), ReportingCurrency.NATURAL);
  }

  public void test_filter() {
    CalculationParameters test = CalculationParameters.of(ReportingCurrency.NATURAL);
    TestTarget target = new TestTarget();

    CalculationParameters filtered1 = test.filter(target, Measures.PRESENT_VALUE);
    assertEquals(filtered1.getParameters().size(), 1);
    assertEquals(filtered1.getParameters().get(ReportingCurrency.class), ReportingCurrency.NATURAL);

    CalculationParameters filtered2 = test.filter(target, Measures.PAR_RATE);
    assertEquals(filtered2.getParameters().size(), 0);
  }

  public void test_without() {
    CalculationParameters test = CalculationParameters.of(ReportingCurrency.NATURAL);

    CalculationParameters filtered1 = test.without(ReportingCurrency.class);
    assertEquals(filtered1.getParameters().size(), 0);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationParameters test = CalculationParameters.of(ReportingCurrency.NATURAL);
    coverImmutableBean(test);
    CalculationParameters test2 = CalculationParameters.empty();
    coverBeanEquals(test, test2);
    assertNotNull(CalculationParameters.meta());
  }

  //-------------------------------------------------------------------------
  private static final class TestParameter implements CalculationParameter {
  }

}
