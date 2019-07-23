/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/**
 * Test {@link LocalDateDoublePoint}
 */
public class LocalDateDoublePointTest {

  private static final LocalDate DATE_2012_06_29 = LocalDate.of(2012, 6, 29);
  private static final LocalDate DATE_2012_06_30 = LocalDate.of(2012, 6, 30);
  private static final LocalDate DATE_2012_07_01 = LocalDate.of(2012, 7, 1);
  private static final Offset<Double> TOLERANCE = within(0.00001d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    LocalDateDoublePoint test = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    assertThat(test.getDate()).isEqualTo(DATE_2012_06_30);
    assertThat(test.getValue()).isEqualTo(1d, TOLERANCE);
  }

  @Test
  public void test_of_nullDate() {
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoublePoint.of(null, 1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withDate() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint test = base.withDate(DATE_2012_06_29);
    assertThat(test.getDate()).isEqualTo(DATE_2012_06_29);
    assertThat(test.getValue()).isEqualTo(1d, TOLERANCE);
  }

  @Test
  public void test_withDate_nullDate() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    assertThatIllegalArgumentException().isThrownBy(() -> base.withDate(null));
  }

  @Test
  public void test_withValue() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint test = base.withValue(2d);
    assertThat(test.getDate()).isEqualTo(DATE_2012_06_30);
    assertThat(test.getValue()).isEqualTo(2d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    LocalDateDoublePoint a = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_07_01, 1d);

    List<LocalDateDoublePoint> list = new ArrayList<>();
    list.add(a);
    list.add(b);
    list.add(c);
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(a, b, c);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(c, b, a);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode_differentDates() {
    LocalDateDoublePoint a1 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint a2 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_07_01, 1d);

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a1.hashCode());
  }

  @Test
  public void test_equalsHashCode_differentValues() {
    LocalDateDoublePoint a1 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint a2 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_29, 2d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_06_29, 3d);

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a1.hashCode());
  }

  @Test
  public void test_equalsBad() {
    LocalDateDoublePoint a = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    assertThat(a.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a.equals(null)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toString() {
    LocalDateDoublePoint test = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    assertThat(test.toString()).isEqualTo("(2012-06-29=1.0)");
  }

}
