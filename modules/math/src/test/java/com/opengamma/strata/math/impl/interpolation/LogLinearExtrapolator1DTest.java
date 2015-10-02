/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class LogLinearExtrapolator1DTest {

  private static final double EPS = 1.e-7;
  private static final double TOL = 1.e-12;

  /**
   * 
   */
  @Test
  public void sameIntervalsTest() {

    final double[] xValues = new double[] {-1., 0., 1., 2., 3., 4., 5., 6., 7., 8. };
    final double[][] yValues = new double[][] { {1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001 }, {11., 11., 8., 5., 1.001, 1.001, 5., 8., 11., 11. },
      {1.001, 1.001, 5., 8., 9., 9., 11., 12., 18., 18. } };
    final int nData = xValues.length;
    final int nKeys = 100 * nData;
    final double[] xKeys = new double[nKeys];
    final double xMin = xValues[0] - 2.2;
    final double xMax = xValues[nData - 1] + 2.;
    final double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }

    final Interpolator1D interp = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Extrapolator1D extrap = new LogLinearExtrapolator1D();
    final CombinedInterpolatorExtrapolator combined1 = new CombinedInterpolatorExtrapolator(interp, extrap, extrap);
    final CombinedInterpolatorExtrapolator combined2 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE,
        Interpolator1DFactory.LOG_LINEAR_EXTRAPOLATOR, Interpolator1DFactory.LOG_LINEAR_EXTRAPOLATOR);

    final int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      final Interpolator1DDataBundle bundle1 = combined1.getDataBundle(xValues, yValues[k]);
      final Interpolator1DDataBundle bundle2 = combined2.getDataBundle(xValues, yValues[k]);

      final double firstStart = combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]);
      final double firstEnd = combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]);
      for (int i = 0; i < nKeys; ++i) {
        /*
         * Check consistency between StrinName and Interpolator1D
         */
        final double res1 = combined1.interpolate(bundle1, xKeys[i]);
        final double res2 = combined2.interpolate(bundle2, xKeys[i]);
        assertEquals(res1, res2);
        /*
         * Check log-linearity 
         */

        if (xKeys[i] <= xValues[0]) {
          assertEquals(firstStart, combined1.firstDerivative(bundle1, xKeys[i]) / combined1.interpolate(bundle1, xKeys[i]), TOL);
        } else {
          if (xKeys[i] >= xValues[nData - 1]) {
            assertEquals(firstEnd, combined1.firstDerivative(bundle1, xKeys[i]) / combined1.interpolate(bundle1, xKeys[i]), TOL);

          }
        }
      }

      final Interpolator1DDataBundle bundleInterp = interp.getDataBundle(xValues, yValues[k]);

      /*
       * Check C0 continuity using interpolator/extrapolator
       */
      assertEquals(interp.interpolate(bundleInterp, xValues[nData - 1]), combined1.interpolate(bundle1, xValues[nData - 1]));
      assertEquals(extrap.extrapolate(bundleInterp, xValues[nData - 1] + 1.e-14, interp), combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(interp.interpolate(bundleInterp, xValues[0]), combined1.interpolate(bundle1, xValues[0]));
      assertEquals(extrap.extrapolate(bundleInterp, xValues[0] - 1.e-14, interp), combined1.interpolate(bundle1, xValues[0]), TOL);

      /*
       * Check C1 continuity using interpolator/extrapolator
       */
      assertEquals(interp.firstDerivative(bundleInterp, xValues[nData - 1]) / interp.interpolate(bundleInterp, xValues[nData - 1]),
          combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(extrap.firstDerivative(bundleInterp, xValues[nData - 1] + TOL, interp) / extrap.extrapolate(
          bundleInterp,
          xValues[nData - 1] + TOL,
          interp),
          combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(interp.firstDerivative(bundleInterp, xValues[0]) / interp.interpolate(bundleInterp, xValues[0]),
          combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]), TOL);
      assertEquals(extrap.firstDerivative(bundleInterp, xValues[0] - TOL, interp) / extrap.extrapolate(
          bundleInterp,
          xValues[0] - TOL,
          interp),
          combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]), TOL);

      /*
       * Test sensitivity
       */
      final double[] yValues1Up = new double[nData];
      final double[] yValues1Dw = new double[nData];
      for (int i = 0; i < nData; ++i) {
        yValues1Up[i] = yValues[k][i];
        yValues1Dw[i] = yValues[k][i];
      }
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k][j] * (1. + EPS);
        yValues1Dw[j] = yValues[k][j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = combined1.getDataBundleFromSortedArrays(xValues, yValues1Up);
        Interpolator1DDataBundle dataBund1Dw = combined1.getDataBundleFromSortedArrays(xValues, yValues1Dw);
        for (int i = 0; i < nKeys; ++i) {
          double res1 = 0.5 * (combined1.interpolate(dataBund1Up, xKeys[i]) - combined1.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[k][j];
          assertEquals(res1, combined1.getNodeSensitivitiesForValue(bundle1, xKeys[i])[j], Math.max(Math.abs(yValues[k][j]) * EPS, EPS) * 1.e2);//because gradient is NOT exact
          assertEquals(combined1.getNodeSensitivitiesForValue(bundle1, xKeys[i])[j], combined2.getNodeSensitivitiesForValue(bundle2, xKeys[i])[j]);
        }
        yValues1Up[j] = yValues[k][j];
        yValues1Dw[j] = yValues[k][j];
      }
    }
  }

  /**
   * 
   */
  @Test
  public void differentIntervalsTest() {
    final double[] xValues = new double[] {1.0328724558967068, 1.2692381049172323, 2.8611430465380905, 4.296118458251132, 7.011992052151352, 7.293354144919639, 8.557971037612713, 8.77306861567384,
      10.572470371584489, 12.96945799507056 };
    final double[][] yValues = new double[][] {
      {1.1593075755231343, 2.794957672828094, 4.674733634811079, 5.517689918508841, 6.138447304104604, 6.264375977142906, 6.581666492568779, 8.378685055774037,
        10.005246918325483, 10.468304334744241 },
      {9.95780079114617, 8.733013195721913, 8.192165283188197, 6.539369493529048, 6.3868683960757515, 4.700471352238411, 4.555354921077598, 3.780781869340659, 2.299369456202763, 0.9182441378327986 } };
    final int nData = xValues.length;
    final int nKeys = 100 * nData;
    final double[] xKeys = new double[nKeys];
    final double xMin = -xValues[0];
    final double xMax = xValues[nData - 1] + 2.;
    final double step = (xMax - xMin) / nKeys;
    for (int i = 0; i < nKeys; ++i) {
      xKeys[i] = xMin + step * i;
    }

    final Interpolator1D interp = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Extrapolator1D extrap = new LogLinearExtrapolator1D(1.e-8);
    final CombinedInterpolatorExtrapolator combined1 = new CombinedInterpolatorExtrapolator(interp, extrap, extrap);
    final CombinedInterpolatorExtrapolator combined2 = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE,
        Interpolator1DFactory.LOG_LINEAR_EXTRAPOLATOR, Interpolator1DFactory.LOG_LINEAR_EXTRAPOLATOR);

    final int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      final Interpolator1DDataBundle bundle1 = combined1.getDataBundle(xValues, yValues[k]);
      final Interpolator1DDataBundle bundle2 = combined2.getDataBundle(xValues, yValues[k]);

      final double firstStart = combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]);
      final double firstEnd = combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]);
      for (int i = 0; i < nKeys; ++i) {
        /*
         * Check consistency between StrinName and Interpolator1D
         */
        final double res1 = combined1.interpolate(bundle1, xKeys[i]);
        final double res2 = combined2.interpolate(bundle2, xKeys[i]);
        assertEquals(res1, res2);
        /*
         * Check log-linearity 
         */

        if (xKeys[i] <= xValues[0]) {
          assertEquals(firstStart, combined1.firstDerivative(bundle1, xKeys[i]) / combined1.interpolate(bundle1, xKeys[i]), TOL);
        } else {
          if (xKeys[i] >= xValues[nData - 1]) {
            assertEquals(firstEnd, combined1.firstDerivative(bundle1, xKeys[i]) / combined1.interpolate(bundle1, xKeys[i]), TOL);

          }
        }
      }

      final Interpolator1DDataBundle bundleInterp = interp.getDataBundle(xValues, yValues[k]);
      final Interpolator1DDataBundle bundleExtrap = interp.getDataBundleFromSortedArrays(xValues, yValues[k]);

      /*
       * Check C0 continuity using interpolator/extrapolator
       */
      assertEquals(interp.interpolate(bundleInterp, xValues[nData - 1]), combined1.interpolate(bundle1, xValues[nData - 1]));
      assertEquals(extrap.extrapolate(bundleExtrap, xValues[nData - 1] + 1.e-14, interp), combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(interp.interpolate(bundleInterp, xValues[0]), combined1.interpolate(bundle1, xValues[0]));
      assertEquals(extrap.extrapolate(bundleExtrap, xValues[0] - 1.e-14, interp), combined1.interpolate(bundle1, xValues[0]), TOL);

      /*
       * Check C1 continuity using interpolator/extrapolator
       */
      assertEquals(interp.firstDerivative(bundleInterp, xValues[nData - 1]) / interp.interpolate(bundleInterp, xValues[nData - 1]),
          combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(extrap.firstDerivative(bundleExtrap, xValues[nData - 1] + TOL, interp) / extrap.extrapolate(
          bundleExtrap,
          xValues[nData - 1] + TOL,
          interp),
          combined1.firstDerivative(bundle1, xValues[nData - 1]) / combined1.interpolate(bundle1, xValues[nData - 1]), TOL);
      assertEquals(interp.firstDerivative(bundleInterp, xValues[0]) / interp.interpolate(bundleInterp, xValues[0]),
          combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]), TOL);
      assertEquals(extrap.firstDerivative(bundleExtrap, xValues[0] - TOL, interp) / extrap.extrapolate(
          bundleExtrap,
          xValues[0] - TOL,
          interp),
          combined1.firstDerivative(bundle1, xValues[0]) / combined1.interpolate(bundle1, xValues[0]), TOL);

      /*
       * Test sensitivity
       */
      final double[] yValues1Up = new double[nData];
      final double[] yValues1Dw = new double[nData];
      for (int i = 0; i < nData; ++i) {
        yValues1Up[i] = yValues[k][i];
        yValues1Dw[i] = yValues[k][i];
      }
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k][j] * (1. + EPS);
        yValues1Dw[j] = yValues[k][j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = combined1.getDataBundleFromSortedArrays(xValues, yValues1Up);
        Interpolator1DDataBundle dataBund1Dw = combined1.getDataBundleFromSortedArrays(xValues, yValues1Dw);
        for (int i = 0; i < nKeys; ++i) {
          double res1 = 0.5 * (combined1.interpolate(dataBund1Up, xKeys[i]) - combined1.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[k][j];
          assertEquals(res1, combined1.getNodeSensitivitiesForValue(bundle1, xKeys[i])[j], Math.max(Math.abs(yValues[k][j]) * EPS, EPS) * 1.e2);//because gradient is NOT exact
          assertEquals(combined1.getNodeSensitivitiesForValue(bundle1, xKeys[i])[j], combined2.getNodeSensitivitiesForValue(bundle2, xKeys[i])[j]);
        }
        yValues1Up[j] = yValues[k][j];
        yValues1Dw[j] = yValues[k][j];
      }
    }
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withinRangeInterpolateTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {11., 8., 5., 1.001, 1.001, 5., 8., 11. };

    final Interpolator1D interp = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Extrapolator1D extrap = new LogLinearExtrapolator1D();
    final Interpolator1DDataBundle bundleExtrap = interp.getDataBundle(xValues, yValues);
    extrap.extrapolate(bundleExtrap, 1d, interp);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withinRangeDerivativeTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {11., 8., 5., 1.001, 1.001, 5., 8., 11. };

    final Interpolator1D interp = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Extrapolator1D extrap = new LogLinearExtrapolator1D();
    final Interpolator1DDataBundle bundleExtrap = interp.getDataBundle(xValues, yValues);
    extrap.firstDerivative(bundleExtrap, 1d, interp);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void withinRangeSensitivityTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {11., 8., 5., 1.001, 1.001, 5., 8., 11. };

    final Interpolator1D interp = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Extrapolator1D extrap = new LogLinearExtrapolator1D();
    final Interpolator1DDataBundle bundleExtrap = interp.getDataBundle(xValues, yValues);
    extrap.getNodeSensitivitiesForValue(bundleExtrap, 1d, interp);
  }
}
