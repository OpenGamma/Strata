/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

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
    Results results = Results.of(0, 0, ImmutableList.of());
    assertThat(results.getRowCount()).isEqualTo(0);
    assertThat(results.getColumnCount()).isEqualTo(0);
    assertThrows(() -> results.get(0, 0), IllegalArgumentException.class, "Row index must be greater than or.*");
  }

  public void nonEmpty() {
    Results results = Results.of(2, 3, results(1, 2, 3, 4, 5, 6));
    assertThat(results.getRowCount()).isEqualTo(2);
    assertThat(results.getColumnCount()).isEqualTo(3);
    assertThat(results.get(0, 0)).hasValue(1);
    assertThat(results.get(1, 2)).hasValue(6);
    assertThrows(() -> results.get(2, 0), IllegalArgumentException.class, "Row index must be greater than or.*");
    assertThrows(() -> results.get(0, 3), IllegalArgumentException.class, "Column index must be greater than or.*");
  }

  /**
   * Tests that it's not possible to create results with invalid combinations of row and column
   * count and number of items
   */
  public void createInvalid() {
    // Zero columns, non-zero items
    assertThrows(() -> Results.of(1, 0, results(1)), IllegalArgumentException.class, "The number of items.*");
    // Zero rows, non-zero items
    assertThrows(() -> Results.of(0, 1, results(1)), IllegalArgumentException.class, "The number of items.*");
    // Zero rows and columns, non-zero items
    assertThrows(() -> Results.of(0, 0, results(1)), IllegalArgumentException.class, "The number of items.*");
    // Zero items, non-zero rows and columns
    assertThrows(() -> Results.of(1, 2, results(0)), IllegalArgumentException.class, "The number of items.*");
    // Number of items not divisible by number of columns
    assertThrows(() -> Results.of(1, 2, results(3)), IllegalArgumentException.class, "The number of items.*");
    // Negative number of columns
    assertThrows(() -> Results.of(1, -2, results(4)), IllegalArgumentException.class, ".* must not be negative");
    // Negative number of rows
    assertThrows(() -> Results.of(-1, 2, results(4)), IllegalArgumentException.class, ".* must not be negative");
  }

  @SafeVarargs
  private static <T> List<Result<T>> results(T... items) {
    return Arrays.stream(items).map(Result::success).collect(toImmutableList());
  }
}
