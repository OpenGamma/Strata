/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.result.Result;

@Test
public class ResultsTest {

  /**
   * Tests that empty results can be constructed but can't provide any data.
   */
  public void empty() {
    Results results = Results.of(0, ImmutableList.of());
    assertThat(results.getRowCount()).isEqualTo(0);
    assertThat(results.getColumnCount()).isEqualTo(0);
    assertThrows(() -> results.get(0, 0), IllegalArgumentException.class, "Row index must be between.*");
  }

  public void nonEmpty() {
    Results results = Results.of(3, results(1, 2, 3, 4, 5, 6));
    assertThat(results.getRowCount()).isEqualTo(2);
    assertThat(results.getColumnCount()).isEqualTo(3);
    assertThat(results.get(0, 0)).hasValue(1);
    assertThat(results.get(1, 2)).hasValue(6);
    assertThrows(() -> results.get(2, 0), IllegalArgumentException.class, "Row index must be between.*");
    assertThrows(() -> results.get(0, 3), IllegalArgumentException.class, "Column index must be between.*");
  }

  /**
   * Tests that it's not possible to create results with invalid combinations of column count and number of values
   */
  public void createInvalid() {
    // Zero columns, non-zero values
    assertThrows(() -> Results.of(0, results(1)), IllegalArgumentException.class, "The values must contain.*");
    // Zero values, non-zero columns
    assertThrows(() -> Results.of(2, results(0)), IllegalArgumentException.class, "The values must contain.*");
    // Number of values not divisible by number of columns
    assertThrows(() -> Results.of(2, results(3)), IllegalArgumentException.class, "The values must contain.*");
    // Negative number of columns
    assertThrows(() -> Results.of(-2, results(4)), IllegalArgumentException.class, ".* must not be negative");
  }

  @SafeVarargs
  private static <T> List<Result<T>> results(T... values) {
    return Arrays.stream(values).map(Result::success).collect(toImmutableList());
  }
}
