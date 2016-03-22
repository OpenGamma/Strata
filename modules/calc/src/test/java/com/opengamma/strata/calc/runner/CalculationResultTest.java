/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationResult}.
 */
@Test
public class CalculationResultTest {

  private static final Result<String> RESULT = Result.success("OK");
  private static final Result<String> RESULT2 = Result.success("OK2");

  //-------------------------------------------------------------------------
  public void of() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getResult(), RESULT);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    coverImmutableBean(test);
    CalculationResult test2 = CalculationResult.of(0, 3, RESULT2);
    coverBeanEquals(test, test2);
  }

}
