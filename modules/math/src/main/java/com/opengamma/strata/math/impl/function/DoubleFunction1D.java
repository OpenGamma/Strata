/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Parent class for a family of functions that take real arguments and return real values.
 * The functionality of {@link Function1D} is  extended; this class allows arithmetic
 * operations on functions and defines a derivative function.
 */
public abstract class DoubleFunction1D extends Function1D<Double, Double> {

  private static final double EPS = 1e-5;

  /**
   * Returns a function that calculates the first derivative. The method used
   * is central finite difference, with $\epsilon = 10^{-5}$.  Implementing
   * classes can override this method to return a function that is the exact
   * functional representation of the first derivative.
   * 
   * @return a function that calculates the first derivative of this function
   */
  public DoubleFunction1D derivative() {
    return derivative(FiniteDifferenceType.CENTRAL, EPS);
  }

  /**
   * Returns a function that calculates the first derivative. The method used
   * is finite difference, with the differencing type and $\epsilon$ as arguments.
   * 
   * @param differenceType  the differencing type to use 
   * @param eps  the $\epsilon$ to use
   * @return a function that calculates the first derivative of this function
   */
  public DoubleFunction1D derivative(FiniteDifferenceType differenceType, double eps) {
    ArgChecker.notNull(differenceType, "difference type");
    switch (differenceType) {
      case CENTRAL:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(Double x) {
            return (DoubleFunction1D.this.evaluate(x + eps) - DoubleFunction1D.this.evaluate(x - eps)) / 2 / eps;
          }

        };
      case BACKWARD:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(Double x) {
            return (DoubleFunction1D.this.evaluate(x) - DoubleFunction1D.this.evaluate(x - eps)) / eps;
          }

        };
      case FORWARD:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(Double x) {
            return (DoubleFunction1D.this.evaluate(x + eps) - DoubleFunction1D.this.evaluate(x)) / eps;
          }

        };
      default:
        throw new IllegalArgumentException("Unhandled FiniteDifferenceType " + differenceType);
    }
  }

  /**
   * For a DoubleFunction1D $g(x)$, adding a function $f(x)$ returns the
   * function $h(x) = f(x) + g(x)$.
   * 
   * @param f  the function to add
   * @return a function $h(x) = f(x) + g(x)$
   */
  public DoubleFunction1D add(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) + f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, adding a constant $a$ returns the function
   * $h(x) = g(x) + a$.
   * 
   * @param a  the constant to add
   * @return a function $h(x) = g(x) + a$
   */
  public DoubleFunction1D add(double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) + a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, dividing by a function $f(x)$ returns the
   * function $h(x) = \frac{g(x)}{f(x)}$.
   * 
   * @param f  the function to divide by
   * @return a function $h(x) = \frac{f(x)}{g(x)}$
   */

  public DoubleFunction1D divide(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) / f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, dividing by a constant $a$ returns the
   * function $h(x) = \frac{g(x)}{a}$.
   * 
   * @param a  the constant to add
   * @return a function $h(x) = \frac{g(x)}{a}$
   */
  public DoubleFunction1D divide(double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) / a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, multiplying by a function $f(x)$ returns
   * the function $h(x) = f(x) g(x)$.
   * 
   * @param f  the function to multiply by
   * @return a function $h(x) = f(x) g(x)$
   */
  public DoubleFunction1D multiply(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) * f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, multiplying by a constant $a$ returns the
   * function $h(x) = a g(x)$.
   * 
   * @param a  the constant to add
   * @return a function $h(x) = a g(x)$
   */
  public DoubleFunction1D multiply(double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) * a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, subtracting a function $f(x)$ returns the
   * function $h(x) = f(x) - g(x)$.
   * 
   * @param f  the function to subtract
   * @return a function $h(x) = g(x) - f(x)$
   */
  public DoubleFunction1D subtract(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) - f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, subtracting a constant $a$ returns the
   * function $h(x) = g(x) - a$.
   * 
   * @param a  the constant to add
   * @return a function $h(x) = g(x) - a$
   */
  public DoubleFunction1D subtract(double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return DoubleFunction1D.this.evaluate(x) - a;
      }

    };
  }

  /**
   * Converts a Function1D<Double, Double> into a DoubleFunction1D.
   * 
   * @param f  the function to convert
   * @return the converted function
   */
  public static DoubleFunction1D from(Function1D<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(Double x) {
        return f.evaluate(x);
      }

    };
  }

}
