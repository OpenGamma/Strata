/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link Results}.
 */
public class ResultsTest {

  private static final ColumnName NAME_A = ColumnName.of("A");
  private static final ColumnName NAME_B = ColumnName.of("B");
  private static final ColumnName NAME_C = ColumnName.of("C");
  private static final ColumnHeader HEADER1 = ColumnHeader.of(NAME_A, TestingMeasures.PRESENT_VALUE);
  private static final ColumnHeader HEADER2 = ColumnHeader.of(NAME_B, TestingMeasures.PRESENT_VALUE);
  private static final ColumnHeader HEADER3 = ColumnHeader.of(NAME_C, TestingMeasures.PRESENT_VALUE);

  @Test
  public void test_empty() {
    Results test = Results.of(ImmutableList.of(), ImmutableList.of());
    assertThat(test.getColumns()).isEmpty();
    assertThat(test.getRowCount()).isEqualTo(0);
    assertThat(test.getColumnCount()).isEqualTo(0);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, 0))
        .withMessageStartingWith("Row index must be greater than or");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, 0, String.class))
        .withMessageStartingWith("Row index must be greater than or");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, NAME_A))
        .withMessageStartingWith("Column name not found");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, NAME_A, String.class))
        .withMessageStartingWith("Column name not found");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.columnResults(0))
        .withMessageStartingWith("Column index must be greater than or");
  }

  @Test
  public void nonEmpty() {
    Results test = Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results("1", "2", "3", "4", "5", "6"));
    assertThat(test.getColumns()).containsExactly(HEADER1, HEADER2, HEADER3);
    assertThat(test.getRowCount()).isEqualTo(2);
    assertThat(test.getColumnCount()).isEqualTo(3);
    assertThat(test.get(0, 0).getValue()).isEqualTo("1");
    assertThat(test.get(0, 0, String.class).getValue()).isEqualTo("1");
    assertThat(test.get(0, NAME_A).getValue()).isEqualTo("1");
    assertThat(test.get(0, NAME_A, String.class).getValue()).isEqualTo("1");
    assertThat(test.get(0, NAME_B).getValue()).isEqualTo("2");
    assertThat(test.get(0, NAME_B, String.class).getValue()).isEqualTo("2");
    assertThat(test.get(1, 2).getValue()).isEqualTo("6");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(-1, 0))
        .withMessageStartingWith("Row index must be greater than or");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(2, 0))
        .withMessageStartingWith("Row index must be greater than or");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, -1))
        .withMessageStartingWith("Column index must be greater than or");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.get(0, 3))
        .withMessageStartingWith("Column index must be greater than or");
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test.get(0, 0, Integer.class))
        .withMessage("Result queried with type 'java.lang.Integer' but was 'java.lang.String'");
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test.get(0, NAME_A, Integer.class))
        .withMessage("Result queried with type 'java.lang.Integer' but was 'java.lang.String'");
  }

  @Test
  public void stream() {
    Results test = Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results("1", "2", "3", "4", "5", "6"));
    assertThat(test.getColumns()).containsExactly(HEADER1, HEADER2, HEADER3);
    assertThat(test.getRowCount()).isEqualTo(2);
    assertThat(test.getColumnCount()).isEqualTo(3);
    assertThat(test.columnResults(0)).containsExactly(Result.success("1"), Result.success("4"));
    assertThat(test.columnResults(1, String.class)).containsExactly(Result.success("2"), Result.success("5"));
    assertThat(test.columnResults(2, String.class)).containsExactly(Result.success("3"), Result.success("6"));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.columnResults(3))
        .withMessageStartingWith("Column index must be greater than or");
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> test.columnResults(1, Integer.class).collect(toImmutableList()))
        .withMessage("Result queried with type 'java.lang.Integer' but was 'java.lang.String'");
  }

  @Test
  public void stream_split() {
    ImmutableList<Result<String>> resultList = IntStream.range(0, 1000)
        .mapToObj(i -> Result.success(Integer.toString(i)))
        .collect(toImmutableList());
    Results test = Results.of(ImmutableList.of(HEADER1), resultList);
    assertThat(test.columnResults(0).count()).isEqualTo(1000);
    // Streams.findLast uses trySplit()
    assertThat(Streams.findLast(test.columnResults(0))).hasValue(Result.success("999"));
    // hopefully the large list size works with parallel
    List<Result<?>> collected = test.columnResults(0).parallel()
        .map(result -> Result.success(result.getValue().toString() + " " + Thread.currentThread().getName()))
        .collect(toImmutableList());
    System.out.println(collected);
    assertThat(collected).hasSize(1000);
    assertThat(collected.get(0).getValue().toString()).startsWith("0 ");
    assertThat(collected.get(999).getValue().toString()).startsWith("999 ");
  }

  /**
   * Tests that it's not possible to create results with invalid combinations of row and column
   * count and number of items
   */
  @Test
  public void createInvalid() {
    // Zero columns, non-zero cells
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Results.of(ImmutableList.of(), results(1)))
        .withMessageStartingWith("The number of cells");
    // More columns than cells
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results(1)))
        .withMessageStartingWith("The number of cells");
  }

  @SafeVarargs
  private static <T> List<Result<T>> results(T... items) {
    return Arrays.stream(items).map(Result::success).collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Results test = Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results(1, 2, 3, 4, 5, 6));
    coverImmutableBean(test);
    Results test2 = Results.of(ImmutableList.of(HEADER1), results(9));
    coverBeanEquals(test, test2);
  }

}
