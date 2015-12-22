/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test {@link LogNaturalDiscountFactorInterpolator1D}.
 */
@Test
public class LogNaturalDiscountFactorInterpolator1DTest {

  private static final Interpolator1D INTERPOLATOR = 
      Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_DISCOUNT_FACTOR);
  private static final double TOL = 1.0e-13;

  public void randomTest() {
    double[][] xValues = new double[][] {
      {0.09315068493150686, 0.17534246575342466, 0.25753424657534246, 0.5150684931506849, 0.7643835616438356,
        1.0136986301369864, 1.5123287671232877, 2.010958904109589, 3.0082191780821916, 4.008219178082192,
        5.010958904109589, 6.010958904109589, 7.016438356164383, 8.013698630136986, 9.013698630136986,
        10.013698630136986 },
      {0.25753424657534246, 0.5150684931506849, 0.7643835616438356, 1.0136986301369864, 2.010958904109589,
        3.0082191780821916, 4.008219178082192, 5.010958904109589, 7.016438356164383, 10.013698630136986,
        12.013698630136986, 15.016438356164384, 20.019178082191782, 25.024657534246575, 30.03013698630137 } };
    double[][] yValues = new double[][] {
      {0.9999322240708384, 0.999855017985639, 0.9997586104367652, 0.9995314376146883, 0.9991889583206024,
        0.9987863565266396, 0.9951443440339545, 0.9935417494457633, 0.9787684539091274, 0.9553160557129285,
        0.9253819365678392, 0.8924036278557023, 0.8584517020246731, 0.8242001356938264, 0.7903762268686396,
        0.7569695275371742 },
      {0.9993828278703681, 0.9987093787169503, 0.997962554825106, 0.9970335646656089, 0.9899680092892333,
        0.9721633595403785, 0.945971194540647, 0.9162728167539897, 0.8425206834333187, 0.7392320584400591,
        0.6714316872227905, 0.5847588628400905, 0.46828486349989557, 0.37936962851076067, 0.30961730831110595 } };
    double[] xKeys = new double[] {-0.01, 1.183889983084374, 0.2385908948332678, 0.9130960889984017,
      1.6751399625052708, 2.061475076285611, 3.8368242544942768, 4.18374791977202, 5.237315353617813,
      6.165363988178849, 7.292538274335414, 8.555769928347884, 9.556400741450425, 10.743446029721234,
      15.758174808226803, 20.354819113659708, 30.54773504143382, 35.665952152847833, 40.720941630811579 };
    int nKeys = xKeys.length;
    NaturalSplineInterpolator baseInterp = new NaturalSplineInterpolator();
    PiecewisePolynomialFunction1D func = new PiecewisePolynomialFunction1D();
    int yDim = yValues.length;
    double eps = 1.e-7;
    for (int k = 0; k < yDim; ++k) {
      int nData = xValues[k].length;
      /* Test interpolation and its first derivative */
      double[] xValuesAll = new double[nData + 1];
      double[] yValuesLogAll = new double[nData + 1];
      System.arraycopy(xValues[k], 0, xValuesAll, 1, nData);
      for (int i = 0; i < nData; ++i) {
        yValuesLogAll[i + 1] = Math.log(yValues[k][i]);
      }
      yValuesLogAll[0] = 0d;
      PiecewisePolynomialResult result = baseInterp.interpolate(xValuesAll, yValuesLogAll);
      Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xValues[k], yValues[k]);
      for (int i = 0; i < nKeys; ++i) {
        double computedValue = INTERPOLATOR.interpolate(bundle, xKeys[i]);
        double expectedValue = Math.exp(func.evaluate(result, xKeys[i]).get(0));
        assertEquals(computedValue, expectedValue, Math.abs(expectedValue) * TOL);
        double computedDerivative = INTERPOLATOR.firstDerivative(bundle, xKeys[i]);
        double expectedDerivative = 0.5 / eps *
            (INTERPOLATOR.interpolate(bundle, xKeys[i] + eps) - INTERPOLATOR.interpolate(bundle, xKeys[i] - eps));
        assertEquals(computedDerivative, expectedDerivative, Math.max(Math.abs(computedDerivative), 1d) * eps);
      }
      assertEquals(INTERPOLATOR.interpolate(bundle, 0d), 1d, TOL);
      /* Test sensitivity */
      for (int j = 0; j < nData; ++j) {
        double[] yValues1Up = Arrays.copyOf(yValues[k], nData);
        double[] yValues1Dw = Arrays.copyOf(yValues[k], nData);
        yValues1Up[j] = yValues[k][j] * (1. + eps);
        yValues1Dw[j] = yValues[k][j] * (1. - eps);
        Interpolator1DDataBundle dataBund1Up = INTERPOLATOR.getDataBundleFromSortedArrays(xValues[k], yValues1Up);
        Interpolator1DDataBundle dataBund1Dw = INTERPOLATOR.getDataBundleFromSortedArrays(xValues[k], yValues1Dw);
        for (int i = 0; i < nKeys; ++i) {
          double expected = 0.5 / eps / yValues[k][j] *
              (INTERPOLATOR.interpolate(dataBund1Up, xKeys[i]) - INTERPOLATOR.interpolate(dataBund1Dw, xKeys[i]));
          assertEquals(expected, INTERPOLATOR.getNodeSensitivitiesForValue(bundle, xKeys[i])[j],
              Math.max(Math.abs(expected), 1d) * eps * 10d);
        }
      }
    }
  }

  public void testFunctionalForm() {
    double[] xValues = new double[] {0.5, 1.0, 3.0, 5.0, 10.0, 30.0 };
    double coef0 = -0.065;
    double[] coefs = new double[] {-0.03, -0.035, -0.015, 0.025, 0d, 0d }; // the last element will be updated
    double tmp = 0d;
    int nData = xValues.length;
    for (int i = 0; i < nData - 1; ++i) {
      coefs[nData - 1] += coefs[i] * xValues[i];
      tmp += coefs[i];
    }
    coefs[nData - 1] *= -1d / xValues[nData - 1];
    tmp += coefs[nData - 1];
    final double coef = tmp;
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        int index = 0;
        double res = coef0 * t - coef * Math.pow(t, 3) / 6.0;
        while (index < nData && t > xValues[index]) {
          res += coefs[index] * Math.pow(t - xValues[index], 3) / 6.0;
          ++index;
        }
        return res;
      }
    };
    double[] yValues = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues[i] = Math.exp(func.apply(xValues[i]));
    }
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolator.of(
        Interpolator1DFactory.LOG_NATURAL_CUBIC_DISCOUNT_FACTOR,
        Interpolator1DFactory.INTERPOLATOR_EXTRAPOLATOR,
        Interpolator1DFactory.LOG_LINEAR_EXTRAPOLATOR);
    Interpolator1DDataBundle bundle = interp.getDataBundle(xValues, yValues);
    for (int i = 0; i < 1000; ++i) { // 0 <= x < 50
      double xSample = 0.05 * i;
      assertEquals(Math.exp(func.apply(xSample)), interp.interpolate(bundle, xSample), TOL);
    }
  }

  public void coverage() {
    coverImmutableBean( (LogNaturalDiscountFactorInterpolator1D)INTERPOLATOR);
  }

}
