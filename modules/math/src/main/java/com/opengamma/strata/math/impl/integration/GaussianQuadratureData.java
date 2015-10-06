/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Class holding the results of calculations of weights and abscissas by {@link QuadratureWeightAndAbscissaFunction}. 
 */
public class GaussianQuadratureData {

  private final double[] _weights;
  private final double[] _abscissas;

  /**
   * @param abscissas An array containing the abscissas, not null
   * @param weights An array containing the weights, not null, must be the same length as the abscissa array
   */
  public GaussianQuadratureData(double[] abscissas, double[] weights) {
    ArgChecker.notNull(abscissas, "abscissas");
    ArgChecker.notNull(weights, "weights");
    ArgChecker.isTrue(abscissas.length == weights.length, "Abscissa and weight arrays must be the same length");
    _weights = weights;
    _abscissas = abscissas;
  }

  /**
   * @return The weights
   */
  public double[] getWeights() {
    return _weights;
  }

  /**
   * @return The abscissas
   */
  public double[] getAbscissas() {
    return _abscissas;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_abscissas);
    result = prime * result + Arrays.hashCode(_weights);
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
    GaussianQuadratureData other = (GaussianQuadratureData) obj;
    if (!Arrays.equals(_abscissas, other._abscissas)) {
      return false;
    }
    return Arrays.equals(_weights, other._weights);
  }

}
