/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Utilities for working with complex numbers.
 */
public class ComplexMathUtils {
// CSOFF: JavadocMethod

  public static ComplexNumber add(ComplexNumber z1, ComplexNumber z2) {
    ArgChecker.notNull(z1, "z1");
    ArgChecker.notNull(z2, "z2");
    return new ComplexNumber(z1.getReal() + z2.getReal(), z1.getImaginary() + z2.getImaginary());
  }

  public static ComplexNumber add(ComplexNumber... z) {
    ArgChecker.notNull(z, "z");
    double res = 0.0;
    double img = 0.0;
    for (ComplexNumber aZ : z) {
      res += aZ.getReal();
      img += aZ.getImaginary();
    }
    return new ComplexNumber(res, img);
  }

  public static ComplexNumber add(ComplexNumber z, double x) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() + x, z.getImaginary());
  }

  public static ComplexNumber add(double x, ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() + x, z.getImaginary());
  }

  public static double arg(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return Math.atan2(z.getImaginary(), z.getReal());
  }

  public static ComplexNumber conjugate(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal(), -z.getImaginary());
  }

  public static ComplexNumber divide(ComplexNumber z1, ComplexNumber z2) {
    ArgChecker.notNull(z1, "z1");
    ArgChecker.notNull(z2, "z2");
    double a = z1.getReal();
    double b = z1.getImaginary();
    double c = z2.getReal();
    double d = z2.getImaginary();
    if (Math.abs(c) > Math.abs(d)) {
      double dOverC = d / c;
      double denom = c + d * dOverC;
      return new ComplexNumber((a + b * dOverC) / denom, (b - a * dOverC) / denom);
    }
    double cOverD = c / d;
    double denom = c * cOverD + d;
    return new ComplexNumber((a * cOverD + b) / denom, (b * cOverD - a) / denom);
  }

  public static ComplexNumber divide(ComplexNumber z, double x) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() / x, z.getImaginary() / x);
  }

  public static ComplexNumber divide(double x, ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double c = z.getReal();
    double d = z.getImaginary();
    if (Math.abs(c) > Math.abs(d)) {
      double dOverC = d / c;
      double denom = c + d * dOverC;
      return new ComplexNumber(x / denom, -x * dOverC / denom);
    }
    double cOverD = c / d;
    double denom = c * cOverD + d;
    return new ComplexNumber(x * cOverD / denom, -x / denom);
  }

  public static ComplexNumber exp(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double mult = Math.exp(z.getReal());
    return new ComplexNumber(mult * Math.cos(z.getImaginary()), mult * Math.sin(z.getImaginary()));
  }

  public static ComplexNumber inverse(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double c = z.getReal();
    double d = z.getImaginary();
    if (Math.abs(c) > Math.abs(d)) {
      double dOverC = d / c;
      double denom = c + d * dOverC;
      return new ComplexNumber(1 / denom, -dOverC / denom);
    }
    double cOverD = c / d;
    double denom = c * cOverD + d;
    return new ComplexNumber(cOverD / denom, -1 / denom);
  }

  /**
   * Returns the principal value of log, with z the principal argument of z defined to lie in the interval (-pi, pi].
   * @param z ComplexNumber
   * @return The log
   */
  public static ComplexNumber log(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(Math.log(Math.hypot(z.getReal(), z.getImaginary())), Math.atan2(z.getImaginary(), z.getReal()));
  }

  public static double mod(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return Math.hypot(z.getReal(), z.getImaginary());
  }

  public static ComplexNumber square(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double a = z.getReal();
    double b = z.getImaginary();
    return new ComplexNumber(a * a - b * b, 2 * a * b);
  }

  public static ComplexNumber multiply(ComplexNumber z1, ComplexNumber z2) {
    ArgChecker.notNull(z1, "z1");
    ArgChecker.notNull(z2, "z2");
    double a = z1.getReal();
    double b = z1.getImaginary();
    double c = z2.getReal();
    double d = z2.getImaginary();
    return new ComplexNumber(a * c - b * d, a * d + b * c);
  }

  public static ComplexNumber multiply(ComplexNumber... z) {
    ArgChecker.notNull(z, "z");
    int n = z.length;
    ArgChecker.isTrue(n > 0, "nothing to multiply");
    if (n == 1) {
      return z[0];
    } else if (n == 2) {
      return multiply(z[0], z[1]);
    } else {
      ComplexNumber product = multiply(z[0], z[1]);
      for (int i = 2; i < n; i++) {
        product = multiply(product, z[i]);
      }
      return product;
    }
  }

  public static ComplexNumber multiply(double x, ComplexNumber... z) {
    ComplexNumber product = multiply(z);
    return multiply(x, product);
  }

  public static ComplexNumber multiply(ComplexNumber z, double x) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() * x, z.getImaginary() * x);
  }

  public static ComplexNumber multiply(double x, ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() * x, z.getImaginary() * x);
  }

  public static ComplexNumber pow(ComplexNumber z1, ComplexNumber z2) {
    ArgChecker.notNull(z1, "z1");
    ArgChecker.notNull(z2, "z2");
    double mod = mod(z1);
    double arg = arg(z1);
    double mult = Math.pow(mod, z2.getReal()) * Math.exp(-z2.getImaginary() * arg);
    double theta = z2.getReal() * arg + z2.getImaginary() * Math.log(mod);
    return new ComplexNumber(mult * Math.cos(theta), mult * Math.sin(theta));
  }

  public static ComplexNumber pow(ComplexNumber z, double x) {
    double mod = mod(z);
    double arg = arg(z);
    double mult = Math.pow(mod, x);
    return new ComplexNumber(mult * Math.cos(x * arg), mult * Math.sin(x * arg));
  }

  public static ComplexNumber pow(double x, ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return pow(new ComplexNumber(x, 0), z);
  }

  public static ComplexNumber sqrt(ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    double c = z.getReal();
    double d = z.getImaginary();
    if (c == 0.0 && d == 0.0) {
      return z;
    }
    double w;
    if (Math.abs(c) > Math.abs(d)) {
      double dOverC = d / c;
      w = Math.sqrt(Math.abs(c)) * Math.sqrt((1 + Math.sqrt(1 + dOverC * dOverC)) / 2);
    } else {
      double cOverD = c / d;
      w = Math.sqrt(Math.abs(d)) * Math.sqrt((Math.abs(cOverD) + Math.sqrt(1 + cOverD * cOverD)) / 2);
    }
    if (c >= 0.0) {
      return new ComplexNumber(w, d / 2 / w);
    }
    if (d >= 0.0) {
      return new ComplexNumber(d / 2 / w, w);
    }
    return new ComplexNumber(-d / 2 / w, -w);
  }

  public static ComplexNumber subtract(ComplexNumber z1, ComplexNumber z2) {
    ArgChecker.notNull(z1, "z1");
    ArgChecker.notNull(z2, "z2");
    return new ComplexNumber(z1.getReal() - z2.getReal(), z1.getImaginary() - z2.getImaginary());
  }

  public static ComplexNumber subtract(ComplexNumber z, double x) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(z.getReal() - x, z.getImaginary());
  }

  public static ComplexNumber subtract(double x, ComplexNumber z) {
    ArgChecker.notNull(z, "z");
    return new ComplexNumber(x - z.getReal(), -z.getImaginary());
  }

}
