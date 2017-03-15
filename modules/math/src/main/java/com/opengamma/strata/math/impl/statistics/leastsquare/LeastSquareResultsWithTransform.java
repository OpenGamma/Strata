/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;

/**
 * Container for the results of a least square (minimum chi-square) fit, where some model (with a set of parameters), is calibrated
 * to a data set, but the model parameters are first transformed to some fitting parameters (usually to impose some constants).
 */
public class LeastSquareResultsWithTransform extends LeastSquareResults {

  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  private final NonLinearParameterTransforms _transform;
  private final DoubleArray _modelParameters;
  private DoubleMatrix _inverseJacobianModelPararms;

  public LeastSquareResultsWithTransform(LeastSquareResults transformedFitResult) {
    super(transformedFitResult);
    _transform = null;
    _modelParameters = transformedFitResult.getFitParameters();
    _inverseJacobianModelPararms = getFittingParameterSensitivityToData();
  }

  public LeastSquareResultsWithTransform(LeastSquareResults transformedFitResult, NonLinearParameterTransforms transform) {
    super(transformedFitResult);
    ArgChecker.notNull(transform, "null transform");
    _transform = transform;
    _modelParameters = transform.inverseTransform(getFitParameters());
  }

  public DoubleArray getModelParameters() {
    return _modelParameters;
  }

  /**
   * This a matrix where the i,j-th element is the (infinitesimal) sensitivity of the i-th model parameter 
   * to the j-th data point, when the fitting parameter are such that the chi-squared is minimised. 
   * So it is a type of (inverse) Jacobian, but should not be confused with the model jacobian 
   * (sensitivity of model parameters to internal parameters used in calibration procedure) or its inverse.
   * @return a matrix
   */
  public DoubleMatrix getModelParameterSensitivityToData() {
    if (_inverseJacobianModelPararms == null) {
      setModelParameterSensitivityToData();
    }
    return _inverseJacobianModelPararms;
  }

  private void setModelParameterSensitivityToData() {
    DoubleMatrix invJac = _transform.inverseJacobian(getFitParameters());
    _inverseJacobianModelPararms = (DoubleMatrix) MA.multiply(invJac, getFittingParameterSensitivityToData());
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((_inverseJacobianModelPararms == null) ? 0 : _inverseJacobianModelPararms.hashCode());
    result = prime * result + ((_modelParameters == null) ? 0 : _modelParameters.hashCode());
    result = prime * result + ((_transform == null) ? 0 : _transform.hashCode());
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
    if (getClass() != obj.getClass()) {
      return false;
    }
    LeastSquareResultsWithTransform other = (LeastSquareResultsWithTransform) obj;
    if (_inverseJacobianModelPararms == null) {
      if (other._inverseJacobianModelPararms != null) {
        return false;
      }
    } else if (!_inverseJacobianModelPararms.equals(other._inverseJacobianModelPararms)) {
      return false;
    }
    if (_modelParameters == null) {
      if (other._modelParameters != null) {
        return false;
      }
    } else if (!_modelParameters.equals(other._modelParameters)) {
      return false;
    }
    if (_transform == null) {
      if (other._transform != null) {
        return false;
      }
    } else if (!_transform.equals(other._transform)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "LeastSquareResults [chiSq=" + getChiSq() + ", fit parameters=" + getFitParameters().toString() +
        ", model parameters= " + getModelParameters().toString() + ", covariance="
        + getCovariance().toString() + "]";
  }

}
