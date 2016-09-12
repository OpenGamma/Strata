/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Contains the result of a least squares regression.
 */
public class LeastSquaresRegressionResult {
  //TODO the predicted value calculation should be separated out from this class.

  private final double[] _residuals;
  private final double[] _betas;
  private final double _meanSquareError;
  private final double[] _standardErrorOfBeta;
  private final double _rSquared;
  private final double _rSquaredAdjusted;
  private final double[] _tStats;
  private final double[] _pValues;
  private final boolean _hasIntercept;

  public LeastSquaresRegressionResult(LeastSquaresRegressionResult result) {
    ArgChecker.notNull(result, "regression result");
    _betas = result.getBetas();
    _residuals = result.getResiduals();
    _meanSquareError = result.getMeanSquareError();
    _standardErrorOfBeta = result.getStandardErrorOfBetas();
    _rSquared = result.getRSquared();
    _rSquaredAdjusted = result.getAdjustedRSquared();
    _tStats = result.getTStatistics();
    _pValues = result.getPValues();
    _hasIntercept = result.hasIntercept();
  }

  public LeastSquaresRegressionResult(
      double[] betas,
      double[] residuals,
      double meanSquareError,
      double[] standardErrorOfBeta,
      double rSquared,
      double rSquaredAdjusted,
      double[] tStats,
      double[] pValues,
      boolean hasIntercept) {

    _betas = betas;
    _residuals = residuals;
    _meanSquareError = meanSquareError;
    _standardErrorOfBeta = standardErrorOfBeta;
    _rSquared = rSquared;
    _rSquaredAdjusted = rSquaredAdjusted;
    _tStats = tStats;
    _pValues = pValues;
    _hasIntercept = hasIntercept;
  }

  public double[] getBetas() {
    return _betas;
  }

  public double[] getResiduals() {
    return _residuals;
  }

  public double getMeanSquareError() {
    return _meanSquareError;
  }

  public double[] getStandardErrorOfBetas() {
    return _standardErrorOfBeta;
  }

  public double getRSquared() {
    return _rSquared;
  }

  public double getAdjustedRSquared() {
    return _rSquaredAdjusted;
  }

  public double[] getTStatistics() {
    return _tStats;
  }

  public double[] getPValues() {
    return _pValues;
  }

  public boolean hasIntercept() {
    return _hasIntercept;
  }

  public double getPredictedValue(double[] x) {
    ArgChecker.notNull(x, "x");
    double[] betas = getBetas();
    if (hasIntercept()) {
      if (x.length != betas.length - 1) {
        throw new IllegalArgumentException("Number of variables did not match number used in regression");
      }
    } else {
      if (x.length != betas.length) {
        throw new IllegalArgumentException("Number of variables did not match number used in regression");
      }
    }
    double sum = 0;
    for (int i = 0; i < (hasIntercept() ? x.length + 1 : x.length); i++) {
      if (hasIntercept()) {
        if (i == 0) {
          sum += betas[0];
        } else {
          sum += betas[i] * x[i - 1];
        }
      } else {
        sum += x[i] * betas[i];
      }
    }
    return sum;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_betas);
    result = prime * result + (_hasIntercept ? 1231 : 1237);
    long temp;
    temp = Double.doubleToLongBits(_meanSquareError);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_pValues);
    temp = Double.doubleToLongBits(_rSquared);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_rSquaredAdjusted);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_residuals);
    result = prime * result + Arrays.hashCode(_standardErrorOfBeta);
    result = prime * result + Arrays.hashCode(_tStats);
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
    LeastSquaresRegressionResult other = (LeastSquaresRegressionResult) obj;
    if (!Arrays.equals(_betas, other._betas)) {
      return false;
    }
    if (_hasIntercept != other._hasIntercept) {
      return false;
    }
    if (Double.doubleToLongBits(_meanSquareError) != Double.doubleToLongBits(other._meanSquareError)) {
      return false;
    }
    if (!Arrays.equals(_pValues, other._pValues)) {
      return false;
    }
    if (Double.doubleToLongBits(_rSquared) != Double.doubleToLongBits(other._rSquared)) {
      return false;
    }
    if (Double.doubleToLongBits(_rSquaredAdjusted) != Double.doubleToLongBits(other._rSquaredAdjusted)) {
      return false;
    }
    if (!Arrays.equals(_residuals, other._residuals)) {
      return false;
    }
    if (!Arrays.equals(_standardErrorOfBeta, other._standardErrorOfBeta)) {
      return false;
    }
    if (!Arrays.equals(_tStats, other._tStats)) {
      return false;
    }
    return true;
  }
}
