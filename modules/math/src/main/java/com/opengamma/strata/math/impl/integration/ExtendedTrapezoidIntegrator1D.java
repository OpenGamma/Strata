/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * The trapezoid integration rule is a two-point Newton-Cotes formula that
 * approximates the area under the curve as a trapezoid. For a function $f(x)$,
 * $$
 * \begin{align*}
 * \int^{x_2} _{x_1} f(x)dx \approx \frac{1}{2}(x_2 - x_1)(f(x_1) + f(x_2))
 * \end{align*}
 * $$
 * <p> 
 * This class is a wrapper for the
 * <a href="http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/analysis/integration/TrapezoidIntegrator.html">Commons Math library implementation</a> 
 * of trapezoidal integration.
 */
public class ExtendedTrapezoidIntegrator1D extends Integrator1D<Double, Double> {

  private static final Logger log = LoggerFactory.getLogger(ExtendedTrapezoidIntegrator1D.class);
  private static final UnivariateIntegrator INTEGRATOR = new TrapezoidIntegrator();
  private static final int MAX_EVAL = 10000;

  /**
   * Trapezoid integration method. Note that the Commons implementation fails if the lower bound is larger than the upper - 
   * in this case, the bounds are reversed and the result negated. 
   * {@inheritDoc}
   */
  @Override
  public Double integrate(Function<Double, Double> f, Double lower, Double upper) {
    ArgChecker.notNull(f, "f");
    ArgChecker.notNull(lower, "lower");
    ArgChecker.notNull(upper, "upper");
    try {
      if (lower < upper) {
        return INTEGRATOR.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), lower, upper);
      }
      log.info("Upper bound was less than lower bound; swapping bounds and negating result");
      return -INTEGRATOR.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), upper, lower);
    } catch (MaxCountExceededException |
        MathIllegalArgumentException e) {
      throw new MathException(e);
    }
  }

}
