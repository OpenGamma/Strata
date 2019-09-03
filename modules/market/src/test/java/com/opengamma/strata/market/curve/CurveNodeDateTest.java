/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CurveNodeDate}.
 */
public class CurveNodeDateTest {

  private static final LocalDate DATE1 = date(2015, 6, 30);
  private static final LocalDate DATE2 = date(2015, 7, 1);
  private static final LocalDate DATE3 = date(2015, 7, 2);

  //-------------------------------------------------------------------------
  @Test
  public void test_END() {
    CurveNodeDate test = CurveNodeDate.END;
    assertThat(test.isFixed()).isFalse();
    assertThat(test.isEnd()).isTrue();
    assertThat(test.isLastFixing()).isFalse();
    assertThat(test.getType()).isEqualTo(CurveNodeDateType.END);
    assertThatIllegalStateException()
        .isThrownBy(() -> test.getDate());
  }

  @Test
  public void test_LAST_FIXING() {
    CurveNodeDate test = CurveNodeDate.LAST_FIXING;
    assertThat(test.isFixed()).isFalse();
    assertThat(test.isEnd()).isFalse();
    assertThat(test.isLastFixing()).isTrue();
    assertThat(test.getType()).isEqualTo(CurveNodeDateType.LAST_FIXING);
    assertThatIllegalStateException()
        .isThrownBy(() -> test.getDate());
  }

  @Test
  public void test_of() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    assertThat(test.isFixed()).isTrue();
    assertThat(test.isEnd()).isFalse();
    assertThat(test.isLastFixing()).isFalse();
    assertThat(test.getType()).isEqualTo(CurveNodeDateType.FIXED);
    assertThat(test.getDate()).isEqualTo(DATE1);
  }

  @Test
  public void test_builder_fixed() {
    CurveNodeDate test = CurveNodeDate.meta().builder()
        .set(CurveNodeDate.meta().type(), CurveNodeDateType.FIXED)
        .set(CurveNodeDate.meta().date(), DATE1)
        .build();
    assertThat(test.isFixed()).isTrue();
    assertThat(test.isEnd()).isFalse();
    assertThat(test.isLastFixing()).isFalse();
    assertThat(test.getType()).isEqualTo(CurveNodeDateType.FIXED);
    assertThat(test.getDate()).isEqualTo(DATE1);
  }

  @Test
  public void test_builder_incorrect_no_fixed_date() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeDate.meta().builder()
            .set(CurveNodeDate.meta().type(), CurveNodeDateType.FIXED)
            .build());
  }

  @Test
  public void test_builder_incorrect_fixed_date() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeDate.meta().builder()
            .set(CurveNodeDate.meta().type(), CurveNodeDateType.LAST_FIXING)
            .set(CurveNodeDate.meta().date(), DATE1)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_calculate() {
    assertThat(CurveNodeDate.of(DATE1).calculate(() -> DATE2, () -> DATE3)).isEqualTo(DATE1);
    assertThat(CurveNodeDate.END.calculate(() -> DATE2, () -> DATE3)).isEqualTo(DATE2);
    assertThat(CurveNodeDate.LAST_FIXING.calculate(() -> DATE2, () -> DATE3)).isEqualTo(DATE3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    coverImmutableBean(test);
    CurveNodeDate test2 = CurveNodeDate.LAST_FIXING;
    coverBeanEquals(test, test2);
    coverEnum(CurveNodeDateType.class);
  }

  @Test
  public void test_serialization() {
    CurveNodeDate test = CurveNodeDate.of(DATE1);
    assertSerialization(test);
  }

}
