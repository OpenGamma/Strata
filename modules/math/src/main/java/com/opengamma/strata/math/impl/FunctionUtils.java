/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A collection of basic useful maths functions.
 */
public final class FunctionUtils {

  /**
   * Restricted constructor.
   */
  private FunctionUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the square of a number.
   * 
   * @param x  the number to square
   * @return x*x
   */
  public static double square(double x) {
    return x * x;
  }

  /**
   * Returns the cube of a number.
   * 
   * @param x  the number to cube
   * @return x*x*x
   */
  public static double cube(double x) {
    return x * x * x;
  }

  //-------------------------------------------------------------------------
  public static int toTensorIndex(int[] indices, int[] dimensions) {
    ArgChecker.notNull(indices, "indices");
    ArgChecker.notNull(dimensions, "dimensions");
    int dim = indices.length;
    ArgChecker.isTrue(dim == dimensions.length);
    int sum = 0;
    int product = 1;
    for (int i = 0; i < dim; i++) {
      ArgChecker.isTrue(indices[i] < dimensions[i], "index out of bounds");
      sum += indices[i] * product;
      product *= dimensions[i];
    }
    return sum;
  }

  public static int[] fromTensorIndex(int index, int[] dimensions) {
    ArgChecker.notNull(dimensions, "dimensions");
    int dim = dimensions.length;
    int[] res = new int[dim];

    int product = 1;
    int[] products = new int[dim - 1];
    for (int i = 0; i < dim - 1; i++) {
      product *= dimensions[i];
      products[i] = product;
    }

    int a = index;
    for (int i = dim - 1; i > 0; i--) {
      res[i] = a / products[i - 1];
      a -= res[i] * products[i - 1];
    }
    res[0] = a;

    return res;
  }

  //-------------------------------------------------------------------------
  /**
   * Same behaviour as mathlab unique.
   * 
   * @param in  the input array
   * @return a sorted array with no duplicates values
   */
  public static double[] unique(double[] in) {
    Arrays.sort(in);
    int n = in.length;
    double[] temp = new double[n];
    temp[0] = in[0];
    int count = 1;
    for (int i = 1; i < n; i++) {
      if (Double.compare(in[i], in[i - 1]) != 0) {
        temp[count++] = in[i];
      }
    }
    if (count == n) {
      return temp;
    }
    return Arrays.copyOf(temp, count);
  }

  /**
   * Same behaviour as mathlab unique.
   * 
   * @param in  the input array
   * @return a sorted array with no duplicates values
   */
  public static int[] unique(int[] in) {
    Arrays.sort(in);
    int n = in.length;
    int[] temp = new int[n];
    temp[0] = in[0];
    int count = 1;
    for (int i = 1; i < n; i++) {
      if (in[i] != in[i - 1]) {
        temp[count++] = in[i];
      }
    }
    if (count == n) {
      return temp;
    }
    return Arrays.copyOf(in, count);
  }

  //-------------------------------------------------------------------------
  /**
   * Find the index of a <b>sorted</b> set that is less than or equal to a given value.
   * If the given value is lower than the lowest member (i.e. the first)
   * of the set, zero is returned.  This uses Arrays.binarySearch.
   * 
   * @param set  a <b>sorted</b> array of numbers. 
   * @param value  the value to search for
   * @return the index in the array
   */
  public static int getLowerBoundIndex(DoubleArray set, double value) {
    int n = set.size();
    if (value < set.get(0)) {
      return 0;
    }
    if (value > set.get(n - 1)) {
      return n - 1;
    }
    int index = Arrays.binarySearch(set.toArrayUnsafe(), value);
    if (index >= 0) {
      // Fast break out if it's an exact match.
      return index;
    }
    if (index < 0) {
      index = -(index + 1);
      index--;
    }
    if (value == -0. && index < n - 1 && set.get(index + 1) == 0.) {
      ++index;
    }
    return index;
  }

}
