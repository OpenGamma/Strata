/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

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
   * Converts a {@code double} array to a {@code Double} array.
   * 
   * @param array  the array to convert
   * @return the converted array
   */
  public static Double[] toObject(double[] array) {
    if (array.length == 0) {
      return EMPTY_DOUBLE_OBJECT_ARRAY;
    }
    Double[] result = new Double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = new Double(array[i]);
    }
    return result;
  }

  /**
   * Converts a {@code Double} array to a {@code double} array.
   * <p>
   * Throws an exception if null is found.
   * 
   * @param array  the array to convert
   * @return the converted array
   * @throws NullPointerException if null found
   */
  public static double[] toPrimitive(Double[] array) {
    if (array.length == 0) {
      return EMPTY_DOUBLE_ARRAY;
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i].doubleValue();
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the sum total of all the elements in the array.
   * <p>
   * The input array is not mutated.
   * 
   * @param array  the array to sum
   * @return the sum total of all the elements
   */
  public static double sum(double[] array) {
    double total = 0d;
    for (int i = 0; i < array.length; i++) {
      total += array[i];
    }
    return total;
  }

  //-------------------------------------------------------------------------
  /**
   * Applies an addition to each element in the array, returning a new array.
   * <p>
   * The result is always a new array. The input array is not mutated.
   * 
   * @param array  the input array, not mutated
   * @param valueToAdd  the value to add
   * @return the resulting array
   */
  public static double[] applyAddition(double[] array, double valueToAdd) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] + valueToAdd;
    }
    return result;
  }

  /**
   * Applies a multiplication to each element in the array, returning a new array.
   * <p>
   * The result is always a new array. The input array is not mutated.
   * 
   * @param array  the input array, not mutated
   * @param valueToMultiplyBy  the value to multiply by
   * @return the resulting array
   */
  public static double[] applyMultiplication(double[] array, double valueToMultiplyBy) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] * valueToMultiplyBy;
    }
    return result;
  }

  /**
   * Applies an operator to each element in the array, returning a new array.
   * <p>
   * The result is always a new array. The input array is not mutated.
   * 
   * @param array  the input array, not mutated
   * @param operator  the operator to use
   * @return the resulting array
   */
  public static double[] apply(double[] array, DoubleUnaryOperator operator) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = operator.applyAsDouble(array[i]);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a constant value to each element in the array by mutation.
   * <p>
   * The input array is mutated.
   * 
   * @param array  the array to mutate
   * @param valueToAdd  the value to add
   */
  public static void mutateByAddition(double[] array, double valueToAdd) {
    for (int i = 0; i < array.length; i++) {
      array[i] += valueToAdd;
    }
  }

  /**
   * Adds values in two arrays together, mutating the first array.
   * <p>
   * The arrays must be the same length. Each value in {@code arrayToAdd} is added to the value at the
   * corresponding index in {@code array}.
   *
   * @param array  the array to mutate
   * @param arrayToAdd  the array containing values to add
   */
  public static void mutateByAddition(double[] array, double[] arrayToAdd) {
    int length = length(array, arrayToAdd);
    for (int i = 0; i < length; i++) {
      array[i] += arrayToAdd[i];
    }
  }

  /**
   * Multiplies each element in the array by a value by mutation.
   * <p>
   * The input array is mutated.
   * 
   * @param array  the array to mutate
   * @param valueToMultiplyBy  the value to multiply by
   */
  public static void mutateByMultiplication(double[] array, double valueToMultiplyBy) {
    for (int i = 0; i < array.length; i++) {
      array[i] *= valueToMultiplyBy;
    }
  }

  /**
   * Multiplies values in two arrays, mutating the first array.
   * <p>
   * The arrays must be the same length. Each value in {@code array} is multiplied by the value at the
   * corresponding index in {@code arrayToMultiplyBy}.
   *
   * @param array  the array to mutate
   * @param arrayToMultiplyBy  the array containing values to multiply by
   */
  public static void mutateByMultiplication(double[] array, double[] arrayToMultiplyBy) {
    int length = length(array, arrayToMultiplyBy);
    for (int i = 0; i < length; i++) {
      array[i] *= arrayToMultiplyBy[i];
    }
  }

  /**
   * Mutates each element in the array using an operator by mutation.
   * <p>
   * The input array is mutated.
   * 
   * @param array  the array to mutate
   * @param operator  the operator to use to perform the mutation
   */
  public static void mutate(double[] array, DoubleUnaryOperator operator) {
    for (int i = 0; i < array.length; i++) {
      array[i] = operator.applyAsDouble(array[i]);
    }
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
   * <p>
   * The result is always a new array. The input arrays are not mutated.
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
   * <p>
   * The result is always a new array. The input arrays are not mutated.
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
   * <p>
   * The result is always a new array. The input arrays are not mutated.
   * 
   * @param array1  the first array
   * @param array2  the second array
   * @param operator  the operator to use when combining values
   * @return an array combining the two input arrays using the operator
   */
  public static double[] combine(double[] array1, double[] array2, DoubleBinaryOperator operator) {
    int length = length(array1, array2);
    double[] result = new double[length];
    for (int i = 0; i < length; i++) {
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
   * <p>
   * The result is always a new array. The input arrays are not mutated.
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
   * <p>
   * The input array is not mutated.
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
   * <p>
   * The input arrays are not mutated.
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

  //-------------------------------------------------------------------------
  /**
   * Sorts the two arrays, retaining the associated values with the sorted keys.
   * <p>
   * The two arrays must be the same size and represent a pair of key to value.
   * The sort order is determined by the array of keys.
   * The position of each value is changed to match that of the sorted keys.
   * <p>
   * The input arrays are mutated.
   * 
   * @param keys  the array of keys to sort
   * @param values  the array of associated values to retain
   */
  public static void sortPairs(double[] keys, double[] values) {
    int len1 = keys.length;
    if (len1 != values.length) {
      throw new IllegalArgumentException("Arrays cannot be sorted as they differ in length");
    }
    dualArrayQuickSort(keys, values, 0, len1 - 1);
  }

  private static void dualArrayQuickSort(double[] keys, double[] values, int left, int right) {
    if (right > left) {
      int pivot = (left + right) >> 1;
      int pivotNewIndex = partition(keys, values, left, right, pivot);
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1);
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right);
    }
  }

  private static int partition(double[] keys, double[] values, int left, int right, int pivot) {
    double pivotValue = keys[pivot];
    swap(keys, values, pivot, right);
    int storeIndex = left;
    for (int i = left; i < right; i++) {
      if (keys[i] <= pivotValue) {
        swap(keys, values, i, storeIndex);
        storeIndex++;
      }
    }
    swap(keys, values, storeIndex, right);
    return storeIndex;
  }

  private static void swap(double[] keys, double[] values, int first, int second) {
    double t = keys[first];
    keys[first] = keys[second];
    keys[second] = t;
    t = values[first];
    values[first] = values[second];
    values[second] = t;
  }

  //-------------------------------------------------------------------------
  /**
   * Sorts the two arrays, retaining the associated values with the sorted keys.
   * <p>
   * The two arrays must be the same size and represent a pair of key to value.
   * The sort order is determined by the array of keys.
   * The position of each value is changed to match that of the sorted keys.
   * <p>
   * The input arrays are mutated.
   * 
   * @param <V>  the type of the values
   * @param keys  the array of keys to sort
   * @param values  the array of associated values to retain
   */
  public static <V> void sortPairs(double[] keys, V[] values) {
    int len1 = keys.length;
    if (len1 != values.length) {
      throw new IllegalArgumentException("Arrays cannot be sorted as they differ in length");
    }
    dualArrayQuickSort(keys, values, 0, len1 - 1);
  }

  private static <T> void dualArrayQuickSort(double[] keys, T[] values, int left, int right) {
    if (right > left) {
      int pivot = (left + right) >> 1;
      int pivotNewIndex = partition(keys, values, left, right, pivot);
      dualArrayQuickSort(keys, values, left, pivotNewIndex - 1);
      dualArrayQuickSort(keys, values, pivotNewIndex + 1, right);
    }
  }

  private static <T> int partition(double[] keys, T[] values, int left, int right, int pivot) {
    double pivotValue = keys[pivot];
    swap(keys, values, pivot, right);
    int storeIndex = left;
    for (int i = left; i < right; i++) {
      if (keys[i] <= pivotValue) {
        swap(keys, values, i, storeIndex);
        storeIndex++;
      }
    }
    swap(keys, values, storeIndex, right);
    return storeIndex;
  }

  private static <T> void swap(double[] keys, T[] values, int first, int second) {
    double x = keys[first];
    keys[first] = keys[second];
    keys[second] = x;
    T t = values[first];
    values[first] = values[second];
    values[second] = t;
  }

  /**
   * Return the array lengths if they are the same, otherwise throws an {@code IllegalArgumentException}.
   */
  private static int length(double[] array1, double[] array2) {
    int len1 = array1.length;
    int len2 = array2.length;
    if (len1 != len2) {
      throw new IllegalArgumentException("Arrays cannot be combined as they differ in length");
    }
    return len1;
  }

}
