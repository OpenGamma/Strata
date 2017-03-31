/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.COMMONS_ALGEBRA;
import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.DecompositionResult;
import com.opengamma.strata.math.impl.linearalgebra.QRDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.QRDecompositionResult;
import com.opengamma.strata.math.impl.regression.LeastSquaresRegressionResult;
import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleStandardDeviationCalculator;

/**
 * Derive coefficients of n-degree polynomial that minimizes least squares error of fit by
 * using QR decomposition and back substitution.
 */
public class PolynomialsLeastSquaresFitter {

  private QRDecompositionResult _qrResult;
  private final double[] _renorm = new double[2];

  /**
   * Given a set of data (X_i, Y_i) and degrees of a polynomial, determines optimal coefficients of the polynomial.
   * @param xData X values of data
   * @param yData Y values of data
   * @param degree Degree of polynomial which fits the given data
   * @return LeastSquaresRegressionResult Containing optimal coefficients of the polynomial and difference between yData[i] and f(xData[i]),
   *   where f() is the polynomial with the derived coefficients
   */
  public LeastSquaresRegressionResult regress(double[] xData, double[] yData, int degree) {

    return regress(xData, yData, degree, false);
  }

  /**
   * Alternative regression method with different output.
   * @param xData X values of data
   * @param yData Y values of data
   * @param degree Degree of polynomial which fits the given data
   * @param normalize Normalize xData by mean and standard deviation if normalize == true
   * @return PolynomialsLeastSquaresRegressionResult containing coefficients, rMatrix, degrees of freedom, norm of residuals, and mean, standard deviation
   */
  public PolynomialsLeastSquaresFitterResult regressVerbose(
      double[] xData,
      double[] yData,
      int degree,
      boolean normalize) {

    LeastSquaresRegressionResult result = regress(xData, yData, degree, normalize);

    int nData = xData.length;
    DoubleMatrix rMatriX = _qrResult.getR();

    DoubleArray resResult = DoubleArray.copyOf(result.getResiduals());
    double resNorm = OG_ALGEBRA.getNorm2(resResult);

    if (normalize == true) {
      return new PolynomialsLeastSquaresFitterResult(result.getBetas(), rMatriX, nData - degree - 1, resNorm, _renorm);
    }
    return new PolynomialsLeastSquaresFitterResult(result.getBetas(), rMatriX, nData - degree - 1, resNorm);
  }

  /**
   * This regression method is private and called in other regression methods
   * @param xData X values of data
   * @param yData Y values of data
   * @param degree Degree of polynomial which fits the given data
   * @param normalize Normalize xData by mean and standard deviation if normalize == true
   * @return LeastSquaresRegressionResult Containing optimal coefficients of the polynomial and difference between yData[i] and f(xData[i])
   */
  private LeastSquaresRegressionResult regress(double[] xData, double[] yData, int degree, boolean normalize) {

    ArgChecker.notNull(xData, "xData");
    ArgChecker.notNull(yData, "yData");

    ArgChecker.isTrue(degree >= 0, "Minus degree");
    ArgChecker.isTrue(xData.length == yData.length, "xData length should be the same as yData length");
    ArgChecker.isTrue(xData.length > degree, "Not enough amount of data");

    int nData = xData.length;

    for (int i = 0; i < nData; ++i) {
      ArgChecker.isFalse(Double.isNaN(xData[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xData[i]), "xData containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yData[i]), "yData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yData[i]), "yData containing Infinity");
    }

    for (int i = 0; i < nData; ++i) {
      for (int j = i + 1; j < nData; ++j) {
        ArgChecker.isFalse(xData[i] == xData[j] && yData[i] != yData[j], "Two distinct data on x=const. line");
      }
    }

    int nRepeat = 0;
    for (int i = 0; i < nData; ++i) {
      for (int j = i + 1; j < nData; ++j) {
        if (xData[i] == xData[j] && yData[i] == yData[j]) {
          ++nRepeat;
        }
      }
    }
    ArgChecker.isFalse(nRepeat > nData - degree - 1, "Too many repeated data");

    double[][] tmpMatrix = new double[nData][degree + 1];

    if (normalize == true) {
      double[] normData = normaliseData(xData);
      for (int i = 0; i < nData; ++i) {
        for (int j = 0; j < degree + 1; ++j) {
          tmpMatrix[i][j] = Math.pow(normData[i], j);
        }
      }
    } else {
      for (int i = 0; i < nData; ++i) {
        for (int j = 0; j < degree + 1; ++j) {
          tmpMatrix[i][j] = Math.pow(xData[i], j);
        }
      }
    }

    DoubleMatrix xDataMatrix = DoubleMatrix.copyOf(tmpMatrix);
    DoubleArray yDataVector = DoubleArray.copyOf(yData);

    double vandNorm = COMMONS_ALGEBRA.getNorm2(xDataMatrix);
    ArgChecker.isFalse(vandNorm > 1e9, "Too large input data or too many degrees");

    return regress(xDataMatrix, yDataVector, nData, degree);

  }

  /**
   * This regression method is private and called in other regression methods
   * @param xDataMatrix _nData x (_degree + 1) matrix whose low vector is (xData[i]^0, xData[i]^1, ..., xData[i]^{_degree})
   * @param yDataVector the y-values
   * @param nData Number of data points
   * @param degree  the degree
   */
  private LeastSquaresRegressionResult regress(
      DoubleMatrix xDataMatrix,
      DoubleArray yDataVector,
      int nData,
      int degree) {

    Decomposition<QRDecompositionResult> qrComm = new QRDecompositionCommons();

    DecompositionResult decompResult = qrComm.apply(xDataMatrix);
    _qrResult = (QRDecompositionResult) decompResult;

    DoubleMatrix qMatrix = _qrResult.getQ();
    DoubleMatrix rMatrix = _qrResult.getR();

    double[] betas = backSubstitution(qMatrix, rMatrix, yDataVector, degree);
    double[] residuals = residualsSolver(xDataMatrix, betas, yDataVector);

    for (int i = 0; i < degree + 1; ++i) {
      ArgChecker.isFalse(Double.isNaN(betas[i]), "Input is too large or small");
    }
    for (int i = 0; i < nData; ++i) {
      ArgChecker.isFalse(Double.isNaN(residuals[i]), "Input is too large or small");
    }

    return new LeastSquaresRegressionResult(betas, residuals, 0.0, null, 0.0, 0.0, null, null, true);

  }

  /**
   * Under the QR decomposition, xDataMatrix = qMatrix * rMatrix, optimal coefficients of the
   * polynomial are computed by back substitution
   * @param qMatrix  the q-matrix
   * @param rMatrix  the r-matrix
   * @param yDataVector  the y-values
   * @param degree  the degree
   * @return Coefficients of the polynomial which minimize least square
   */
  private double[] backSubstitution(
      DoubleMatrix qMatrix,
      DoubleMatrix rMatrix,
      DoubleArray yDataVector,
      int degree) {

    double[] res = new double[degree + 1];
    Arrays.fill(res, 0.);

    DoubleMatrix tpMatrix = OG_ALGEBRA.getTranspose(qMatrix);
    DoubleArray yDataVecConv = (DoubleArray) OG_ALGEBRA.multiply(tpMatrix, yDataVector);

    for (int i = 0; i < degree + 1; ++i) {
      double tmp = 0.;
      for (int j = 0; j < i; ++j) {
        tmp -= rMatrix.get(degree - i, degree - j) * res[degree - j] / rMatrix.get(degree - i, degree - i);
      }
      res[degree - i] = yDataVecConv.get(degree - i) / rMatrix.get(degree - i, degree - i) + tmp;
    }

    return res;
  }

  /**
   *
   * @param xDataMatrix  the x-matrix
   * @param betas Optimal coefficients of the polynomial
   * @param yDataVector  the y-vlaues
   * @return Difference between yData[i] and f(xData[i]), where f() is the polynomial with derived coefficients
   */
  private double[] residualsSolver(DoubleMatrix xDataMatrix, double[] betas, DoubleArray yDataVector) {

    DoubleArray betasVector = DoubleArray.copyOf(betas);

    DoubleArray modelValuesVector = (DoubleArray) OG_ALGEBRA.multiply(xDataMatrix, betasVector);
    DoubleArray res = (DoubleArray) OG_ALGEBRA.subtract(yDataVector, modelValuesVector);

    return res.toArray();

  }

  /**
   * Normalize x_i as x_i -> (x_i - mean)/(standard deviation)
   * @param xData X values of data
   * @return Normalized X values
   */
  private double[] normaliseData(double[] xData) {

    int nData = xData.length;
    double[] res = new double[nData];

    Function<double[], Double> calculator = new MeanCalculator();
    _renorm[0] = calculator.apply(xData);
    calculator = new SampleStandardDeviationCalculator();
    _renorm[1] = calculator.apply(xData);

    double tmp = _renorm[0] / _renorm[1];
    for (int i = 0; i < nData; ++i) {
      res[i] = xData[i] / _renorm[1] - tmp;
    }

    return res;
  }

}
