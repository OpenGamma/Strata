/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * A parameterised surface that gives the both the surface (the function z=f(xy) where xy is a 2D point and z is a scalar)
 *  and the surface sensitivity (dz/dp where p is one of the parameters) for given parameters.
 */
public abstract class ParameterizedSurface extends ParameterizedFunction<DoublesPair, DoubleMatrix1D, Double> {
  private static final ScalarFieldFirstOrderDifferentiator FIRST_ORDER_DIFF = new ScalarFieldFirstOrderDifferentiator();

  /**
   * For a function of two variables (surface) that can be written as $z=f(x, y;\boldsymbol{\theta})$ where x, y & z are scalars and
   * $\boldsymbol{\theta})$ is a vector of parameters (i.e. $x,y,z \in \mathbb{R}$ and $\boldsymbol{\theta} \in \mathbb{R}^n$)
   * this returns the function $g : \mathbb{R} \to \mathbb{R}^n; x,y \mapsto g(x,y)$, which is the function's (curves') sensitivity 
   * to its parameters, i.e. $g(x,y) = \frac{\partial f(x,y;\boldsymbol{\theta})}{\partial \boldsymbol{\theta}}$<p>
   * The default calculation is performed using finite difference (via {@link ScalarFieldFirstOrderDifferentiator}) but
   * it is expected that this will be overridden by concrete subclasses.  
   * @param params The value of the parameters ($\boldsymbol{\theta}$) at which the sensitivity is calculated 
   * @return The sensitivity as a function with a DoublesPair (x,y) as its single argument and a vector as its return value
   */
  public Function1D<DoublesPair, DoubleMatrix1D> getZParameterSensitivity(final DoubleMatrix1D params) {

    return new Function1D<DoublesPair, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(final DoublesPair xy) {
        final Function1D<DoubleMatrix1D, Double> f = asFunctionOfParameters(xy);

        final Function1D<DoubleMatrix1D, DoubleMatrix1D> g = FIRST_ORDER_DIFF.differentiate(f);
        return g.evaluate(params);
      }
    };
  }

}
