/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationResult}.
 */
public class CalculationResultTest {

  private static final Result<String> RESULT = Result.success("OK");
  private static final Result<String> RESULT2 = Result.success("OK2");
  private static final Result<String> FAILURE = Result.failure(FailureReason.NOT_APPLICABLE, "N/A");

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    assertThat(test.getRowIndex()).isEqualTo(1);
    assertThat(test.getColumnIndex()).isEqualTo(2);
    assertThat(test.getResult()).isEqualTo(RESULT);
    assertThat(test.getResult(String.class)).isEqualTo(RESULT);
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test.getResult(Integer.class));
  }

  @Test
  public void of_failure() {
    CalculationResult test = CalculationResult.of(1, 2, FAILURE);
    assertThat(test.getRowIndex()).isEqualTo(1);
    assertThat(test.getColumnIndex()).isEqualTo(2);
    assertThat(test.getResult()).isEqualTo(FAILURE);
    assertThat(test.getResult(String.class)).isEqualTo(FAILURE);
    assertThat(test.getResult(Integer.class)).isEqualTo(FAILURE);  // cannot throw exception as generic type not known
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CalculationResult test = CalculationResult.of(1, 2, RESULT);
    coverImmutableBean(test);
    CalculationResult test2 = CalculationResult.of(0, 3, RESULT2);
    coverBeanEquals(test, test2);
    assertThat(CalculationResult.meta()).isNotNull();
  }

}
