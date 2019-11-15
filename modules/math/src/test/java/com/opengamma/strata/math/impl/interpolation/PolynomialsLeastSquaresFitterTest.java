/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;
import com.opengamma.strata.math.impl.regression.LeastSquaresRegressionResult;
import com.opengamma.strata.math.impl.statistics.descriptive.MeanCalculator;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleStandardDeviationCalculator;

/**
 * Test.
 */
public class PolynomialsLeastSquaresFitterTest {
  private static final double EPS = 1e-14;

  private final Function<double[], Double> _meanCal = new MeanCalculator();
  private final Function<double[], Double> _stdCal = new SampleStandardDeviationCalculator();

  /**
   * Checks coefficients of polynomial f(x) are recovered and residuals, { y_i -f(x_i) }, are accurate
   */
  @Test
  public void polynomialFunctionRecoverTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();
    final double[] coeff = new double[] {3.4, 5.6, 1., -4.};

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
      assertThat(coeff[i]).isCloseTo(coeffResult[i], offset(EPS * Math.abs(coeff[i])));
    }

    final double[] residuals = result.getResiduals();
    func = new RealPolynomialFunction1D(coeffResult);
    double[] yValuesFit = new double[nPts];
    for (int i = 0; i < nPts; ++i) {
      yValuesFit[i] = func.applyAsDouble(xValues[i]);
    }

    for (int i = 0; i < nPts; ++i) {
      assertThat(Math.abs(yValuesFit[i] - yValues[i])).isCloseTo(0., offset(Math.abs(yValues[i]) * EPS));
    }

    for (int i = 0; i < nPts; ++i) {
      assertThat(Math.abs(yValuesFit[i] - yValues[i])).isCloseTo(Math.abs(residuals[i]), offset(Math.abs(yValues[i]) * EPS));
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

    assertThat(nPts - (degree + 1)).isEqualTo(resultVer.getDof());
    for (int i = 0; i < degree + 1; ++i) {
      assertThat(coeff[i]).isCloseTo(coeffResult[i], offset(EPS * Math.abs(coeff[i])));
    }

    for (int i = 0; i < nPts; ++i) {
      assertThat(Math.abs(yValuesFit[i] - yValues[i])).isCloseTo(0., offset(Math.abs(yValues[i]) * EPS));
    }

    assertThat(sum).isCloseTo(resultVer.getDiffNorm(), offset(EPS));

    /**
     * Tests for regressVerbose(.., true)
     */

    PolynomialsLeastSquaresFitterResult resultNorm = regObj.regressVerbose(xValues, yValuesNorm, degree, true);

    coeffResult = resultNorm.getCoeff();
    final double[] meanAndStd = resultNorm.getMeanAndStd();

    assertThat(nPts - (degree + 1)).isEqualTo(resultNorm.getDof());
    assertThat(mean).isCloseTo(meanAndStd[0], offset(EPS));
    assertThat(std).isCloseTo(meanAndStd[1], offset(EPS));
    for (int i = 0; i < degree + 1; ++i) {
      assertThat(coeff[i]).isCloseTo(coeffResult[i], offset(EPS * Math.abs(coeff[i])));
    }

    func = new RealPolynomialFunction1D(coeffResult);
    for (int i = 0; i < nPts; ++i) {
      final double tmp = xValues[i] / std - ratio;
      yValuesFit[i] = func.applyAsDouble(tmp);
    }

    for (int i = 0; i < nPts; ++i) {
      assertThat(Math.abs(yValuesFit[i] - yValuesNorm[i])).isCloseTo(0., offset(Math.abs(yValuesNorm[i]) * EPS));
    }

    sum = 0.;
    for (int i = 0; i < nPts; ++i) {
      sum += (yValuesFit[i] - yValuesNorm[i]) * (yValuesFit[i] - yValuesNorm[i]);
    }
    sum = Math.sqrt(sum);

    assertThat(sum).isCloseTo(resultNorm.getDiffNorm(), offset(EPS));

  }

  /**
   * 
   */
  @Test
  public void rmatrixTest() {

    final PolynomialsLeastSquaresFitter regObj1 = new PolynomialsLeastSquaresFitter();
    final double[] xValues = new double[] {-1., 0, 1.};
    final double[] yValues = new double[] {1., 0, 1.};
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
        assertThat(rMatrix[i][j]).isCloseTo(rMatResult.get(i, j), offset(EPS));
      }
    }

    final PolynomialsLeastSquaresFitter regObj2 = new PolynomialsLeastSquaresFitter();
    PolynomialsLeastSquaresFitterResult resultNorm = regObj2.regressVerbose(xValues, yValues, degree, true);
    rMatResult = resultNorm.getRMat();

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        assertThat(rMatrix[i][j]).isCloseTo(rMatResult.get(i, j), offset(EPS));
      }
    }

  }

  /**
   * An error is thrown if rescaling of xValues is NOT used and we try to access data, mean and standard deviation 
   */
  @Test
  public void normalisationErrorTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    PolynomialsLeastSquaresFitterResult result = regObj.regressVerbose(xValues, yValues, degree, false);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> result.getMeanAndStd());

  }

  /**
   * Number of data points should be larger than (degree + 1) of a polynomial
   */
  @Test
  public void dataShortTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void dataShortVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void dataShortVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * Degree of polynomial must be positive 
   */
  @Test
  public void minusDegreeTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void minusDegreeVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void minusDegreeVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = -4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * xValues length should be the same as yValues length
   */
  @Test
  public void wrongDataLengthTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void wrongDataLengthVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void wrongDataLengthVerboseTureTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 5, 6};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 1, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * An error is thrown if too many repeated data are found
   */
  @Test
  public void repeatDataTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void repeatDataVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void repeatDataVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1, 2, 3, 1, 1};
    final double[] yValues = new double[] {1, 2, 3, 4, 2, 2};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * 
   */
  @Test
  public void extremeValueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e18, 4};
    final double[] yValues = new double[] {1, 2, 3, 4, 5};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void extremeValueVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e18, 4};
    final double[] yValues = new double[] {1, 2, 3, 4, 5};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void extremeValueVerboseTrueAlphaTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final double[] xValues = new double[] {0, 1e-307, 2e-307, 3e-307, 4};
    final double[] yValues = new double[] {1, 2, 3, 4, 5};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * 
   */
  @Test
  public void nullTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();
    final int degree = 4;
    double[] xValues = null;
    double[] yValues = null;

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void nullVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();
    final int degree = 4;
    double[] xValues = null;
    double[] yValues = null;

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void nullVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();
    final int degree = 4;
    double[] xValues = null;
    double[] yValues = null;

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * 
   */
  @Test
  public void infinityTest() {

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

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void infinityVerboseFalseTest() {

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

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void infinityVerboseTrueTest() {

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

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * 
   */
  @Test
  public void naNTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void naNVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void naNVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    final int nPts = 5;
    double[] xValues = new double[nPts];
    double[] yValues = new double[nPts];

    for (int i = 0; i < nPts; ++i) {
      xValues[i] = Double.NaN;
      yValues[i] = Double.NaN;
    }

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

  /**
   * 
   */
  @Test
  public void largeNumberTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    double[] xValues = new double[] {1, 2, 3, 4e2, 5, 6, 7};
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regress(xValues, yValues, degree));

  }

  /**
   * 
   */
  @Test
  public void largeNumberVerboseFalseTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 4;

    double[] xValues = new double[] {1, 2, 3, 4e2, 5, 6, 7};
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, false));

  }

  /**
   * 
   */
  @Test
  public void largeNumberVerboseTrueTest() {

    final PolynomialsLeastSquaresFitter regObj = new PolynomialsLeastSquaresFitter();

    final int degree = 6;

    double[] xValues = new double[] {1, 2, 3, 4e17, 5, 6, 7};
    double[] yValues = new double[] {1, 2, 3, 4, 5, 6, 7};

    assertThatIllegalArgumentException()
        .isThrownBy(() -> regObj.regressVerbose(xValues, yValues, degree, true));

  }

}
