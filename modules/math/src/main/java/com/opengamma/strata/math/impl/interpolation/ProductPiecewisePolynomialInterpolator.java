/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;

/**
 * Given a data set {xValues[i], yValues[i]}, interpolate {xValues[i], xValues[i] * yValues[i]} by a piecewise polynomial function. 
 * The interpolation can be clamped at {xValuesClamped[j], xValuesClamped[j] * yValuesClamped[j]}, i.e., {xValuesClamped[j], yValuesClamped[j]}, 
 * where the extra points can be inside or outside the data range. 
 * By default right extrapolation is completed with a linear function, whereas default left extrapolation uses polynomial coefficients for the leftmost interval 
 * and left linear extrapolation can be straightforwardly computed from the coefficients.
 * This default setting is changed by adding extra node points outside the data range. 
 */
public class ProductPiecewisePolynomialInterpolator extends PiecewisePolynomialInterpolator {
  private final PiecewisePolynomialInterpolator _baseMethod;
  private final double[] _xValuesClamped;
  private final double[] _yValuesClamped;
  private final boolean _isClamped;
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC = new PiecewisePolynomialWithSensitivityFunction1D();
  private static final double EPS = 1.0e-15;

  /**
   * Construct the interpolator without clamped points. 
   * @param baseMethod The base interpolator must not be itself
   */
  public ProductPiecewisePolynomialInterpolator(PiecewisePolynomialInterpolator baseMethod) {
    ArgChecker.notNull(baseMethod, "baseMethod");
    ArgChecker.isFalse(baseMethod instanceof ProductPiecewisePolynomialInterpolator,
        "baseMethod should not be ProductPiecewisePolynomialInterpolator");
    _baseMethod = baseMethod;
    _xValuesClamped = null;
    _yValuesClamped = null;
    _isClamped = false;
  }

  /**
   * Construct the interpolator with clamped points.
   * @param baseMethod The base interpolator must be not be itself
   * @param xValuesClamped X values of the clamped points
   * @param yValuesClamped Y values of the clamped points
   */
  public ProductPiecewisePolynomialInterpolator(PiecewisePolynomialInterpolator baseMethod, double[] xValuesClamped,
      double[] yValuesClamped) {
    ArgChecker.notNull(baseMethod, "method");
    ArgChecker.notNull(xValuesClamped, "xValuesClamped");
    ArgChecker.notNull(yValuesClamped, "yValuesClamped");
    ArgChecker.isFalse(baseMethod instanceof ProductPiecewisePolynomialInterpolator,
        "baseMethod should not be ProductPiecewisePolynomialInterpolator");
    int nExtraPoints = xValuesClamped.length;
    ArgChecker.isTrue(yValuesClamped.length == nExtraPoints,
        "xValuesClamped and yValuesClamped should be the same length");
    _baseMethod = baseMethod;
    _xValuesClamped = Arrays.copyOf(xValuesClamped, nExtraPoints);
    _yValuesClamped = Arrays.copyOf(yValuesClamped, nExtraPoints);
    _isClamped = true;
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    PiecewisePolynomialResult result;
    if (_isClamped) {
      double[][] xyValuesAll = getDataTotal(xValues, yValues);
      result = _baseMethod.interpolate(xyValuesAll[0], xyValuesAll[1]);
    } else {
      double[] xyValues = getProduct(xValues, yValues);
      result = _baseMethod.interpolate(xValues, xyValues);
    }
    return extrapolateByLinearFunction(result, xValues);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[][] yValuesMatrix) {
    throw new UnsupportedOperationException("Use 1D interpolation method");
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(double[] xValues, double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");
    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    PiecewisePolynomialResultsWithSensitivity result;
    if (_isClamped) {
      double[][] xyValuesAll = getDataTotal(xValues, yValues);
      result = _baseMethod.interpolateWithSensitivity(xyValuesAll[0], xyValuesAll[1]);
    } else {
      double[] xyValues = getProduct(xValues, yValues);
      result = _baseMethod.interpolateWithSensitivity(xValues, xyValues);
    }
    return (PiecewisePolynomialResultsWithSensitivity) extrapolateByLinearFunction(result, xValues);
  }

  /**
   * Left extrapolation by linear function unless extra node is added on the left 
   */
  private PiecewisePolynomialResult extrapolateByLinearFunction(PiecewisePolynomialResult result, double[] xValues) {
    int nIntervalsAll = result.getNumberOfIntervals();
    double[] nodes = result.getKnots().toArray();
    if (Math.abs(xValues[xValues.length - 1] - nodes[nIntervalsAll]) < EPS) {
      double lastNodeX = nodes[nIntervalsAll];
      double lastNodeY = FUNC.evaluate(result, lastNodeX).get(0);
      double extraNode = 2.0 * nodes[nIntervalsAll] - nodes[nIntervalsAll - 1];
      double extraDerivative = FUNC.differentiate(result, lastNodeX).get(0);
      double[] newKnots = new double[nIntervalsAll + 2];
      System.arraycopy(nodes, 0, newKnots, 0, nIntervalsAll + 1);
      newKnots[nIntervalsAll + 1] = extraNode; // dummy node, outside the data range
      double[][] newCoefMatrix = new double[nIntervalsAll + 1][];
      for (int i = 0; i < nIntervalsAll; ++i) {
        newCoefMatrix[i] = Arrays.copyOf(result.getCoefMatrix().row(i).toArray(), result.getOrder());
      }
      newCoefMatrix[nIntervalsAll] = new double[result.getOrder()];
      newCoefMatrix[nIntervalsAll][result.getOrder() - 1] = lastNodeY;
      newCoefMatrix[nIntervalsAll][result.getOrder() - 2] = extraDerivative;
      if (result instanceof PiecewisePolynomialResultsWithSensitivity) {
        PiecewisePolynomialResultsWithSensitivity resultCast = (PiecewisePolynomialResultsWithSensitivity) result;
        double[] extraSense = FUNC.nodeSensitivity(resultCast, lastNodeX).toArray();
        double[] extraSenseDer = FUNC.differentiateNodeSensitivity(resultCast, lastNodeX).toArray();
        DoubleMatrix[] newCoefSense = new DoubleMatrix[nIntervalsAll + 1];
        for (int i = 0; i < nIntervalsAll; ++i) {
          newCoefSense[i] = resultCast.getCoefficientSensitivity(i);
        }
        double[][] extraCoefSense = new double[resultCast.getOrder()][extraSense.length];
        extraCoefSense[resultCast.getOrder() - 1] = Arrays.copyOf(extraSense, extraSense.length);
        extraCoefSense[resultCast.getOrder() - 2] = Arrays.copyOf(extraSenseDer, extraSenseDer.length);
        newCoefSense[nIntervalsAll] = DoubleMatrix.copyOf(extraCoefSense);
        return new PiecewisePolynomialResultsWithSensitivity(
            DoubleArray.copyOf(newKnots),
            DoubleMatrix.copyOf(newCoefMatrix),
            resultCast.getOrder(),
            1,
            newCoefSense);
      }
      return new PiecewisePolynomialResult(
          DoubleArray.copyOf(newKnots), DoubleMatrix.copyOf(newCoefMatrix), result.getOrder(), 1);
    }
    return result;
  }

  @Override
  public PiecewisePolynomialInterpolator getPrimaryMethod() {
    return _baseMethod;
  }

  private double[][] getDataTotal(double[] xData, double[] yData) {
    int nExtraPoints = _xValuesClamped.length;
    int nData = xData.length;
    int nTotal = nExtraPoints + nData;
    double[] xValuesTotal = new double[nTotal];
    double[] yValuesTotal = new double[nTotal];
    System.arraycopy(xData, 0, xValuesTotal, 0, nData);
    System.arraycopy(yData, 0, yValuesTotal, 0, nData);
    System.arraycopy(_xValuesClamped, 0, xValuesTotal, nData, nExtraPoints);
    System.arraycopy(_yValuesClamped, 0, yValuesTotal, nData, nExtraPoints);
    DoubleArrayMath.sortPairs(xValuesTotal, yValuesTotal);
    double[] xyTotal = getProduct(xValuesTotal, yValuesTotal);
    return new double[][] {xValuesTotal, xyTotal };
  }

  private double[] getProduct(double[] x, double[] y) {
    int n = x.length;
    double[] xy = new double[n];
    for (int i = 0; i < n; ++i) {
      xy[i] = x[i] * y[i];
    }
    return xy;
  }
}
