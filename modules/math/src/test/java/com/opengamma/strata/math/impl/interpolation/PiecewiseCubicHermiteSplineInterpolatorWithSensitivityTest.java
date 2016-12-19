/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Test.
 */
@Test
public class PiecewiseCubicHermiteSplineInterpolatorWithSensitivityTest {

  private final static MatrixAlgebra MA = new OGMatrixAlgebra();
  private final static PiecewiseCubicHermiteSplineInterpolator PCHIP = new PiecewiseCubicHermiteSplineInterpolator();
  private final static PiecewiseCubicHermiteSplineInterpolatorWithSensitivity PCHIP_S = new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
  private final static PiecewisePolynomialFunction1D PPVAL = new PiecewisePolynomialFunction1D();
  private final static PiecewisePolynomialWithSensitivityFunction1D PPVAL_S = new PiecewisePolynomialWithSensitivityFunction1D();
  private final static double[] X = new double[] {0, 0.4000, 1.0000, 2.0000, 3.0000, 3.25, 5.0000 };
  private final static double[][] Y = new double[][] { {1.2200, 1.0, 0.9, 1.1, 1.2000, 1.3, 1.2000 }, // no flat sections
    {0.2200, 1.12, 1.5, 1.5, 1.7000, 1.8, 1.9000 }, // flat middle section
    {1.2200, 1.12, 1.5, 1.5, 1.5000, 1.8, 1.9000 }, // extended flat middle section
    {1.0, 1.0, 0.9, 1.1, 1.2000, 1.3, 1.3000 }, // flat ends
    {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 } };

  private final static double[] XX;
  private final static double[][] YY;

  static {
    int nSamples = 66;
    XX = new double[nSamples];
    for (int i = 0; i < nSamples; i++) {
      XX[i] = -0.5 + 0.1 * i;
    }
    // number from external PCHIP
    double[] ones = new double[nSamples];
    Arrays.fill(ones, 1.0);
    YY = new double[][] {
      {1.4827280948553, 1.45755198285101, 1.41376386655948, 1.35629099678456, 1.29006062433011, 1.22, 1.15103637459807, 1.08809699892818, 1.03610912379421, 1, 0.97417083482196, 0.950494224127665,
        0.930104501607717, 0.914136000952721, 0.903723055853281, 0.9, 0.9044, 0.916533333333333, 0.9348, 0.9576, 0.983333333333333, 1.0104, 1.0372, 1.06213333333333, 1.0836, 1.1,
        1.11196363636363, 1.12164848484848, 1.12974545454545, 1.13694545454545, 1.14393939393939, 1.15141818181818, 1.16007272727272, 1.17059393939393, 1.18367272727272, 1.2, 1.24174545454545,
        1.29105454545454, 1.29999766763848, 1.29993702623906, 1.29970845481049, 1.2992, 1.29829970845481, 1.29689562682215, 1.29487580174927, 1.29212827988338, 1.28854110787172, 1.28400233236151,
        1.2784, 1.2716221574344, 1.26355685131195, 1.25409212827988, 1.24311603498542, 1.2305166180758, 1.21618192419825, 1.2, 1.18185889212828, 1.16164664723032, 1.13925131195335,
        1.1145609329446, 1.08746355685131, 1.0578472303207, 1.0256, 0.990609912536443, 0.952765014577259, 0.911953352769679 },
      {-0.821780174139311, -0.736123832399252, -0.567498448759007, -0.33752321857486, -0.067817337203096, 0.22, 0.504309597678143, 0.763492260475047, 0.975928793034428, 1.12, 1.21945519378465,
        1.30979153693325, 1.38701160928743, 1.44711799068884, 1.48611326097915, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.50302857142857, 1.51165714285714, 1.5252,
        1.54297142857142, 1.56428571428571, 1.58845714285714, 1.6148, 1.64262857142857, 1.67125714285714, 1.7, 1.74253186813186, 1.78794725274725, 1.80604745458623, 1.81751047320026,
        1.82814532406369, 1.83796923076923, 1.84699941690962, 1.85525310607759, 1.86274752186588, 1.86949988786723, 1.87552742767436, 1.88084736488001, 1.88547692307692, 1.88943332585781,
        1.89273379681542, 1.89539555954249, 1.89743583763175, 1.89887185467593, 1.89972083426777, 1.9, 1.89972657546535, 1.89891778425656, 1.89759084996636, 1.89576299618748, 1.89345144651267,
        1.89067342453464, 1.88744615384615, 1.88378685803991, 1.87971276070867, 1.87524108544516 },
      {1.8878125, 1.68533333333333, 1.5211875, 1.3915, 1.29239583333333, 1.22, 1.1704375, 1.13983333333333, 1.1243125, 1.12, 1.14814814814814, 1.21851851851851, 1.31, 1.40148148148148,
        1.47185185185185, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.60221176470588, 1.76428235294117, 1.80690145772594,
        1.81977993483107, 1.83146973074944, 1.84202352941176, 1.85149401474875, 1.85993387069113, 1.86739578116961, 1.8739324301149, 1.87959650145772, 1.88444067912879, 1.88851764705882,
        1.89188008917852, 1.89458068941862, 1.89667213170982, 1.89820709998285, 1.89923827816841, 1.89981835019722, 1.9, 1.89983591150746, 1.89937876865031, 1.89868125535928, 1.89779605556508,
        1.89677585319842, 1.89567333219001, 1.89454117647058, 1.89343206997084, 1.8923986966215, 1.89149374035328 },
      {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.992592592592593, 0.974074074074074, 0.95, 0.925925925925926, 0.907407407407407, 0.9, 0.9044, 0.916533333333333, 0.9348, 0.9576, 0.983333333333333, 1.0104,
        1.0372, 1.06213333333333, 1.0836, 1.1, 1.11196363636363, 1.12164848484848, 1.12974545454545, 1.13694545454545, 1.14393939393939, 1.15141818181818, 1.16007272727272, 1.17059393939393,
        1.18367272727272, 1.2, 1.24174545454545, 1.29105454545454, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3, 1.3,
        1.3, 1.3, 1.3 }, ones };

  }

  public void baseInterpolationTest() {
    final int nExamples = Y.length;
    final int n = XX.length;

    for (int example = 0; example < nExamples; example++) {
      PiecewisePolynomialResult pp = PCHIP.interpolate(X, Y[example]);
      for (int i = 0; i < n; i++) {
        final double y = PPVAL.evaluate(pp, XX[i]).get(0);
        assertEquals(YY[example][i], y, 1e-14);
      }
    }
  }

  public void interpolationTest() {
    final int nExamples = Y.length;
    final int n = XX.length;

    for (int example = 0; example < nExamples; example++) {
      PiecewisePolynomialResult pp = PCHIP_S.interpolateWithSensitivity(X, Y[example]);
      for (int i = 0; i < n; i++) {
        final double y = PPVAL_S.evaluate(pp, XX[i]).get(0);

        assertEquals("example: " + example + ", index:" + i, YY[example][i], y, 1e-14);
      }
    }
  }

  public void sensitivityTest() {
    final int nExamples = Y.length;
    final int n = XX.length;
    final int nData = X.length;

    for (int example = 0; example < nExamples; example++) {
      PiecewisePolynomialResultsWithSensitivity pp = PCHIP_S.interpolateWithSensitivity(X, Y[example]);

      DoubleArray[] fdRes = fdSenseCal(X, Y[example], XX);

      for (int i = 0; i < n; i++) {
        DoubleArray res = PPVAL_S.nodeSensitivity(pp, XX[i]);
        for (int j = 0; j < nData; j++) {
          assertEquals("example: " + example + ", sample: " + i + ", node: " + j, fdRes[j].get(i), res.get(j), 1e-4);
        }
      }
    }
  }

  public void sensitivityTwoNodeTest() {
    int n = XX.length;
    double[] xValues = new double[] {-0.2, 3.63};
    double[] yValues = new double[] {4.67, -1.22};
    PiecewisePolynomialResultsWithSensitivity pp = PCHIP_S.interpolateWithSensitivity(xValues, yValues);
    DoubleArray[] fdRes = fdSenseCal(xValues, yValues, XX);
    for (int i = 0; i < n; i++) {
      DoubleArray res = PPVAL_S.nodeSensitivity(pp, XX[i]);
      for (int j = 0; j < 2; j++) {
        assertEquals(fdRes[j].get(i), res.get(j), 1e-4);
      }
    }
  }

  private DoubleArray[] fdSenseCal(double[] xValues, double[] yValues, double[] xx) {
    int nData = yValues.length;
    double eps = 1e-6;
    double scale = 0.5 / eps;
    DoubleArray[] res = new DoubleArray[nData];
    double[] temp = new double[nData];
    PiecewisePolynomialResult pp;
    for (int i = 0; i < nData; i++) {
      System.arraycopy(yValues, 0, temp, 0, nData);
      temp[i] += eps;
      pp = PCHIP.interpolate(xValues, temp);
      DoubleArray yUp = PPVAL.evaluate(pp, xx).row(0);
      temp[i] -= 2 * eps;
      pp = PCHIP.interpolate(xValues, temp);
      DoubleArray yDown = PPVAL.evaluate(pp, xx).row(0);
      res[i] = (DoubleArray) MA.scale(MA.subtract(yUp, yDown), scale);
    }
    return res;
  }

}
