/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.special.Gamma;

/**
 * The gamma function is a generalization of the factorial to complex and real
 * numbers. It is defined by the integral:
 * $$
 * \begin{equation*}
 * \Gamma(z)=\int_0^\infty t^{z-1}e^{-t}dt
 * \end{equation*}
 * $$
 * and is related to the factorial by
 * $$
 * \begin{equation*}
 * \Gamma(n+1)=n!
 * \end{equation*}
 * $$
 * It is analytic everywhere but $z=0, -1, -2, \ldots$
 * <p>
 * This class is a wrapper for the <a href="http://commons.apache.org/proper/commons-math/javadocs/api-3.5/org/apache/commons/math3/special/Gamma.html">Commons Math library implementation</a> 
 * of the Gamma function.
 * 
 */
public class GammaFunction implements DoubleUnaryOperator {

  @Override
  public double applyAsDouble(double x) {
    if (x > 0.0) {
      return Math.exp(Gamma.logGamma(x));
    }
    return Math.PI / Math.sin(Math.PI * x) / applyAsDouble(1 - x);
  }

}
