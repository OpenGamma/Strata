/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * 
 * Romberg's method estimates an integral by repeatedly using <a href="http://en.wikipedia.org/wiki/Richardson_extrapolation">Richardson extrapolation</a> 
 * on the extended trapezium rule {@link ExtendedTrapezoidIntegrator1D}. 
 * <p>
 * This class is a wrapper for the
 * <a href="http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/analysis/integration/RombergIntegrator.html">Commons Math library implementation</a> 
 * of Romberg integration.
 */
public class RombergIntegrator1D extends Integrator1D<Double, Double> {

  private static final Logger log = LoggerFactory.getLogger(RombergIntegrator1D.class);
  private static final int MAX_EVAL = 10000;
  private final UnivariateIntegrator integrator = new RombergIntegrator();

  /**
   * Romberg integration method. Note that the Commons implementation fails if the lower bound is larger than the upper - 
   * in this case, the bounds are reversed and the result negated. 
   * @param f The function to integrate, not null
   * @param lower The lower bound, not null
   * @param upper The upper bound, not null
   * @return The result of the integration
   */
  @Override
  public Double integrate(Function<Double, Double> f, Double lower, Double upper) {
    ArgChecker.notNull(f, "f");
    ArgChecker.notNull(lower, "lower bound");
    ArgChecker.notNull(upper, "upper bound");

    try {
      if (lower < upper) {
        return integrator.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), lower, upper);
      }
      log.info("Upper bound was less than lower bound; swapping bounds and negating result");
      return -integrator.integrate(MAX_EVAL, CommonsMathWrapper.wrapUnivariate(f), upper, lower);
    } catch (MaxCountExceededException | MathIllegalArgumentException e) {
      throw new MathException(e);
    }
  }

}
