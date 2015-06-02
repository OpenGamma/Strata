/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.function.DoubleBinaryOperator;

import com.google.common.math.DoubleMath;

/**
 * Contains utility methods for maths on double arrays.
 * <p>
 * This utility is used throughout the system when working with double arrays.
 */
public final class DoubleArrayMath {

  /**
   * An empty {@code double} array.
   */
  public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
  /**
   * An empty {@code Double} array.
   */
  public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];

  /**
   * Restricted constructor.
   */
  private DoubleArrayMath() {
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two arrays, returning an array where each element is the sum of the two matching inputs.
   * <p>
   * Each element in the result will be the sum of the matching index in the two input arrays.
   * The two input arrays must have the same length.
   * <p>
   * For example:
   * <pre>
   *  double[] array1 = {1, 5, 9};
   *  double[] array2 = {2, 3, 2};
   *  double[] result = DoubleArrayMath.combineByAddition(array1, array2);
   *  // result contains {3, 8, 11}
   * </pre>
   * 
   * @param array1  the first array
   * @param array2  the second array
   * @return an array combining the two input arrays using the plus operator
   */
  public static double[] combineByAddition(double[] array1, double[] array2) {
    return combine(array1, array2, (a, b) -> a + b);
  }

  /**
   * Combines two arrays, returning an array where each element is the multiplication of the two matching inputs.
   * <p>
   * Each element in the result will be the multiplication of the matching index in the two input arrays.
   * The two input arrays must have the same length.
   * <p>
   * For example:
   * <pre>
   *  double[] array1 = {1, 5, 9};
   *  double[] array2 = {2, 3, 4};
   *  double[] result = DoubleArrayMath.combineByMultiplication(array1, array2);
   *  // result contains {2, 15, 36}
   * </pre>
   * 
   * @param array1  the first array
   * @param array2  the second array
   * @return an array combining the two input arrays using the multiply operator
   */
  public static double[] combineByMultiplication(double[] array1, double[] array2) {
    return combine(array1, array2, (a, b) -> a * b);
  }

  /**
   * Combines two arrays, returning an array where each element is the combination of the two matching inputs.
   * <p>
   * Each element in the result will be the combination of the matching index in the two
   * input arrays using the operator. The two input arrays must have the same length.
   * 
   * @param array1  the first array
   * @param array2  the second array
   * @param operator  the operator to use when combining values
   * @return an array combining the two input arrays using the operator
   */
  public static double[] combine(double[] array1, double[] array2, DoubleBinaryOperator operator) {
    int len1 = array1.length;
    int len2 = array2.length;
    if (len1 != len2) {
      throw new IllegalArgumentException("Arrays cannot be combined as they differ in length");
    }
    double[] result = new double[len1];
    for (int i = 0; i < len1; i++) {
      result[i] = operator.applyAsDouble(array1[i], array2[i]);
    }
    return result;
  }

  /**
   * Combines two arrays, returning an array where each element is the combination of the two matching inputs.
   * <p>
   * Each element in the result will be the combination of the matching index in the two
   * input arrays using the operator.
   * The result will have the length of the longest of the two inputs.
   * Where one array is longer than the other, the values from the longer array will be used.
   * 
   * @param array1  the first array
   * @param array2  the second array
   * @param operator  the operator to use when combining values
   * @return an array combining the two input arrays using the operator
   */
  public static double[] combineLenient(double[] array1, double[] array2, DoubleBinaryOperator operator) {
    int len1 = array1.length;
    int len2 = array2.length;
    if (len1 == len2) {
      return combine(array1, array2, operator);
    }
    int size = Math.max(len1, len2);
    double[] result = new double[size];
    for (int i = 0; i < size; i++) {
      if (i < len1) {
        if (i < len2) {
          result[i] = operator.applyAsDouble(array1[i], array2[i]);
        } else {
          result[i] = array1[i];
        }
      } else {
        result[i] = array2[i];
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares each element in the array to zero within a tolerance.
   * <p>
   * An empty array returns true;
   * 
   * @param array  the array to check
   * @param tolerance  the tolerance to use
   * @return true if the array is effectively equal to zero
   */
  public static boolean fuzzyEqualsZero(double[] array, double tolerance) {
    for (int i = 0; i < array.length; i++) {
      if (!DoubleMath.fuzzyEquals(array[i], 0, tolerance)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares each element in the first array to the matching index in the second array within a tolerance.
   * <p>
   * If the arrays differ in length, false is returned.
   * 
   * @param array1  the first array to check
   * @param array2  the second array to check
   * @param tolerance  the tolerance to use
   * @return true if the arrays are effectively equal
   */
  public static boolean fuzzyEquals(double[] array1, double[] array2, double tolerance) {
    if (array1.length != array2.length) {
      return false;
    }
    for (int i = 0; i < array1.length; i++) {
      if (!DoubleMath.fuzzyEquals(array1[i], array2[i], tolerance)) {
        return false;
      }
    }
    return true;
  }

}
