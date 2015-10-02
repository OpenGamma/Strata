/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Parent class for a family of functions that take real arguments and return real values. The functionality of {@link Function1D} is 
 * extended; this class allows arithmetic operations on functions and defines a derivative function.
 */
public abstract class DoubleFunction1D extends Function1D<Double, Double> {
  private static final double EPS = 1e-5;

  /**
   * Returns a function that calculates the first derivative. The method used
   * is central finite difference, with $\epsilon = 10^{-5}$.  Implementing
   * classes can override this method to return a function that is the exact
   * functional representation of the first derivative.
   * @return A function that calculates the first derivative of this function
   */
  public DoubleFunction1D derivative() {
    return derivative(FiniteDifferenceType.CENTRAL, EPS);
  }

  /**
   * Returns a function that calculates the first derivative. The method used
   * is finite difference, with the differencing type and $\epsilon$ as
   * arguments
   * @param differenceType The differencing type to use 
   * @param eps The $\epsilon$ to use
   * @return A function that calculates the first derivative of this function
   */
  public DoubleFunction1D derivative(final FiniteDifferenceType differenceType, final double eps) {
    ArgChecker.notNull(differenceType, "difference type");
    switch (differenceType) {
      case CENTRAL:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(final Double x) {
            return (DoubleFunction1D.this.evaluate(x + eps) - DoubleFunction1D.this.evaluate(x - eps)) / 2 / eps;
          }

        };
      case BACKWARD:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(final Double x) {
            return (DoubleFunction1D.this.evaluate(x) - DoubleFunction1D.this.evaluate(x - eps)) / eps;
          }

        };
      case FORWARD:
        return new DoubleFunction1D() {

          @Override
          public Double evaluate(final Double x) {
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
   * @param f The function to add, not null
   * @return A function $h(x) = f(x) + g(x)$
   */
  public DoubleFunction1D add(final DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) + f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, adding a constant $a$ returns the function
   * $h(x) = g(x) + a$.
   * @param a The constant to add
   * @return A function $h(x) = g(x) + a$
   */
  public DoubleFunction1D add(final double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) + a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, dividing by a function $f(x)$ returns the
   * function $h(x) = \frac{g(x)}{f(x)}$.
   * @param f The function to divide by, not null
   * @return A function $h(x) = \frac{f(x)}{g(x)}$
   */

  public DoubleFunction1D divide(final DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) / f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, dividing by a constant $a$ returns the
   * function $h(x) = \frac{g(x)}{a}$.
   * @param a The constant to add
   * @return A function $h(x) = \frac{g(x)}{a}$
   */
  public DoubleFunction1D divide(final double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) / a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, multiplying by a function $f(x)$ returns
   * the function $h(x) = f(x) g(x)$.
   * @param f The function to multiply by, not null
   * @return A function $h(x) = f(x) g(x)$
   */
  public DoubleFunction1D multiply(final DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) * f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, multiplying by a constant $a$ returns the
   * function $h(x) = a g(x)$.
   * @param a The constant to add
   * @return A function $h(x) = a g(x)$
   */
  public DoubleFunction1D multiply(final double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) * a;
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, subtracting a function $f(x)$ returns the
   * function $h(x) = f(x) - g(x)$.
   * @param f The function to subtract, not null
   * @return A function $h(x) = g(x) - f(x)$
   */
  public DoubleFunction1D subtract(final DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) - f.evaluate(x);
      }

    };
  }

  /**
   * For a DoubleFunction1D $g(x)$, subtracting a constant $a$ returns the
   * function $h(x) = g(x) - a$.
   * @param a The constant to add
   * @return A function $h(x) = g(x) - a$
   */
  public DoubleFunction1D subtract(final double a) {
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return DoubleFunction1D.this.evaluate(x) - a;
      }

    };
  }

  /**
   * Converts a Function1D<Double, Double> into a DoubleFunction1D.
   * @param f The function to convert
   * @return The converted function
   */
  public static DoubleFunction1D from(final Function1D<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public Double evaluate(final Double x) {
        return f.evaluate(x);
      }

    };
  }

}
