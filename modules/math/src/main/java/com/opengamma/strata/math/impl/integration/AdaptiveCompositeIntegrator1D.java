/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Adaptive composite integrator: step size is set to be small if functional variation of integrand is large
 * The integrator in individual intervals (base integrator) should be specified by constructor
 */
public class AdaptiveCompositeIntegrator1D extends Integrator1D<Double, Double> {
  private static final Logger log = LoggerFactory.getLogger(AdaptiveCompositeIntegrator1D.class);
  private final Integrator1D<Double, Double> integrator;
  private static final int MAX_IT = 15;
  private final double gain;
  private final double tol;

  /**
   * @param integrator The base integrator 
   */
  public AdaptiveCompositeIntegrator1D(Integrator1D<Double, Double> integrator) {
    ArgChecker.notNull(integrator, "integrator");
    this.integrator = integrator;
    this.gain = 15.;
    this.tol = 1.e-13;
  }

  /**
   * @param integrator The base integrator
   * @param gain The gain ratio
   * @param tol The tolerance
   */
  public AdaptiveCompositeIntegrator1D(Integrator1D<Double, Double> integrator, double gain, double tol) {
    ArgChecker.notNull(integrator, "integrator");
    this.integrator = integrator;
    this.gain = gain;
    this.tol = tol;
  }

  @Override
  public Double integrate(Function<Double, Double> f, Double lower, Double upper) {
    ArgChecker.notNull(f, "f");
    ArgChecker.notNull(lower, "lower bound");
    ArgChecker.notNull(upper, "upper bound");
    try {
      if (lower < upper) {
        return integration(f, lower, upper);
      }
      log.info("Upper bound was less than lower bound; swapping bounds and negating result");
      return -integration(f, upper, lower);
    } catch (Exception e) {
      throw new IllegalStateException("function evaluation returned NaN or Inf");
    }
  }

  private Double integration(Function<Double, Double> f, Double lower, Double upper) {
    double res = integrator.integrate(f, lower, upper);
    return integrationRec(f, lower, upper, res, MAX_IT);
  }

  private double integrationRec(Function<Double, Double> f, double lower, double upper, double res, double counter) {
    double localTol = gain * tol;
    double half = 0.5 * (lower + upper);
    double newResDw = integrator.integrate(f, lower, half);
    double newResUp = integrator.integrate(f, half, upper);
    double newRes = newResUp + newResDw;

    if (Math.abs(res - newRes) < localTol || counter == 0 ||
        (Math.abs(res) < 1.e-14 && Math.abs(newResUp) < 1.e-14 && Math.abs(newResDw) < 1.e-14)) {
      return newRes + (newRes - res) / gain;
    }

    return integrationRec(f, lower, half, newResDw, counter - 1) +
        integrationRec(f, half, upper, newResUp, counter - 1);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(gain);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + integrator.hashCode();
    temp = Double.doubleToLongBits(tol);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AdaptiveCompositeIntegrator1D)) {
      return false;
    }
    AdaptiveCompositeIntegrator1D other = (AdaptiveCompositeIntegrator1D) obj;
    if (Double.doubleToLongBits(this.gain) != Double.doubleToLongBits(other.gain)) {
      return false;
    }
    if (!this.integrator.equals(other.integrator)) {
      return false;
    }
    if (Double.doubleToLongBits(this.tol) != Double.doubleToLongBits(other.tol)) {
      return false;
    }
    return true;
  }

}
