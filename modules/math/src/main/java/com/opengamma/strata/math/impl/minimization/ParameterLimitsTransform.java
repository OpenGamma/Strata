/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

/**
 * Interface for objects containing functions that can transform constrained model parameters into unconstrained fitting parameters and vice versa. It also
 * provides functions that will provide the gradient of the functions that perform these transformations. Let y be the model parameter and
 * yStar the transformed (fitting) parameter, then we write y* = f(y)
 */
public interface ParameterLimitsTransform {

  /** Types of the limits. */
  public enum LimitType {
    /** Greater than limit. */
    GREATER_THAN,
    /** Less than limit. */
    LESS_THAN
  }

  /**
   * A function to transform a constrained model parameter (y) to an unconstrained fitting parameter (y*) - i.e. y* = f(y)
   * @param x Model parameter 
   * @return Fitting parameter
   */
  double transform(double x);

  //  /**
  //   * A function to transform a set of constrained model parameters to a set of unconstrained fitting parameters
  //   * @param x Model parameter 
  //   * @return Fitting parameter
  //   */
  //  double[] transform(double[] x);

  /**
   * A function to transform an unconstrained fitting parameter (y*) to a constrained model parameter (y) - i.e. y = f^-1(y*)
   * @param y Fitting parameter
   * @return Model parameter 
   */
  double inverseTransform(double y);

  /**
   * The gradient of the function used to transform from a model parameter that is only allows
   * to take certain values, to a fitting parameter that can take any value.
   * @param x Model parameter
   * @return the gradient
   */
  double transformGradient(double x);

  /**
   * The gradient of the function used to transform from a fitting parameter that can take any value,
   * to a model parameter that is only allows to take certain values.
   * @param y fitting parameter
   * @return the gradient
   */
  double inverseTransformGradient(double y);

}
