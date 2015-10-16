/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import java.util.Objects;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Container for the results of a least square (minimum chi-square) fit, where some model (with a set of parameters), is calibrated
 * to a data set.
 */
public class LeastSquareResults {

  private final double _chiSq;
  private final DoubleArray _parameters;
  private final DoubleMatrix _covariance;
  private final DoubleMatrix _inverseJacobian;

  public LeastSquareResults(LeastSquareResults from) {
    this(from._chiSq, from._parameters, from._covariance, from._inverseJacobian);
  }

  public LeastSquareResults(double chiSq, DoubleArray parameters, DoubleMatrix covariance) {
    this(chiSq, parameters, covariance, null);
  }

  public LeastSquareResults(
      double chiSq,
      DoubleArray parameters,
      DoubleMatrix covariance,
      DoubleMatrix inverseJacobian) {

    ArgChecker.isTrue(chiSq >= 0, "chi square < 0");
    ArgChecker.notNull(parameters, "parameters");
    ArgChecker.notNull(covariance, "covariance");
    int n = parameters.size();
    ArgChecker.isTrue(covariance.columnCount() == n, "covariance matrix not square");
    ArgChecker.isTrue(covariance.rowCount() == n, "covariance matrix wrong size");
    //TODO test size of inverse Jacobian
    _chiSq = chiSq;
    _parameters = parameters;
    _covariance = covariance;
    _inverseJacobian = inverseJacobian;
  }

  /**
   * Gets the Chi-square of the fit
   * @return the chiSq
   */
  public double getChiSq() {
    return _chiSq;
  }

  /**
   * Gets the value of the fitting parameters, when the chi-squared is minimised
   * @return the parameters
   */
  public DoubleArray getFitParameters() {
    return _parameters;
  }

  /**
   * Gets the estimated covariance matrix of the standard errors in the fitting parameters.
   * <b>Note</b> only in the case of normally distributed errors, does this have any meaning
   * full mathematical interpretation (See NR third edition, p812-816)
   * @return the formal covariance matrix
   */
  public DoubleMatrix getCovariance() {
    return _covariance;
  }

  /**
   * This a matrix where the i,jth element is the (infinitesimal) sensitivity of the ith fitting
   * parameter to the jth data point (NOT the model point), when the fitting parameter are such
   * that the chi-squared is minimised. So it is a type of (inverse) Jacobian, but should not be
   * confused with the model jacobian (sensitivity of model data points, to parameters) or its inverse.
   * 
   * @return a matrix
   */
  public DoubleMatrix getFittingParameterSensitivityToData() {
    if (_inverseJacobian == null) {
      throw new UnsupportedOperationException("The inverse Jacobian was not set");
    }
    return _inverseJacobian;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_chiSq);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + _covariance.hashCode();
    result = prime * result + _parameters.hashCode();
    result = prime * result + (_inverseJacobian == null ? 0 : _inverseJacobian.hashCode());
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
    LeastSquareResults other = (LeastSquareResults) obj;
    if (Double.doubleToLongBits(_chiSq) != Double.doubleToLongBits(other._chiSq)) {
      return false;
    }
    if (!Objects.equals(_covariance, other._covariance)) {
      return false;
    }
    if (!Objects.equals(_inverseJacobian, other._inverseJacobian)) {
      return false;
    }
    return Objects.equals(_parameters, other._parameters);
  }

  @Override
  public String toString() {
    return "LeastSquareResults [chiSq=" + _chiSq + ", fit parameters=" + _parameters.toString() +
        ", covariance=" + _covariance.toString() + "]";
  }

}
