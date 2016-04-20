/**
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
import com.opengamma.strata.calc.config.ReportingCurrency;

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

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationParameters test = CalculationParameters.of(ReportingCurrency.NATURAL);
    coverImmutableBean(test);
    CalculationParameters test2 = CalculationParameters.empty();
    coverBeanEquals(test, test2);
    assertNotNull(CalculationParameters.meta());
  }

}
