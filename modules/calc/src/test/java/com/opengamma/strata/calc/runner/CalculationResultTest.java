/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationResult}.
 */
@Test
public class CalculationResultTest {

  private static final Result<String> RESULT = Result.success("OK");
  private static final Result<String> RESULT2 = Result.success("OK2");
  private static final Result<String> FAILURE = Result.failure(FailureReason.NOT_APPLICABLE, "N/A");

  //-------------------------------------------------------------------------
  public void of() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getResult(), RESULT);
    assertEquals(test.getResult(String.class), RESULT);
    assertThrows(() -> test.getResult(Integer.class), ClassCastException.class);
  }

  public void of_failure() {
    CalculationResult test = CalculationResult.of(1, 2, FAILURE);
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getResult(), FAILURE);
    assertEquals(test.getResult(String.class), FAILURE);
    assertEquals(test.getResult(Integer.class), FAILURE);  // cannot throw exception as generic type not known
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    coverImmutableBean(test);
    CalculationResult test2 = CalculationResult.of(0, 3, RESULT2);
    coverBeanEquals(test, test2);
    assertNotNull(CalculationResult.meta());
  }

}
