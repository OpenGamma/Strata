/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Period;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link CsvRow}.
 */
public class CsvRowTest {

  private static final ImmutableList<String> HEADERS = ImmutableList.of("A", "B", "C", "A", "B");
  private static final ImmutableMap<String, Integer> SEARCH_HEADERS = ImmutableMap.of("a", 0, "b", 1, "c", 2);
  private static final ImmutableList<String> FIELDS = ImmutableList.of("m", "", "P1D", "p", "q");

  //-------------------------------------------------------------------------
  @Test
  public void test_basics() {
    CsvRow row = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    assertThat(row.headers()).isSameAs(HEADERS);
    assertThat(row.fields()).isSameAs(FIELDS);
    assertThat(row.fieldCount()).isEqualTo(5);
    assertThat(row.field(0)).isEqualTo("m");
    assertThat(row.field(4)).isEqualTo("q");
    assertThat(row.lineNumber()).isEqualTo(1);
    assertThat(row.lineNumber()).isEqualTo(1);
  }

  @Test
  public void test_getField_String() {
    CsvRow row = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    assertThat(row.getField("A")).isEqualTo("m");
    assertThat(row.getField("B")).isEqualTo("");
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField("X"));

    assertThat(row.getField("C", Period::parse)).isEqualTo(Period.ofDays(1));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField("C", Integer::parseInt));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField("X", Period::parse));
  }

  @Test
  public void test_getField_Pattern() {
    CsvRow row = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    assertThat(row.getField(Pattern.compile("A"))).isEqualTo("m");
    assertThat(row.getField(Pattern.compile("B"))).isEqualTo("");
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField(Pattern.compile("X")));

    assertThat(row.getField(Pattern.compile("C"), Period::parse)).isEqualTo(Period.ofDays(1));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField(Pattern.compile("C"), Integer::parseInt));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getField(Pattern.compile("X"), Period::parse));
  }

  @Test
  public void test_getValue() {
    CsvRow row = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    assertThat(row.getValue("A")).isEqualTo("m");
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue("B"));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue("X"));

    assertThat(row.getValue("C", Period::parse)).isEqualTo(Period.ofDays(1));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue("C", Integer::parseInt));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue("X", Period::parse));
  }

  @Test
  public void test_getValue_Pattern() {
    CsvRow row = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    assertThat(row.getValue(Pattern.compile("A"))).isEqualTo("m");
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue(Pattern.compile("B")));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue(Pattern.compile("X")));

    assertThat(row.getValue(Pattern.compile("C"), Period::parse)).isEqualTo(Period.ofDays(1));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue(Pattern.compile("C"), Integer::parseInt));
    assertThatIllegalArgumentException().isThrownBy(() -> row.getValue(Pattern.compile("X"), Period::parse));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCodeToString() {
    CsvRow a = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    CsvRow a2 = new CsvRow(HEADERS, SEARCH_HEADERS, 1, FIELDS);
    CsvRow b = new CsvRow(ImmutableList.of(), SEARCH_HEADERS, 1, FIELDS);
    CsvRow c = new CsvRow(HEADERS, SEARCH_HEADERS, 1, ImmutableList.of());
    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
    assertThat(a.toString()).isNotBlank();
  }

}
