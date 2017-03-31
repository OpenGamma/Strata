/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.Arrays;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Class representing a polynomial that has real coefficients and takes a real
 * argument. The function is defined as:
 * $$
 * \begin{align*}
 * p(x) = a_0 + a_1 x + a_2 x^2 + \ldots + a_{n-1} x^{n-1}
 * \end{align*}
 * $$
 */
public class RealPolynomialFunction1D implements DoubleFunction1D {

  private final double[] _coefficients;
  private final int _n;

  /**
   * Creates an instance.
   * 
   * The array of coefficients for a polynomial
   * $p(x) = a_0 + a_1 x + a_2 x^2 + ... + a_{n-1} x^{n-1}$
   * is $\\{a_0, a_1, a_2, ..., a_{n-1}\\}$.
   * 
   * @param coefficients  the array of coefficients, not null or empty
   */
  public RealPolynomialFunction1D(double... coefficients) {
    ArgChecker.notNull(coefficients, "coefficients");
    ArgChecker.isTrue(coefficients.length > 0, "coefficients length must be greater than zero");
    _coefficients = coefficients;
    _n = _coefficients.length;
  }

  //-------------------------------------------------------------------------
  @Override
  public double applyAsDouble(double x) {
    ArgChecker.notNull(x, "x");
    double y = _coefficients[_n - 1];
    for (int i = _n - 2; i >= 0; i--) {
      y = x * y + _coefficients[i];
    }
    return y;
  }

  /**
   * Gets the coefficients of this polynomial.
   * 
   * @return the coefficients of this polynomial
   */
  public double[] getCoefficients() {
    return _coefficients;
  }

  /**
   * Adds a function to the polynomial.
   * If the function is not a {@link RealPolynomialFunction1D} then the addition takes
   * place as in {@link DoubleFunction1D}, otherwise the result will also be a polynomial.
   * 
   * @param f  the function to add
   * @return $P+f$
   */
  @Override
  public DoubleFunction1D add(DoubleFunction1D f) {
    ArgChecker.notNull(f, "function");
    if (f instanceof RealPolynomialFunction1D) {
      RealPolynomialFunction1D p1 = (RealPolynomialFunction1D) f;
      double[] c1 = p1.getCoefficients();
      double[] c = _coefficients;
      int n = c1.length;
      boolean longestIsNew = n > _n;
      double[] c3 = longestIsNew ? Arrays.copyOf(c1, n) : Arrays.copyOf(c, _n);
      for (int i = 0; i < (longestIsNew ? _n : n); i++) {
        c3[i] += longestIsNew ? c[i] : c1[i];
      }
      return new RealPolynomialFunction1D(c3);
    }
    return DoubleFunction1D.super.add(f);
  }

  /**
   * Adds a constant to the polynomial (equivalent to adding the value to the constant
   * term of the polynomial). The result is also a polynomial.
   * 
   * @param a  the value to add
   * @return $P+a$ 
   */
  @Override
  public RealPolynomialFunction1D add(double a) {
    double[] c = Arrays.copyOf(getCoefficients(), _n);
    c[0] += a;
    return new RealPolynomialFunction1D(c);
  }

  /**
   * Returns the derivative of this polynomial (also a polynomial), where
   * $$
   * \begin{align*}
   * P'(x) = a_1 + 2 a_2 x + 3 a_3 x^2 + 4 a_4 x^3 + \dots + n a_n x^{n-1}
   * \end{align*}
   * $$.
   * 
   * @return the derivative polynomial
   */
  @Override
  public RealPolynomialFunction1D derivative() {
    int n = _coefficients.length - 1;
    double[] coefficients = new double[n];
    for (int i = 1; i <= n; i++) {
      coefficients[i - 1] = i * _coefficients[i];
    }
    return new RealPolynomialFunction1D(coefficients);
  }

  /**
   * Divides the polynomial by a constant value (equivalent to dividing each coefficient by this value).
   * The result is also a polynomial.
   * 
   * @param a  the divisor
   * @return the polynomial 
   */
  @Override
  public RealPolynomialFunction1D divide(double a) {
    double[] c = Arrays.copyOf(getCoefficients(), _n);
    for (int i = 0; i < _n; i++) {
      c[i] /= a;
    }
    return new RealPolynomialFunction1D(c);
  }

  /**
   * Multiplies the polynomial by a function.
   * If the function is not a {@link RealPolynomialFunction1D} then the multiplication takes
   * place as in {@link DoubleFunction1D}, otherwise the result will also be a polynomial.
   * 
   * @param f  the function by which to multiply
   * @return $P \dot f$
   */
  @Override
  public DoubleFunction1D multiply(DoubleFunction1D f) {
    ArgChecker.notNull(f, "function");
    if (f instanceof RealPolynomialFunction1D) {
      RealPolynomialFunction1D p1 = (RealPolynomialFunction1D) f;
      double[] c = _coefficients;
      double[] c1 = p1.getCoefficients();
      int m = c1.length;
      double[] newC = new double[_n + m - 1];
      for (int i = 0; i < newC.length; i++) {
        newC[i] = 0;
        for (int j = Math.max(0, i + 1 - m); j < Math.min(_n, i + 1); j++) {
          newC[i] += c[j] * c1[i - j];
        }
      }
      return new RealPolynomialFunction1D(newC);
    }
    return DoubleFunction1D.super.multiply(f);
  }

  /**
   * Multiplies the polynomial by a constant value (equivalent to multiplying each
   * coefficient by this value). The result is also a polynomial.
   * 
   * @param a  the multiplicator
   * @return the polynomial 
   */
  @Override
  public RealPolynomialFunction1D multiply(double a) {
    double[] c = Arrays.copyOf(getCoefficients(), _n);
    for (int i = 0; i < _n; i++) {
      c[i] *= a;
    }
    return new RealPolynomialFunction1D(c);
  }

  /**
   * Subtracts a function from the polynomial.
   * <p>
   * If the function is not a {@link RealPolynomialFunction1D} then the subtract takes place
   * as in {@link DoubleFunction1D}, otherwise the result will also be a polynomial.
   * 
   * @param f  the function to subtract
   * @return $P-f$
   */
  @Override
  public DoubleFunction1D subtract(DoubleFunction1D f) {
    ArgChecker.notNull(f, "function");
    if (f instanceof RealPolynomialFunction1D) {
      RealPolynomialFunction1D p1 = (RealPolynomialFunction1D) f;
      double[] c = _coefficients;
      double[] c1 = p1.getCoefficients();
      int m = c.length;
      int n = c1.length;
      int min = Math.min(m, n);
      int max = Math.max(m, n);
      double[] c3 = new double[max];
      for (int i = 0; i < min; i++) {
        c3[i] = c[i] - c1[i];
      }
      for (int i = min; i < max; i++) {
        if (m == max) {
          c3[i] = c[i];
        } else {
          c3[i] = -c1[i];
        }
      }
      return new RealPolynomialFunction1D(c3);
    }
    return DoubleFunction1D.super.subtract(f);
  }

  /**
   * Subtracts a constant from the polynomial (equivalent to subtracting the value from the
   * constant term of the polynomial). The result is also a polynomial.
   * 
   * @param a  the value to add
   * @return $P-a$ 
   */
  @Override
  public RealPolynomialFunction1D subtract(double a) {
    double[] c = Arrays.copyOf(getCoefficients(), _n);
    c[0] -= a;
    return new RealPolynomialFunction1D(c);
  }

  /**
   * Converts the polynomial to its monic form. If 
   * $$
   * \begin{align*}
   * P(x) = a_0 + a_1 x + a_2 x^2 + a_3 x^3 \dots + a_n x^n
   * \end{align*}
   * $$
   * then the monic form is
   * $$
   * \begin{align*}
   * P(x) = \lambda_0 + \lambda_1 x + \lambda_2 x^2 + \lambda_3 x^3 \dots + x^n
   * \end{align*}
   * $$
   * where 
   * $$
   * \begin{align*}
   * \lambda_i = \frac{a_i}{a_n}
   * \end{align*}
   * $$
   * 
   * @return the polynomial in monic form
   */
  public RealPolynomialFunction1D toMonic() {
    double an = _coefficients[_n - 1];
    if (DoubleMath.fuzzyEquals(an, (double) 1, 1e-15)) {
      return new RealPolynomialFunction1D(Arrays.copyOf(_coefficients, _n));
    }
    double[] rescaled = new double[_n];
    for (int i = 0; i < _n; i++) {
      rescaled[i] = _coefficients[i] / an;
    }
    return new RealPolynomialFunction1D(rescaled);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RealPolynomialFunction1D other = (RealPolynomialFunction1D) obj;
    if (!Arrays.equals(_coefficients, other._coefficients)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_coefficients);
    return result;
  }

}
