/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;
import com.opengamma.strata.math.impl.regression.LeastSquaresRegressionResult;
import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleStandardDeviationCalculator;

/**
 * Test.
 */
@Test
public class PolynomialsLeastSquaresFitterTest {
  private static final double EPS = 1e-14;

  private final Function<double[], Double> _meanCal = new MeanCalculator();
  private final Function<double[], Double> _stdCal = new SampleStandardDeviationCalculator();

  /**
   * Checks coefficients of polynomial f(x) are recovered and residuals, { y_i -f(x_i) }, are accurate
   */
  public void PolynomialFunctionRecoverTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();
    final double[] coeff = new double[] {3.4, 5.6, 1., -4. };

    DoubleFunction1D func = new RealPolynomialFunction1D(coeff);

    final int degree = coeff.length - 1;

    final int nPts = 7;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = -5. + 10 * i / (nPts - 1);
      yValues[i] = func.applyAsDouble(xValues[i]);
    }

    double[] yValuesNorm = new double[nPts];

    final double mean = _meanCal.apply(xValues);
    final double std = _stdCal.apply(xValues);
    final double ratio = mean / std;

    for (int i = 0; i < nPts; ++i) {
      final double tmp = xValues[i] / std - ratio;
      yValuesNorm[i] = func.applyAsDouble(tmp);
    }

    /**
     * Tests for regress(..)
     */

    LeastSquaresRegressionResult result = regObj.regress(xValues, yValues, degree);

    double[] coeffResult = result.getBetas();

    for (int i = 0; i < degree + 1; ++i) {
      assertEquals(coeff[i], coeffResult[i], EPS * Math.abs(coeff[i]));
    }

    final double[] residuals = result.getResiduals();
    func = new RealPolynomialFunction1D(coeffResult);
    double[] yValuesFit = new double[nPts];
    for (int i = 0; i < nPts; ++i) {
      yValuesFit[i] = func.applyAsDouble(xValues[i]);
    }

    for (int i = 0; i < nPts; ++i) {
      assertEquals(Math.abs(yValuesFit[i] - yValues[i]), 0., Math.abs(yValues[i]) * EPS);
    }

    for (int i = 0; i < nPts; ++i) {
      assertEquals(Math.abs(yValuesFit[i] - yValues[i]), Math.abs(residuals[i]), Math.abs(yValues[i]) * EPS);
    }

    double sum = 0.;
    for (int i = 0; i < nPts; ++i) {
      sum += residuals[i] * residuals[i];
    }
    sum = Math.sqrt(sum);

    /**
     * Tests for regressVerbose(.., false)
     */

    PolynomialsLeastSquaresFitterResult resultVer = regObj.regressVerbose(xValues, yValues, degree, false);
    coeffResult = resultVer.getCoeff();
    func = new RealPolynomialFunction1D(coeffResult);
    for (int i = 0; i < nPts; ++i) {
      yValuesFit[i] = func.applyAsDouble(xValues[i]);
    }

    assertEquals(nPts - (degree + 1), resultVer.getDof(), 0);
    for (int i = 0; i < degree + 1; ++i) {
      assertEquals(coeff[i], coeffResult[i], EPS * Math.abs(coeff[i]));
    }

    for (int i = 0; i < nPts; ++i) {
      assertEquals(Math.abs(yValuesFit[i] - yValues[i]), 0., Math.abs(yValues[i]) * EPS);
    }

    assertEquals(sum, resultVer.getDiffNorm(), EPS);

    /**
     * Tests for regressVerbose(.., true)
     */

    PolynomialsLeastSquaresFitterResult resultNorm = regObj.regressVerbose(xValues, yValuesNorm, degree, true);

    coeffResult = resultNorm.getCoeff();
    final double[] meanAndStd = resultNorm.getMeanAndStd();

    assertEquals(nPts - (degree + 1), resultNorm.getDof(), 0);
    assertEquals(mean, meanAndStd[0], EPS);
    assertEquals(std, meanAndStd[1], EPS);
    for (int i = 0; i < degree + 1; ++i) {
      assertEquals(coeff[i], coeffResult[i], EPS * Math.abs(coeff[i]));
    }

    func = new RealPolynomialFunction1D(coeffResult);
    for (int i = 0; i < nPts; ++i) {
      final double tmp = xValues[i] / std - ratio;
      yValuesFit[i] = func.applyAsDouble(tmp);
    }

    for (int i = 0; i < nPts; ++i) {
      assertEquals(Math.abs(yValuesFit[i] - yValuesNorm[i]), 0., Math.abs(yValuesNorm[i]) * EPS);
    }

    sum = 0.;
    for (int i = 0; i < nPts; ++i) {
      sum += (yValuesFit[i] - yValuesNorm[i]) * (yValuesFit[i] - yValuesNorm[i]);
    }
    sum = Math.sqrt(sum);

    assertEquals(sum, resultNorm.getDiffNorm(), EPS);

  }

  /**
   * 
   */
  public void RmatrixTest() {

    final PolynomialsLeastSquaresFitter regObj1 = new PolynomialsLeastSquaresFitter();
    final double[] xValues = new double[] {-1., 0, 1. };
    final double[] yValues = new double[] {1., 0, 1. };
    final double[][] rMatrix = new double[][] {
        {-Math.sqrt(3.), 0., -2. / Math.sqrt(3.)},
        {0., -Math.sqrt(2.), 0.},
        {0., 0., -Math.sqrt(2. / 3.)}
    };

    final int degree = 2;

    PolynomialsLeastSquaresFitterResult resultVer = regObj1.regressVerbose(xValues, yValues, degree, false);
    DoubleMatrix rMatResult = resultVer.getRMat();

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        assertEquals(rMatrix[i][j], rMatResult.get(i, j), EPS);
      }
    }

    final PolynomialsLeastSquaresFitter regObj2 = new PolynomialsLeastSquaresFitter();
    PolynomialsLeastSquaresFitterResult resultNorm = regObj2.regressVerbose(xValues, yValues, degree, true);
    rMatResult = resultNorm.getRMat();

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        assertEquals(rMatrix[i][j], rMatResult.get(i, j), EPS);
      }
    }

  }

  /**
   * An error is thrown if rescaling of xValues is NOT used and we try to access data, mean and standard deviation 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NormalisationErrorTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    PolynomialsLeastSquaresFitterResult result = regObj.regressVerbose(xValues, yValues, degree, false);
    result.getMeanAndStd();

  }

  /**
   * Number of data points should be larger than (degree + 1) of a polynomial
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void DataShortVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * Degree of polynomial must be positive 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void MinusDegreeTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void MinusDegreeVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void MinusDegreeVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * xValues length should be the same as yValues length
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void WrongDataLengthTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void WrongDataLengthVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void WrongDataLengthVerboseTureTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * An error is thrown if too many repeated data are found
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void RepeatDataTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void RepeatDataVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void RepeatDataVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1 };
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ExtremeValueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e18, 4 };
    final double[] yValues = new double[] {1, 2, 3, 4, 5 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ExtremeValueVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e18, 4 };
    final double[] yValues = new double[] {1, 2, 3, 4, 5 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void ExtremeValueVerboseTrueAlphaTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e-307, 4 };
    final double[] yValues = new double[] {1, 2, 3, 4, 5 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    xValues = null;
    yValues = null;

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    xValues = null;
    yValues = null;

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NullVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    xValues = null;
    yValues = null;

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = 1. / zero;
      yValues[i] = 1. / zero;
    }

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = 1. / zero;
      yValues[i] = 1. / zero;
    }

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void InfinityVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    final double zero = 0.;

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = 1. / zero;
      yValues[i] = 1. / zero;
    }

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeNumberTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    double[] xValues = new double[] {1, 2, 3, 4e2, 5, 6, 7 };
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7 };

    regObj.regress(xValues, yValues, degree);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeNumberVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    double[] xValues = new double[] {1, 2, 3, 4e2, 5, 6, 7 };
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7 };

    regObj.regressVerbose(xValues, yValues, degree, false);

  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void LargeNumberVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    double[] xValues = new double[] {1, 2, 3, 4e17, 5, 6, 7 };
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7 };

    regObj.regressVerbose(xValues, yValues, degree, true);

  }

}
