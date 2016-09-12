/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the result of a least squares regression for polynomial.
 */
public class PolynomialsLeastSquaresFitterResult {

  private double[] _coefficients;
  private DoubleMatrix _rMatrix;
  private int _dof;
  private double _diffNorm;
  private double[] _meanAndStd;

  /**
   * @param coefficients Coefficients of the polynomial
   * @param rMatrix R-matrix of the QR decomposition used in PolynomialsLeastSquaresRegression
   * @param dof Degrees of freedom = Number of data points - (degrees of Polynomial + 1) 
   * @param diffNorm Square norm of the vector, "residuals," whose components are yData_i - f(xData_i)
   */
  public PolynomialsLeastSquaresFitterResult(final double[] coefficients, final DoubleMatrix rMatrix, final int dof, final double diffNorm) {

    _coefficients = coefficients;
    _rMatrix = rMatrix;
    _dof = dof;
    _diffNorm = diffNorm;
    _meanAndStd = null;

  }

  /**
   * @param coefficients Coefficients {a_0, a_1, a_2 ...} of the polynomial a_0 + a_1 x^1 + a_2 x^2 + ....
   * @param rMatrix R-matrix of the QR decomposition used in PolynomialsLeastSquaresRegression
   * @param dof Degrees of freedom = Number of data points - (degrees of Polynomial + 1) 
   * @param diffNorm Norm of the vector, "residuals," whose components are yData_i - f(xData_i)
   * @param meanAndStd Vector (mean , standard deviation) used in normalization 
   */
  public PolynomialsLeastSquaresFitterResult(final double[] coefficients, final DoubleMatrix rMatrix, final int dof, final double diffNorm, final double[] meanAndStd) {

    _coefficients = coefficients;
    _rMatrix = rMatrix;
    _dof = dof;
    _diffNorm = diffNorm;
    _meanAndStd = meanAndStd;

  }

  /**
   * @return Coefficients {a_0, a_1, a_2 ...} of polynomial a_0 + a_1 x^1 + a_2 x^2 + ....
   */
  public double[] getCoeff() {
    return _coefficients;
  }

  /**
   * @return R Matrix of QR decomposition
   */
  public DoubleMatrix getRMat() {
    return _rMatrix;
  }

  /**
   * @return Degrees of freedom = Number of data points - (degrees of Polynomial + 1) 
   */
  public int getDof() {
    return _dof;
  }

  /**
   * @return Norm of the vector, "residuals," whose components are yData_i - f(xData_i)
   */
  public double getDiffNorm() {
    return _diffNorm;
  }

  /**
   * @return Vector (mean , standard deviation) used in normalization 
   */
  public double[] getMeanAndStd() {
    ArgChecker.notNull(_meanAndStd, "xData are not normalized");
    return _meanAndStd;
  }

}
