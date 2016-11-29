/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.Objects;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.special.OrthogonalPolynomialFunctionGenerator;

/**
 * Class that performs integration using Gaussian quadrature.
 * <p>
 * If a function $f(x)$ can be written as $f(x) = W(x)g(x)$, where $g(x)$ is
 * approximately polynomial, then for suitably chosen weights $w_i$ and points
 * $x_i$, the integral can be approximated as:
 * $$
 * \begin{align*}
 * \int_{-1}^1 f(x)dx 
 * &=\int_{-1}^1 W(x)g(x)dx\\
 * &\approx \sum_{\i=1}^{n} w_i f(x_i)
 * \end{align*}
 * $$
 * The evaluation points, weights and valid limits of integration depend on the type of orthogonal
 * polynomials that are used 
 * (see {@link OrthogonalPolynomialFunctionGenerator} and {@link GaussLaguerreWeightAndAbscissaFunction}).
 * 
 */
public abstract class GaussianQuadratureIntegrator1D extends Integrator1D<Double, Double> {

  private final int size;
  private final QuadratureWeightAndAbscissaFunction generator;
  private final GaussianQuadratureData quadrature;

  /**
   * @param n The number of sample points to be used in the integration, not negative or zero
   * @param generator The generator of weights and abscissas
   */
  public GaussianQuadratureIntegrator1D(int n, QuadratureWeightAndAbscissaFunction generator) {
    ArgChecker.isTrue(n > 0, "number of intervals must be > 0");
    ArgChecker.notNull(generator, "generating function");
    this.size = n;
    this.generator = generator;
    this.quadrature = generator.generate(size);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double integrate(Function<Double, Double> function, Double lower, Double upper) {
    ArgChecker.notNull(function, "function");
    ArgChecker.notNull(lower, "lower");
    ArgChecker.notNull(upper, "upper");
    Function<Double, Double> integral = getIntegralFunction(function, lower, upper);
    return integrateFromPolyFunc(integral);
  }

  /**
   * If a function $g(x)$ can be written as $W(x)f(x)$, where the weight function $W(x)$ corresponds
   * to one of the Gaussian quadrature forms, then we may approximate the integral of $g(x)$ over
   * a specific range as $\int^b_a g(x) dx =\int^b_a W(x)f(x) dx \approx \sum_{i=0}^{N-1} w_i f(x_i)$,
   * were the abscissas $x_i$ and the weights $w_i$ have been precomputed. This is accurate
   * if $f(x)$ can be approximated by a polynomial.
   * 
   * @param polyFunction The function $f(x)$ rather than the full function $g(x) = W(x)f(x)$
   *  This should be well approximated by a polynomial.
   * @return The integral 
   */
  public double integrateFromPolyFunc(Function<Double, Double> polyFunction) {
    ArgChecker.notNull(polyFunction, "polyFunction");
    double[] abscissas = quadrature.getAbscissas();
    int n = abscissas.length;
    double[] weights = quadrature.getWeights();
    double sum = 0;
    for (int i = 0; i < n; i++) {
      sum += polyFunction.apply(abscissas[i]) * weights[i];
    }
    return sum;
  }

  /**
   * @return The lower and upper limits for which the quadrature is valid
   */
  public abstract Double[] getLimits();

  /**
   * Returns a function that is valid for both the type of quadrature and the limits of integration. 
   * @param function The function to be integrated, not null
   * @param lower The lower integration limit, not null
   * @param upper The upper integration limit, not null
   * @return A function in the appropriate form for integration
   */
  public abstract Function<Double, Double> getIntegralFunction(
      Function<Double, Double> function,
      Double lower,
      Double upper);

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + generator.hashCode();
    result = prime * result + size;
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
    if (getClass() != obj.getClass()) {
      return false;
    }
    GaussianQuadratureIntegrator1D other = (GaussianQuadratureIntegrator1D) obj;
    if (this.size != other.size) {
      return false;
    }
    return Objects.equals(this.generator, other.generator);
  }

}
