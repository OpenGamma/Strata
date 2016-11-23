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
import java.util.function.DoubleUnaryOperator;

import org.testng.annotations.AfterMethod;
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

  public void toPrimitive() {
    assertThat(DoubleArrayMath.toPrimitive(new Double[] {})).isEqualTo(new double[] {});
    assertThat(DoubleArrayMath.toPrimitive(new Double[] {1d, 2.5d})).isEqualTo(new double[] {1d, 2.5d});
  }

  public void toObject() {
    assertThat(DoubleArrayMath.toObject(new double[] {})).isEqualTo(new Double[] {});
    assertThat(DoubleArrayMath.toObject(new double[] {1d, 2.5d})).isEqualTo(new Double[] {1d, 2.5d});
  }

  //-------------------------------------------------------------------------
  public void test_sum() {
    assertThat(DoubleArrayMath.sum(ARRAY_1_2)).isEqualTo(3d);
  }

  //-------------------------------------------------------------------------
  public void test_applyAddition() {
    assertThat(DoubleArrayMath.applyAddition(ARRAY_1_2, 2d)).contains(3d, 4d);
  }

  public void test_applyMultiplication() {
    assertThat(DoubleArrayMath.applyMultiplication(ARRAY_1_2, 4d)).contains(4d, 8d);
  }

  public void test_apply() {
    DoubleUnaryOperator operator = a -> 1 / a;
    assertThat(DoubleArrayMath.apply(ARRAY_1_2, operator)).contains(1d, 1d / 2d);
  }

  //-------------------------------------------------------------------------
  public void test_mutateByAddition_byConstant() {
    double[] testArray = ARRAY_1_2.clone();
    DoubleArrayMath.mutateByAddition(testArray, 2d);
    assertThat(testArray).contains(3d, 4d);
  }

  public void test_mutateByMultiplication_byConstant() {
    double[] testArray = ARRAY_1_2.clone();
    DoubleArrayMath.mutateByMultiplication(testArray, 4d);
    assertThat(testArray).contains(4d, 8d);
  }

  public void test_mutateByAddition_byArray() {
    double[] testArray = ARRAY_1_2.clone();
    DoubleArrayMath.mutateByAddition(testArray, new double[] {2d, 3d});
    assertThat(testArray).contains(3d, 5d);
  }

  public void test_mutateByMultiplication_byArray() {
    double[] testArray = ARRAY_1_2.clone();
    DoubleArrayMath.mutateByMultiplication(testArray, new double[] {4d, 5d});
    assertThat(testArray).contains(4d, 10d);
  }

  public void test_mutateByAddition_byArray_sizeDifferent() {
    double[] testArray = ARRAY_1_2.clone();
    assertThrowsIllegalArg(() -> DoubleArrayMath.mutateByAddition(testArray, new double[] {2d}));
  }

  public void test_mutateByMultiplication_byArray_sizeDifferent() {
    double[] testArray = ARRAY_1_2.clone();
    assertThrowsIllegalArg(() -> DoubleArrayMath.mutateByMultiplication(testArray, new double[] {4d}));
  }

  public void test_mutate() {
    DoubleUnaryOperator operator = a -> 1 / a;
    double[] testArray = ARRAY_1_2.clone();
    DoubleArrayMath.mutate(testArray, operator);
    assertThat(testArray).contains(1d, 1d / 2d);
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
  public void test_sortPairs_doubledouble_1() {
    double[] keys = {3d, 5d, 2d, 4d};
    double[] values = {6d, 10d, 4d, 8d};
    DoubleArrayMath.sortPairs(keys, values);
    assertThat(keys).containsExactly(2d, 3d, 4d, 5d);
    assertThat(values).containsExactly(4d, 6d, 8d, 10d);
  }

  public void test_sortPairs_doubledouble_2() {
    double[] keys = {3d, 2d, 5d, 4d};
    double[] values = {6d, 4d, 10d, 8d};
    DoubleArrayMath.sortPairs(keys, values);
    assertThat(keys).containsExactly(2d, 3d, 4d, 5d);
    assertThat(values).containsExactly(4d, 6d, 8d, 10d);
  }

  public void test_sortPairs_doubledouble_sizeDifferent() {
    double[] keys = {3d, 2d, 5d, 4d};
    double[] values = {6d, 4d};
    assertThrowsIllegalArg(() -> DoubleArrayMath.sortPairs(keys, values));
  }

  //-------------------------------------------------------------------------
  public void test_sortPairs_doubleObject_1() {
    double[] keys = {3d, 5d, 2d, 4d};
    Integer[] values = {6, 10, 4, 8};
    DoubleArrayMath.sortPairs(keys, values);
    assertThat(keys).containsExactly(2d, 3d, 4d, 5d);
    assertThat(values).containsExactly(4, 6, 8, 10);
  }

  public void test_sortPairs_doubleObject_2() {
    double[] keys = {3d, 2d, 5d, 4d};
    Integer[] values = {6, 4, 10, 8};
    DoubleArrayMath.sortPairs(keys, values);
    assertThat(keys).containsExactly(2d, 3d, 4d, 5d);
    assertThat(values).containsExactly(4, 6, 8, 10);
  }

  public void test_sortPairs_doubleObject_sizeDifferent() {
    double[] keys = {3d, 2d, 5d, 4d};
    Integer[] values = {6, 4};
    assertThrowsIllegalArg(() -> DoubleArrayMath.sortPairs(keys, values));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DoubleArrayMath.class);
  }

  //-------------------------------------------------------------------------
  @AfterMethod
  private void assertInputsUnchanged() {
    assertThat(ARRAY_0_0).containsExactly(-1e-4, 1e-3);
    assertThat(ARRAY_1_2).containsExactly(1d, 2d);
    assertThat(ARRAY_1_2B).containsExactly(1 - 1e-4, 2 + 1e-3);
    assertThat(ARRAY_3_4).containsExactly(3d, 4d);
    assertThat(ARRAY_3).containsExactly(3d);
  }

}
