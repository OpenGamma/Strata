/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationResults}.
 */
public class CalculationResultsTest {

  private static final CalculationTarget TARGET = new CalculationTarget() {};
  private static final CalculationTarget TARGET2 = new CalculationTarget() {};
  private static final Result<String> RESULT = Result.success("OK");
  private static final CalculationResult CALC_RESULT = CalculationResult.of(1, 2, RESULT);
  private static final CalculationResult CALC_RESULT2 = CalculationResult.of(1, 2, RESULT);

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    CalculationResults test = CalculationResults.of(TARGET, ImmutableList.of(CALC_RESULT));
    assertThat(test.getTarget()).isEqualTo(TARGET);
    assertThat(test.getCells()).containsExactly(CALC_RESULT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CalculationResults test = CalculationResults.of(TARGET, ImmutableList.of(CALC_RESULT));
    coverImmutableBean(test);
    CalculationResults test2 = CalculationResults.of(TARGET2, ImmutableList.of(CALC_RESULT2));
    coverBeanEquals(test, test2);
    assertThat(CalculationResults.meta()).isNotNull();
  }

}
