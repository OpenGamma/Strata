/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Computes the numerical difference between adjacent elements in vector. 
 */
public class Diff {

  /**
   * Finds the numerical difference between value at position (i+1) and (i)
   * returning a vector of what would be needed to be added to the first (n-1) elements
   * of the original vector to get the original vector. 
   * 
   * @param v  the vector
   * @return the numerical difference between adjacent elements in v
   */
  public static double[] values(double[] v) {
    ArgChecker.notNull(v, "v");
    int n = v.length - 1;
    double[] tmp = new double[n];
    for (int i = 0; i < n; i++) {
      tmp[i] = v[i + 1] - v[i];
    }
    return tmp;
  }

  /**
   * Finds the t^{th} numerical difference between value at position (i+1) and (i)
   * (effectively recurses #values "t" times).
   * 
   * @param v  the vector
   * @param t  the number of differences to be taken (t positive)
   * @return the numerical difference between adjacent elements in v
   */
  public static double[] values(double[] v, int t) {
    ArgChecker.notNull(v, "v");
    ArgChecker.isTrue((t > -1), "Invalid number of differences requested, t must be positive or 0, but was {}", t);
    ArgChecker.isTrue((t < v.length), "Invalid number of differences requested, 't' is greater than the number of " +
        "elements in 'v'. The given 't' was: {} and 'v' contains {} elements", t, v.length);
    double[] tmp;
    if (t == 0) { // no differencing done
      tmp = new double[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
    } else {
      tmp = values(v);
      for (int i = 0; i < t - 1; i++) {
        tmp = values(tmp);
      }
    }
    return tmp;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the numerical difference between value at position (i+1) and (i)
   * returning a vector of what would be needed to be added to the first (n-1) elements
   * of the original vector to get the original vector. 
   * 
   * @param v  the vector
   * @return the numerical difference between adjacent elements in v
   */
  public static float[] values(float[] v) {
    ArgChecker.notNull(v, "v");
    int n = v.length - 1;
    float[] tmp = new float[n];
    for (int i = 0; i < n; i++) {
      tmp[i] = v[i + 1] - v[i];
    }
    return tmp;
  }

  /**
   * Finds the t^{th} numerical difference between value at position (i+1) and (i)
   * (effectively recurses #values "t" times).
   * 
   * @param v  the vector
   * @param t  the number of differences to be taken (t positive)
   * @return the numerical difference between adjacent elements in v
   */
  public static float[] values(float[] v, int t) {
    ArgChecker.notNull(v, "v");
    ArgChecker.isTrue((t > -1), "Invalid number of differences requested, t must be positive or 0, but was {}", t);
    ArgChecker.isTrue((t < v.length), "Invalid number of differences requested, 't' is greater than the number of " +
        "elements in 'v'. The given 't' was: {} and 'v' contains {} elements", t, v.length);
    float[] tmp;
    if (t == 0) { // no differencing done
      tmp = new float[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
    } else {
      tmp = values(v);
      for (int i = 0; i < t - 1; i++) {
        tmp = values(tmp);
      }
    }
    return tmp;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the numerical difference between value at position (i+1) and (i)
   * returning a vector of what would be needed to be added to the first (n-1) elements
   * of the original vector to get the original vector. 
   * 
   * @param v  the vector
   * @return the numerical difference between adjacent elements in v
   */
  public static int[] values(int[] v) {
    ArgChecker.notNull(v, "v");
    int n = v.length - 1;
    int[] tmp = new int[n];
    for (int i = 0; i < n; i++) {
      tmp[i] = v[i + 1] - v[i];
    }
    return tmp;
  }

  /**
   * Finds the t^{th} numerical difference between value at position (i+1) and (i)
   * (effectively recurses #values "t" times).
   * 
   * @param v  the vector
   * @param t  the number of differences to be taken (t positive)
   * @return the numerical difference between adjacent elements in v
   */
  public static int[] values(int[] v, int t) {
    ArgChecker.notNull(v, "v");
    ArgChecker.isTrue((t > -1), "Invalid number of differences requested, t must be positive or 0, but was {}", t);
    ArgChecker.isTrue((t < v.length), "Invalid number of differences requested, 't' is greater than the number of " +
        "elements in 'v'. The given 't' was: {} and 'v' contains {} elements", t, v.length);
    int[] tmp;
    if (t == 0) { // no differencing done
      tmp = new int[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
    } else {
      tmp = values(v);
      for (int i = 0; i < t - 1; i++) {
        tmp = values(tmp);
      }
    }
    return tmp;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the numerical difference between value at position (i+1) and (i)
   * returning a vector of what would be needed to be added to the first (n-1) elements
   * of the original vector to get the original vector. 
   * 
   * @param v  the vector
   * @return the numerical difference between adjacent elements in v
   */
  public static long[] values(long[] v) {
    ArgChecker.notNull(v, "v");
    int n = v.length - 1;
    long[] tmp = new long[n];
    for (int i = 0; i < n; i++) {
      tmp[i] = v[i + 1] - v[i];
    }
    return tmp;
  }

  /**
   * Finds the t^{th} numerical difference between value at position (i+1) and (i)
   * (effectively recurses #values "t" times).
   * 
   * @param v  the vector
   * @param t  the number of differences to be taken (t positive)
   * @return the numerical difference between adjacent elements in v
   */
  public static long[] values(long[] v, int t) {
    ArgChecker.notNull(v, "v");
    ArgChecker.isTrue((t > -1), "Invalid number of differences requested, t must be positive or 0, but was {}", t);
    ArgChecker.isTrue((t < v.length), "Invalid number of differences requested, 't' is greater than the number of " +
        "elements in 'v'. The given 't' was: {} and 'v' contains {} elements", t, v.length);
    long[] tmp;
    if (t == 0) { // no differencing done
      tmp = new long[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
    } else {
      tmp = values(v);
      for (int i = 0; i < t - 1; i++) {
        tmp = values(tmp);
      }
    }
    return tmp;
  }

}
