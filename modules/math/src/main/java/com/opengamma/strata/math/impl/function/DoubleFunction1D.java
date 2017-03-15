/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Defines a family of functions that take real arguments and return real values.
 * The functionality of {@link Function} is extended; this class allows arithmetic
 * operations on functions and defines a derivative function.
 */
public interface DoubleFunction1D extends DoubleUnaryOperator {

  /**
   * Returns a function that calculates the first derivative.
   * <p>
   * The method used is central finite difference, with $\epsilon = 10^{-5}$.
   * Implementing classes can override this method to return a function that
   * is the exact functional representation of the first derivative.
   * 
   * @return a function that calculates the first derivative of this function
   */
  public default DoubleFunction1D derivative() {
    return derivative(FiniteDifferenceType.CENTRAL, 1e-5);
  }

  /**
   * Returns a function that calculates the first derivative. The method used
   * is finite difference, with the differencing type and $\epsilon$ as arguments.
   * 
   * @param differenceType  the differencing type to use 
   * @param eps  the $\epsilon$ to use
   * @return a function that calculates the first derivative of this function
   */
  public default DoubleFunction1D derivative(FiniteDifferenceType differenceType, double eps) {
    ArgChecker.notNull(differenceType, "difference type");
    switch (differenceType) {
      case CENTRAL:
        return new DoubleFunction1D() {

          @Override
          public double applyAsDouble(double x) {
            return (DoubleFunction1D.this.applyAsDouble(x + eps) - DoubleFunction1D.this.applyAsDouble(x - eps)) / 2 / eps;
          }

        };
      case BACKWARD:
        return new DoubleFunction1D() {

          @Override
          public double applyAsDouble(double x) {
            return (DoubleFunction1D.this.applyAsDouble(x) - DoubleFunction1D.this.applyAsDouble(x - eps)) / eps;
          }

        };
      case FORWARD:
        return new DoubleFunction1D() {

          @Override
          public double applyAsDouble(double x) {
            return (DoubleFunction1D.this.applyAsDouble(x + eps) - DoubleFunction1D.this.applyAsDouble(x)) / eps;
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
  public default DoubleFunction1D add(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) + f.applyAsDouble(x);
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
  public default DoubleFunction1D add(double a) {
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) + a;
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

  public default DoubleFunction1D divide(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) / f.applyAsDouble(x);
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
  public default DoubleFunction1D divide(double a) {
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) / a;
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
  public default DoubleFunction1D multiply(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) * f.applyAsDouble(x);
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
  public default DoubleFunction1D multiply(double a) {
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) * a;
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
  public default DoubleFunction1D subtract(DoubleFunction1D f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) - f.applyAsDouble(x);
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
  public default DoubleFunction1D subtract(double a) {
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return DoubleFunction1D.this.applyAsDouble(x) - a;
      }

    };
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a Function<Double, Double> into a DoubleFunction1D.
   * 
   * @param f  the function to convert
   * @return the converted function
   */
  public static DoubleFunction1D from(Function<Double, Double> f) {
    ArgChecker.notNull(f, "f");
    return new DoubleFunction1D() {

      @Override
      public double applyAsDouble(double x) {
        return f.apply(x);
      }

    };
  }

}
