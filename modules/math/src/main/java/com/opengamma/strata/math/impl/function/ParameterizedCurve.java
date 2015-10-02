/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * A parameterised curve that gives the both the curve (the function y=f(x) where x and y are scalars) and the
 * curve sensitivity (dy/dp where p is one of the parameters) for given parameters.
 */
public abstract class ParameterizedCurve extends ParameterizedFunction<Double, DoubleMatrix1D, Double> {

  private static final ScalarFieldFirstOrderDifferentiator FIRST_ORDER_DIFF = new ScalarFieldFirstOrderDifferentiator();

  /**
   * For a scalar function (curve) that can be written as $y=f(x;\boldsymbol{\theta})$ where x & y are scalars and
   * $\boldsymbol{\theta})$ is a vector of parameters (i.e. $x,y \in \mathbb{R}$ and $\boldsymbol{\theta} \in \mathbb{R}^n$)
   * this returns the function $g : \mathbb{R} \to \mathbb{R}^n; x \mapsto g(x)$, which is the function's (curve's) sensitivity 
   * to its parameters, i.e. $g(x) = \frac{\partial f(x;\boldsymbol{\theta})}{\partial \boldsymbol{\theta}}$<p>
   * The default calculation is performed using finite difference (via {@link ScalarFieldFirstOrderDifferentiator}) but
   * it is expected that this will be overridden by concrete subclasses.  
   * @param params The value of the parameters ($\boldsymbol{\theta}$) at which the sensitivity is calculated 
   * @return The sensitivity as a function with a Double (x) as its single argument and a vector as its return value.
   */
  public Function1D<Double, DoubleMatrix1D> getYParameterSensitivity(final DoubleMatrix1D params) {

    return new Function1D<Double, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(final Double x) {
        final Function1D<DoubleMatrix1D, Double> f = asFunctionOfParameters(x);

        final Function1D<DoubleMatrix1D, DoubleMatrix1D> g = FIRST_ORDER_DIFF.differentiate(f);
        return g.evaluate(params);
      }
    };
  }

}
