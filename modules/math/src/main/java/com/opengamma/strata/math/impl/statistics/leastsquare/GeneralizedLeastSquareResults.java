/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.BasisFunctionAggregation;

/**
 * 
 * @param <T> The type of the inputs to the basis functions
 */
public class GeneralizedLeastSquareResults<T> extends LeastSquareResults {

  private final Function<T, Double> _function;

  /**
   * Creates an instance
   * 
   * @param basisFunctions  the basis functions
   * @param chiSq  the chi-squared of the fit
   * @param parameters  the parameters that were fit
   * @param covariance  the covariance matrix of the result
   */
  public GeneralizedLeastSquareResults(
      List<Function<T, Double>> basisFunctions,
      double chiSq,
      DoubleArray parameters,
      DoubleMatrix covariance) {

    super(chiSq, parameters, covariance, null);

    _function = new BasisFunctionAggregation<>(basisFunctions, parameters.toArray());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the functions field.
   * @return the functions
   */
  public Function<T, Double> getFunction() {
    return _function;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + _function.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof GeneralizedLeastSquareResults)) {
      return false;
    }
    GeneralizedLeastSquareResults<?> other = (GeneralizedLeastSquareResults<?>) obj;
    if (!Objects.equals(_function, other._function)) {
      return false;
    }
    return true;
  }

}
