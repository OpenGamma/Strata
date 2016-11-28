/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Simpson's integration rule is a Newton-Cotes formula that approximates the
 * function to be integrated with quadratic polynomials before performing the
 * integration. For a function $f(x)$, if three points $x_1$, $x_2$ and $x_3$
 * are equally spaced on the abscissa with $x_2 - x_1 = h$ then
 * $$
 * \begin{align*}
 * \int^{x_3} _{x_1} f(x)dx \approx \frac{1}{3}h(f(x_1) + 4f(x_2) + f(x_3))
 * \end{align*}
 * $$
 * <p> 
 * This class is a wrapper for the
 * <a href="http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/analysis/integration/SimpsonIntegrator.html">Commons Math library implementation</a> 
 * of Simpson integration.
 */
public class SimpsonIntegrator1D extends Integrator1D<Double, Double> {

  private static final Logger log = LoggerFactory.getLogger(SimpsonIntegrator1D.class);
  private static final int MAX_EVAL = 1000;
  private final UnivariateIntegrator integrator = new SimpsonIntegrator();

  /**
   * Simpson's integration method.
   * <p>
   * Note that the Commons implementation fails if the lower bound is larger than the upper - 
   * in this case, the bounds are reversed and the result negated. 
   * 
   * @param f The function to integrate, not null
   * @param lower The lower bound, not null
   * @param upper The upper bound, not null
   * @return The result of the integration
   */
  @Override
  public Double integrate(Function<Double, Double> f, Double lower, Double upper) {
    ArgChecker.notNull(f, "function");
    ArgChecker.notNull(lower, "lower bound");
    ArgChecker.notNull(upper, "upper bound");
    try {
      if (lower < upper) {
        return integrator.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), lower, upper);
      }
      log.info("Upper bound was less than lower bound; swapping bounds and negating result");
      return -integrator.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), upper, lower);
    } catch (NumberIsTooSmallException | NumberIsTooLargeException e) {
      throw new MathException(e);
    }
  }

}
