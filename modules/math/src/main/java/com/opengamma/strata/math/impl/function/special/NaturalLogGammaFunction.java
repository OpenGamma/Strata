/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import java.util.function.Function;

import org.apache.commons.math3.special.Gamma;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The natural logarithm of the Gamma function {@link GammaFunction}.
 * <p>
 * This class is a wrapper for the
 * <a href="http://commons.apache.org/proper/commons-math/javadocs/api-3.5/org/apache/commons/math3/special/Gamma.html">Commons Math library implementation</a> 
 * of the log-Gamma function
 */
public class NaturalLogGammaFunction implements Function<Double, Double> {

  @Override
  public Double apply(Double x) {
    ArgChecker.isTrue(x > 0, "x must be greater than zero");
    return Gamma.logGamma(x);
  }

}
