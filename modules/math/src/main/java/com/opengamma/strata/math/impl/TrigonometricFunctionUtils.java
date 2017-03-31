/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static com.opengamma.strata.math.impl.ComplexNumber.I;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Trigonometric utilities.
 */
public class TrigonometricFunctionUtils {
// CSOFF: JavadocMethod

  private static final ComplexNumber NEGATIVE_I = new ComplexNumber(0, -1);

  public static double acos(double x) {
    return Math.acos(x);
  }

  /**
   * arccos - the inverse of cos.
   * @param z A complex number
   * @return acos(z)
   */
  public static ComplexNumber acos(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return ComplexMathUtils.multiply(
        NEGATIVE_I,
        ComplexMathUtils.log(
            ComplexMathUtils.add(
                z,
                ComplexMathUtils.sqrt(ComplexMathUtils.subtract(ComplexMathUtils.multiply(z, z), 1)))));
  }

  public static double acosh(double x) {
    double y = x * x - 1;
    ArgChecker.isTrue(y >= 0, "|x|>=1.0 for real solution");
    return Math.log(x + Math.sqrt(x * x - 1));
  }

  public static ComplexNumber acosh(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return ComplexMathUtils.log(
        ComplexMathUtils.add(
            z,
            ComplexMathUtils.sqrt(ComplexMathUtils.subtract(ComplexMathUtils.multiply(z, z), 1))));
  }

  public static double asin(double x) {

    return Math.asin(x);
  }

  public static ComplexNumber asin(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return ComplexMathUtils.multiply(NEGATIVE_I,
        ComplexMathUtils.log(
            ComplexMathUtils.add(
                ComplexMathUtils.multiply(I, z),
                ComplexMathUtils.sqrt(ComplexMathUtils.subtract(1, ComplexMathUtils.multiply(z, z))))));
  }

  public static double asinh(double x) {
    return Math.log(x + Math.sqrt(x * x + 1));
  }

  public static ComplexNumber asinh(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return ComplexMathUtils.log(
        ComplexMathUtils.add(
            z,
            ComplexMathUtils.sqrt(ComplexMathUtils.add(ComplexMathUtils.multiply(z, z), 1))));
  }

  public static double atan(double x) {
    return Math.atan(x);
  }

  public static ComplexNumber atan(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    ComplexNumber iZ = ComplexMathUtils.multiply(z, I);
    ComplexNumber half = new ComplexNumber(0, 0.5);
    return ComplexMathUtils.multiply(
        half,
        ComplexMathUtils.log(ComplexMathUtils.divide(ComplexMathUtils.subtract(1, iZ), ComplexMathUtils.add(1, iZ))));
  }

  public static double atanh(double x) {
    return 0.5 * Math.log((1 + x) / (1 - x));
  }

  public static ComplexNumber atanh(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return ComplexMathUtils.multiply(
        0.5,
        ComplexMathUtils.log(ComplexMathUtils.divide(ComplexMathUtils.add(1, z), ComplexMathUtils.subtract(1, z))));
  }

  public static double cos(double x) {
    return Math.cos(x);
  }

  public static ComplexNumber cos(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double x = z.getReal();
    double y = z.getImaginary();
    return new ComplexNumber(Math.cos(x) * Math.cosh(y), -Math.sin(x) * Math.sinh(y));
  }

  public static double cosh(double x) {
    return Math.cosh(x);
  }

  public static ComplexNumber cosh(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(
        Math.cosh(z.getReal()) * Math.cos(z.getImaginary()), Math.sinh(z.getReal()) * Math.sin(z.getImaginary()));
  }

  public static double sin(double x) {
    return Math.sin(x);
  }

  public static ComplexNumber sin(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double x = z.getReal();
    double y = z.getImaginary();
    return new ComplexNumber(Math.sin(x) * Math.cosh(y), Math.cos(x) * Math.sinh(y));
  }

  public static double sinh(double x) {
    return Math.sinh(x);
  }

  public static ComplexNumber sinh(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(
        Math.sinh(z.getReal()) * Math.cos(z.getImaginary()), Math.cosh(z.getReal()) * Math.sin(z.getImaginary()));
  }

  public static double tan(double x) {
    return Math.tan(x);
  }

  public static ComplexNumber tan(ComplexNumber z) {
    ComplexNumber b = ComplexMathUtils.exp(ComplexMathUtils.multiply(ComplexMathUtils.multiply(I, 2), z));
    return ComplexMathUtils.divide(
        ComplexMathUtils.subtract(b, 1),
        ComplexMathUtils.multiply(I, ComplexMathUtils.add(b, 1)));
  }

  public static double tanh(double x) {
    return Math.tanh(x);
  }

  public static ComplexNumber tanh(ComplexNumber z) {
    ComplexNumber z2 = ComplexMathUtils.exp(z);
    ComplexNumber z3 = ComplexMathUtils.exp(ComplexMathUtils.multiply(z, -1));
    return ComplexMathUtils.divide(ComplexMathUtils.subtract(z2, z3), ComplexMathUtils.add(z2, z3));
  }

}
