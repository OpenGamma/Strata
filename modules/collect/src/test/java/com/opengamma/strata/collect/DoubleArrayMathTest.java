/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.DoubleBinaryOperator;

import org.testng.annotations.Test;

/**
 * Test {@link DoubleArrayMath}.
 */
@Test
public class DoubleArrayMathTest {

  private static final double[] ARRAY_0_0 = new double[] {-1e-4, 1e-3};
  private static final double[] ARRAY_1_2 = new double[] {1, 2};
  private static final double[] ARRAY_1_2B = new double[] {1 - 1e-4, 2 + 1e-3};
  private static final double[] ARRAY_3_4 = new double[] {3, 4};
  private static final double[] ARRAY_3 = new double[] {3};

  public void test_EMPTY_DOUBLE_ARRAY() {
    assertThat(DoubleArrayMath.EMPTY_DOUBLE_ARRAY).contains();
  }

  public void test_EMPTY_DOUBLE_OBJECT_ARRAY() {
    assertThat(DoubleArrayMath.EMPTY_DOUBLE_OBJECT_ARRAY).contains();
  }

  //-------------------------------------------------------------------------
  public void test_combineByAddition() {
    assertThat(DoubleArrayMath.combineByAddition(ARRAY_1_2, ARRAY_3_4)).contains(4d, 6d);
    assertThrowsIllegalArg(() -> DoubleArrayMath.combineByAddition(ARRAY_1_2, ARRAY_3));
  }

  public void test_combineByMultiplication() {
    assertThat(DoubleArrayMath.combineByMultiplication(ARRAY_1_2, ARRAY_3_4)).contains(3d, 8d);
    assertThrowsIllegalArg(() -> DoubleArrayMath.combineByMultiplication(ARRAY_1_2, ARRAY_3));
  }

  public void test_combine() {
    DoubleBinaryOperator operator = (a, b) -> a / b;
    assertThat(DoubleArrayMath.combine(ARRAY_1_2, ARRAY_3_4, operator)).contains(1d / 3d, 2d / 4d);
    assertThrowsIllegalArg(() -> DoubleArrayMath.combine(ARRAY_1_2, ARRAY_3, operator));
  }

  public void test_combineLenient() {
    DoubleBinaryOperator operator = (a, b) -> a / b;
    assertThat(DoubleArrayMath.combineLenient(ARRAY_1_2, ARRAY_3_4, operator)).contains(1d / 3d, 2d / 4d);
    assertThat(DoubleArrayMath.combineLenient(ARRAY_1_2, ARRAY_3, operator)).contains(1d / 3d, 2d);
    assertThat(DoubleArrayMath.combineLenient(ARRAY_3, ARRAY_1_2, operator)).contains(3d / 1d, 2d);
  }

  //-------------------------------------------------------------------------
  public void test_fuzzyEqualsZero() {
    assertThat(DoubleArrayMath.fuzzyEqualsZero(DoubleArrayMath.EMPTY_DOUBLE_ARRAY, 1e-2)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEqualsZero(ARRAY_0_0, 1e-2)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEqualsZero(ARRAY_1_2, 1e-2)).isFalse();
  }

  public void test_fuzzyEquals() {
    assertThat(DoubleArrayMath.fuzzyEquals(DoubleArrayMath.EMPTY_DOUBLE_ARRAY, ARRAY_0_0, 1e-2)).isFalse();
    assertThat(DoubleArrayMath.fuzzyEquals(ARRAY_0_0, ARRAY_0_0, 1e-2)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(ARRAY_1_2, ARRAY_1_2, 1e-2)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(ARRAY_1_2, ARRAY_1_2B, 1e-2)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(ARRAY_1_2, ARRAY_1_2B, 1e-3)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(ARRAY_1_2, ARRAY_1_2B, 1e-4)).isFalse();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DoubleArrayMath.class);
  }

}
