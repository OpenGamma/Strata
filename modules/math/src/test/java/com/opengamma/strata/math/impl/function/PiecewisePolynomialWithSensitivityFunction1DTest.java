/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.ConstrainedCubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.CubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewiseCubicHermiteSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.interpolation.SemiLocalCubicSplineInterpolator;

/**
 * Test.
 */
@Test
public class PiecewisePolynomialWithSensitivityFunction1DTest {
  private static final double EPS = 1.e-7;
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNCTION = new PiecewisePolynomialWithSensitivityFunction1D();

  /**
   * 
   */
  @Test
  public void firstDerivativeFiniteDifferenceTest() {
    final PiecewisePolynomialInterpolator[] interps = new PiecewisePolynomialInterpolator[] {new NaturalSplineInterpolator(), new CubicSplineInterpolator(),
      new PiecewiseCubicHermiteSplineInterpolator(), new ConstrainedCubicSplineInterpolator(), new SemiLocalCubicSplineInterpolator() };
    final int nInterps = interps.length;
    for (int k = 0; k < nInterps; ++k) {
      final double[] xValues = new double[] {1., 2.8, 3.1, 5.9, 10., 16. };
      final double[] yValues = new double[] {1., 2., 3., -2., 5., -5. };
      final int nData = xValues.length;
      double[] yValuesUp = Arrays.copyOf(yValues, nData);
      double[] yValuesDw = Arrays.copyOf(yValues, nData);
      final double[] xKeys = new double[10 * nData];
      final double xMin = xValues[0];
      final double xMax = xValues[nData - 1];
      for (int i = 0; i < 10 * nData; ++i) {
        xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
      }

      PiecewisePolynomialResultsWithSensitivity result = interps[k].interpolateWithSensitivity(xValues, yValues);
      for (int j = 0; j < nData; ++j) {
        yValuesUp[j] = yValues[j] * (1. + EPS);
        yValuesDw[j] = yValues[j] * (1. - EPS);
        final PiecewisePolynomialResultsWithSensitivity resultUp = interps[k].interpolateWithSensitivity(xValues, yValuesUp);
        final PiecewisePolynomialResultsWithSensitivity resultDw = interps[k].interpolateWithSensitivity(xValues, yValuesDw);

        final double[] valuesUp = FUNCTION.evaluate(resultUp, xKeys).rowArray(0);
        final double[] valuesDw = FUNCTION.evaluate(resultDw, xKeys).rowArray(0);
        final double[] diffUp = FUNCTION.differentiate(resultUp, xKeys).rowArray(0);
        final double[] diffDw = FUNCTION.differentiate(resultDw, xKeys).rowArray(0);
        for (int i = 0; i < 10 * nData; ++i) {
          final double xKeyUp = xKeys[i] * (1. + EPS);
          final double xKeyDw = xKeys[i] * (1. - EPS);
          double valueFinite = 0.5 * (valuesUp[i] - valuesDw[i]) / EPS / yValues[j];
          double senseFinite = 0.5 * (diffUp[i] - diffDw[i]) / EPS / yValues[j];
          final double resNodeSensitivity = FUNCTION.nodeSensitivity(result, xKeys[i]).get(j);
          final double resNodeSensitivityXkeyUp = FUNCTION.nodeSensitivity(result, xKeyUp).get(j);
          final double resNodeSensitivityXkeyDw = FUNCTION.nodeSensitivity(result, xKeyDw).get(j);
          final double senseFiniteXkey = 0.5 * (resNodeSensitivityXkeyUp - resNodeSensitivityXkeyDw) / EPS / xKeys[i];
          final double resDiffNodeSensitivity = FUNCTION.differentiateNodeSensitivity(result, xKeys[i]).get(j);
          assertEquals(valueFinite, resNodeSensitivity, Math.max(Math.abs(yValues[j]) * EPS, EPS));
          assertEquals(senseFinite, resDiffNodeSensitivity, Math.max(Math.abs(yValues[j]) * EPS, EPS));
          assertEquals(senseFiniteXkey, resDiffNodeSensitivity, Math.max(Math.abs(xKeys[i]) * EPS, EPS));
        }
        yValuesUp[j] = yValues[j];
        yValuesDw[j] = yValues[j];
      }
    }
  }

  /**
   * 
   */
  @Test
  public void secondDerivativeFiniteDifferenceTest() {
    final PiecewisePolynomialInterpolator[] interps = new PiecewisePolynomialInterpolator[] {new NaturalSplineInterpolator(), new CubicSplineInterpolator(),
      new PiecewiseCubicHermiteSplineInterpolator(), new ConstrainedCubicSplineInterpolator(), new SemiLocalCubicSplineInterpolator() };
    final int nInterps = interps.length;
    for (int k = 0; k < nInterps; ++k) {
      final double[] xValues = new double[] {1., 2.8, 3.1, 5.9, 10., 16. };
      final double[] yValues = new double[] {1., 2., 3., -2., 5., -5. };
      final int nData = xValues.length;
      double[] yValuesUp = Arrays.copyOf(yValues, nData);
      double[] yValuesDw = Arrays.copyOf(yValues, nData);
      final double[] xKeys = new double[10 * nData];
      final double xMin = xValues[0];
      final double xMax = xValues[nData - 1];
      for (int i = 0; i < 10 * nData; ++i) {
        xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
      }

      PiecewisePolynomialResultsWithSensitivity result = interps[k].interpolateWithSensitivity(xValues, yValues);
      for (int j = 0; j < nData; ++j) {
        yValuesUp[j] = yValues[j] * (1. + EPS);
        yValuesDw[j] = yValues[j] * (1. - EPS);
        final PiecewisePolynomialResultsWithSensitivity resultUp = interps[k].interpolateWithSensitivity(xValues, yValuesUp);
        final PiecewisePolynomialResultsWithSensitivity resultDw = interps[k].interpolateWithSensitivity(xValues, yValuesDw);

        final double[] diffUp = FUNCTION.differentiateTwice(resultUp, xKeys).toArray()[0];
        final double[] diffDw = FUNCTION.differentiateTwice(resultDw, xKeys).toArray()[0];
        for (int i = 0; i < 10 * nData; ++i) {
          final double xKeyUp = xKeys[i] * (1. + EPS);
          final double xKeyDw = xKeys[i] * (1. - EPS);

          double senseFinite = 0.5 * (diffUp[i] - diffDw[i]) / EPS / yValues[j];
          final double resdiffNodeSensitivityXkeyUp = FUNCTION.differentiateNodeSensitivity(result, xKeyUp).get(j);
          final double resdiffNodeSensitivityXkeyDw = FUNCTION.differentiateNodeSensitivity(result, xKeyDw).get(j);
          final double senseFiniteXkey = 0.5 * (resdiffNodeSensitivityXkeyUp - resdiffNodeSensitivityXkeyDw) / EPS / xKeys[i];
          final double resDiffTwiceNodeSensitivity = FUNCTION.differentiateTwiceNodeSensitivity(result, xKeys[i]).get(j);

          assertEquals(senseFinite, resDiffTwiceNodeSensitivity, Math.max(Math.abs(yValues[j]) * EPS, EPS));
          assertEquals(senseFiniteXkey, resDiffTwiceNodeSensitivity, Math.max(Math.abs(xKeys[i]) * EPS, EPS));
        }
        yValuesUp[j] = yValues[j];
        yValuesDw[j] = yValues[j];
      }
    }
  }

  /**
   * Interpolations with longer yValues
   */
  @Test
  public void clampedFiniteDifferenceTest() {
    final PiecewisePolynomialInterpolator[] interps = new PiecewisePolynomialInterpolator[] {new CubicSplineInterpolator() };
    final int nInterps = interps.length;
    for (int k = 0; k < nInterps; ++k) {
      final double[] xValues = new double[] {1., 2.8, 3.1, 5.9, 10., 16. };
      final double[] bcs = new double[] {-2., -1.5, 0., 1. / 3., 3.2 };
      final int nBcs = bcs.length;
      for (int l = 0; l < nBcs; ++l) {
        for (int m = 0; m < nBcs; ++m) {
          final double[] yValues = new double[] {bcs[l], 1., 2., 3., -2., 5., -5., bcs[m] };
          final int nData = xValues.length;
          double[] yValuesUp = Arrays.copyOf(yValues, nData + 2);
          double[] yValuesDw = Arrays.copyOf(yValues, nData + 2);
          final double[] xKeys = new double[10 * nData];
          final double xMin = xValues[0];
          final double xMax = xValues[nData - 1];
          for (int i = 0; i < 10 * nData; ++i) {
            xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
          }

          PiecewisePolynomialResultsWithSensitivity result = interps[k].interpolateWithSensitivity(xValues, yValues);
          for (int j = 0; j < nData; ++j) {
            yValuesUp[j + 1] = yValues[j + 1] * (1. + EPS);
            yValuesDw[j + 1] = yValues[j + 1] * (1. - EPS);
            final PiecewisePolynomialResultsWithSensitivity resultUp = interps[k].interpolateWithSensitivity(xValues, yValuesUp);
            final PiecewisePolynomialResultsWithSensitivity resultDw = interps[k].interpolateWithSensitivity(xValues, yValuesDw);

            final double[] valuesUp = FUNCTION.evaluate(resultUp, xKeys).toArray()[0];
            final double[] valuesDw = FUNCTION.evaluate(resultDw, xKeys).toArray()[0];
            final double[] diffUp = FUNCTION.differentiate(resultUp, xKeys).toArray()[0];
            final double[] diffDw = FUNCTION.differentiate(resultDw, xKeys).toArray()[0];
            for (int i = 0; i < 10 * nData; ++i) {
              final double xKeyUp = xKeys[i] * (1. + EPS);
              final double xKeyDw = xKeys[i] * (1. - EPS);
              double valueFinite = 0.5 * (valuesUp[i] - valuesDw[i]) / EPS / yValues[j + 1];
              double senseFinite = 0.5 * (diffUp[i] - diffDw[i]) / EPS / yValues[j + 1];
              final double resNodeSensitivity = FUNCTION.nodeSensitivity(result, xKeys[i]).get(j);
              final double resNodeSensitivityXkeyUp = FUNCTION.nodeSensitivity(result, xKeyUp).get(j);
              final double resNodeSensitivityXkeyDw = FUNCTION.nodeSensitivity(result, xKeyDw).get(j);
              final double senseFiniteXkey = 0.5 * (resNodeSensitivityXkeyUp - resNodeSensitivityXkeyDw) / EPS / xKeys[i];
              final double resDiffNodeSensitivity = FUNCTION.differentiateNodeSensitivity(result, xKeys[i]).get(j);
              assertEquals(valueFinite, resNodeSensitivity, Math.max(Math.abs(yValues[j + 1]) * EPS, EPS));
              assertEquals(senseFinite, resDiffNodeSensitivity, Math.max(Math.abs(yValues[j + 1]) * EPS, EPS));
              assertEquals(senseFiniteXkey, resDiffNodeSensitivity, Math.max(Math.abs(xKeys[i]) * EPS, EPS));
            }
            yValuesUp[j + 1] = yValues[j + 1];
            yValuesDw[j + 1] = yValues[j + 1];
          }
        }
      }
    }
  }

  /**
   * 
   */
  @Test
  public void clampedSecondDerivativeFiniteDifferenceTest() {
    final PiecewisePolynomialInterpolator[] interps = new PiecewisePolynomialInterpolator[] {new CubicSplineInterpolator() };
    final int nInterps = interps.length;
    for (int k = 0; k < nInterps; ++k) {
      final double[] xValues = new double[] {1., 2.8, 3.1, 5.9, 10., 16. };
      final double[] bcs = new double[] {-2., -1.5, 0., 1. / 3., 3.2 };
      final int nBcs = bcs.length;
      for (int l = 0; l < nBcs; ++l) {
        for (int m = 0; m < nBcs; ++m) {
          final double[] yValues = new double[] {bcs[l], 1., 2., 3., -2., 5., -5., bcs[m] };
          final int nData = xValues.length;
          double[] yValuesUp = Arrays.copyOf(yValues, nData + 2);
          double[] yValuesDw = Arrays.copyOf(yValues, nData + 2);
          final double[] xKeys = new double[10 * nData];
          final double xMin = xValues[0];
          final double xMax = xValues[nData - 1];
          for (int i = 0; i < 10 * nData; ++i) {
            xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
          }

          PiecewisePolynomialResultsWithSensitivity result = interps[k].interpolateWithSensitivity(xValues, yValues);
          for (int j = 0; j < nData; ++j) {
            yValuesUp[j + 1] = yValues[j + 1] * (1. + EPS);
            yValuesDw[j + 1] = yValues[j + 1] * (1. - EPS);
            final PiecewisePolynomialResultsWithSensitivity resultUp = interps[k].interpolateWithSensitivity(xValues, yValuesUp);
            final PiecewisePolynomialResultsWithSensitivity resultDw = interps[k].interpolateWithSensitivity(xValues, yValuesDw);

            final double[] diffUp = FUNCTION.differentiateTwice(resultUp, xKeys).toArray()[0];
            final double[] diffDw = FUNCTION.differentiateTwice(resultDw, xKeys).toArray()[0];
            for (int i = 0; i < 10 * nData; ++i) {
              final double xKeyUp = xKeys[i] * (1. + EPS);
              final double xKeyDw = xKeys[i] * (1. - EPS);

              double senseFinite = 0.5 * (diffUp[i] - diffDw[i]) / EPS / yValues[j + 1];
              final double resdiffNodeSensitivityXkeyUp = FUNCTION.differentiateNodeSensitivity(result, xKeyUp).get(j);
              final double resdiffNodeSensitivityXkeyDw = FUNCTION.differentiateNodeSensitivity(result, xKeyDw).get(j);
              final double senseFiniteXkey = 0.5 * (resdiffNodeSensitivityXkeyUp - resdiffNodeSensitivityXkeyDw) / EPS / xKeys[i];
              final double resDiffTwiceNodeSensitivity = FUNCTION.differentiateTwiceNodeSensitivity(result, xKeys[i]).get(j);

              assertEquals(senseFinite, resDiffTwiceNodeSensitivity, Math.max(Math.abs(yValues[j + 1]) * EPS, EPS));
              assertEquals(senseFiniteXkey, resDiffTwiceNodeSensitivity, Math.max(Math.abs(xKeys[i]) * EPS, EPS));
            }
            yValuesUp[j + 1] = yValues[j + 1];
            yValuesDw[j + 1] = yValues[j + 1];
          }
        }
      }
    }
  }
}
