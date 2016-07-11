/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.BasisFunctionAggregation;
import com.opengamma.strata.math.impl.interpolation.BasisFunctionGenerator;
import com.opengamma.strata.math.impl.interpolation.PSplineFitter;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Test.
 */
@SuppressWarnings("deprecation")
@Test
public class GeneralizedLeastSquareTest {
  private static boolean PRINT = false;

  protected static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  private static final NormalDistribution NORMAL = new NormalDistribution(0, 1.0, RANDOM);
  private static final double[] WEIGHTS = new double[] {1.0, -0.5, 2.0, 0.23, 1.45 };
  private static final Double[] X;
  private static final double[] Y;
  private static final double[] SIGMA;
  private static final List<DoubleArray> X_TRIG;
  private static final List<Double> Y_TRIG;
  private static final List<Double> SIGMA_TRIG;
  private static final List<Double> SIGMA_COS_EXP;
  private static final List<double[]> X_SIN_EXP;
  private static final List<Double> Y_SIN_EXP;
  private static final List<Function<Double, Double>> SIN_FUNCTIONS;
  private static final Function<Double, Double> TEST_FUNCTION;
  private static final List<Function<Double, Double>> BASIS_FUNCTIONS;
  private static final List<Function<double[], Double>> BASIS_FUNCTIONS_2D;
  private static Function<double[], Double> SIN_EXP_FUNCTION;

  private static final List<Function<DoubleArray, Double>> VECTOR_TRIG_FUNCTIONS;
  private static final Function<DoubleArray, Double> VECTOR_TEST_FUNCTION;

  static {
    SIN_FUNCTIONS = new ArrayList<>();
    for (int i = 0; i < WEIGHTS.length; i++) {
      final int k = i;
      final Function<Double, Double> func = new Function<Double, Double>() {

        @Override
        public Double apply(final Double x) {
          return Math.sin((2 * k + 1) * x);
        }
      };
      SIN_FUNCTIONS.add(func);
    }
    TEST_FUNCTION = new BasisFunctionAggregation<>(SIN_FUNCTIONS, WEIGHTS);

    VECTOR_TRIG_FUNCTIONS = new ArrayList<>();
    for (int i = 0; i < WEIGHTS.length; i++) {
      final int k = i;
      final Function<DoubleArray, Double> func = new Function<DoubleArray, Double>() {
        @Override
        public Double apply(final DoubleArray x) {
          ArgChecker.isTrue(x.size() == 2);
          return Math.sin((2 * k + 1) * x.get(0)) * Math.cos((2 * k + 1) * x.get(1));
        }
      };
      VECTOR_TRIG_FUNCTIONS.add(func);
    }
    VECTOR_TEST_FUNCTION = new BasisFunctionAggregation<>(VECTOR_TRIG_FUNCTIONS, WEIGHTS);

    SIN_EXP_FUNCTION = new Function<double[], Double>() {

      @Override
      public Double apply(final double[] x) {
        return Math.sin(Math.PI * x[0] / 10.0) * Math.exp(-x[1] / 5.);
      }
    };

    final int n = 10;

    X = new Double[n];
    Y = new double[n];
    SIGMA = new double[n];
    X_TRIG = new ArrayList<>();
    Y_TRIG = new ArrayList<>();
    SIGMA_TRIG = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      X[i] = i / 5.0;
      Y[i] = TEST_FUNCTION.apply(X[i]);
      final double[] temp = new double[2];
      temp[0] = 2.0 * RANDOM.nextDouble();
      temp[1] = 2.0 * RANDOM.nextDouble();
      X_TRIG.add(DoubleArray.copyOf(temp));
      Y_TRIG.add(VECTOR_TEST_FUNCTION.apply(X_TRIG.get(i)));
      SIGMA[i] = 0.01;
      SIGMA_TRIG.add(0.01);
    }

    SIGMA_COS_EXP = new ArrayList<>();
    X_SIN_EXP = new ArrayList<>();
    Y_SIN_EXP = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      final double[] temp = new double[2];
      temp[0] = 10.0 * RANDOM.nextDouble();
      temp[1] = 10.0 * RANDOM.nextDouble();
      X_SIN_EXP.add(temp);
      Y_SIN_EXP.add(SIN_EXP_FUNCTION.apply(X_SIN_EXP.get(i)));
      SIGMA_COS_EXP.add(0.01);
    }

    final BasisFunctionGenerator generator = new BasisFunctionGenerator();
    BASIS_FUNCTIONS = generator.generateSet(0.0, 2.0, 20, 3);
    BASIS_FUNCTIONS_2D = generator.generateSet(new double[] {0.0, 0.0 }, new double[] {10.0, 10.0 }, new int[] {10, 10 }, new int[] {3, 3 });

  }

  public void testPerfectFit() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();
    final LeastSquareResults results = gls.solve(X, Y, SIGMA, SIN_FUNCTIONS);
    assertEquals(0.0, results.getChiSq(), 1e-8);
    final DoubleArray w = results.getFitParameters();
    for (int i = 0; i < WEIGHTS.length; i++) {
      assertEquals(WEIGHTS[i], w.get(i), 1e-8);
    }
  }

  public void testPerfectFitVector() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();
    final LeastSquareResults results = gls.solve(X_TRIG, Y_TRIG, SIGMA_TRIG, VECTOR_TRIG_FUNCTIONS);
    assertEquals(0.0, results.getChiSq(), 1e-8);
    final DoubleArray w = results.getFitParameters();
    for (int i = 0; i < WEIGHTS.length; i++) {
      assertEquals(WEIGHTS[i], w.get(i), 1e-8);
    }
  }

  public void testFit() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();
    final double[] y = new double[Y.length];
    for (int i = 0; i < Y.length; i++) {
      y[i] = Y[i] + SIGMA[i] * NORMAL.nextRandom();
    }

    final LeastSquareResults results = gls.solve(X, y, SIGMA, SIN_FUNCTIONS);
    assertTrue(results.getChiSq() < 3 * Y.length);

  }

  public void testBSplineFit() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();

    final LeastSquareResults results = gls.solve(X, Y, SIGMA, BASIS_FUNCTIONS);
    final Function<Double, Double> spline =
        new BasisFunctionAggregation<>(BASIS_FUNCTIONS, results.getFitParameters().toArray());
    assertEquals(0.0, results.getChiSq(), 1e-12);
    assertEquals(-0.023605293, spline.apply(0.5), 1e-8);

    if (PRINT) {
      System.out.println("Chi^2:\t" + results.getChiSq());
      System.out.println("weights:\t" + results.getFitParameters());

      for (int i = 0; i < 101; i++) {
        final double x = 0 + i * 2.0 / 100.0;
        System.out.println(x + "\t" + spline.apply(x));
      }
      for (int i = 0; i < X.length; i++) {
        System.out.println(X[i] + "\t" + Y[i]);
      }
    }
  }

  public void testBSplineFit2D() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();

    final LeastSquareResults results = gls.solve(X_SIN_EXP, Y_SIN_EXP, SIGMA_COS_EXP, BASIS_FUNCTIONS_2D);
    final Function<double[], Double> spline =
        new BasisFunctionAggregation<>(BASIS_FUNCTIONS_2D, results.getFitParameters().toArray());
    assertEquals(0.0, results.getChiSq(), 1e-16);
    assertEquals(0.05161579, spline.apply(new double[] {4, 3 }), 1e-8);

    /*
     * Print out function for debugging
     */
    if (PRINT) {
      System.out.println("Chi^2:\t" + results.getChiSq());
      System.out.println("weights:\t" + results.getFitParameters());

      final double[] x = new double[2];

      for (int i = 0; i < 101; i++) {
        x[0] = 0 + i * 10.0 / 100.0;
        System.out.print("\t" + x[0]);
      }
      System.out.print("\n");
      for (int i = 0; i < 101; i++) {
        x[0] = -0. + i * 10 / 100.0;
        System.out.print(x[0]);
        for (int j = 0; j < 101; j++) {
          x[1] = -0.0 + j * 10.0 / 100.0;
          final double y = spline.apply(x);
          System.out.print("\t" + y);
        }
        System.out.print("\n");
      }
    }
  }

  public void testPSplineFit() {
    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();

    final GeneralizedLeastSquareResults<Double> results = gls.solve(X, Y, SIGMA, BASIS_FUNCTIONS, 1000.0, 2);
    final Function<Double, Double> spline = results.getFunction();
    assertEquals(2225.7, results.getChiSq(), 1e-1);
    assertEquals(-0.758963811327287, spline.apply(1.1), 1e-8);

    /*
     * Print out function for debugging
     */
    if (PRINT) {
      System.out.println("Chi^2:\t" + results.getChiSq());
      System.out.println("weights:\t" + results.getFitParameters());

      for (int i = 0; i < 101; i++) {
        final double x = 0 + i * 2.0 / 100.0;
        System.out.println(x + "\t" + spline.apply(x));
      }
      for (int i = 0; i < X.length; i++) {
        System.out.println(X[i] + "\t" + Y[i]);
      }
    }
  }

  public void testPSplineFit2() {
    final BasisFunctionGenerator generator = new BasisFunctionGenerator();
    List<Function<Double, Double>> basisFuncs = generator.generateSet(0, 12, 100, 3);
    List<Function<Double, Double>> basisFuncsLog = generator.generateSet(-5, 3, 100, 3);

    final GeneralizedLeastSquare gls = new GeneralizedLeastSquare();

    final double[] xData = new double[] {7. / 365, 14 / 365., 21 / 365., 1 / 12., 3 / 12., 0.5, 0.75, 1, 5, 10 };
    final double[] yData = new double[] {0.972452371,
      0.749039802,
      0.759792085,
      0.714206462,
      0.604446956,
      0.517955313,
      0.474807307,
      0.443532132,
      0.2404755,
      0.197128583,

    };

    final int n = xData.length;
    final double[] lnX = new double[n];
    final double[] yData2 = new double[n];
    for (int i = 0; i < n; i++) {
      lnX[i] = Math.log(xData[i]);
      yData2[i] = yData[i] * yData[i] * xData[i];
    }

    final double[] sigma = new double[n];
    Arrays.fill(sigma, 0.01);
    final GeneralizedLeastSquareResults<Double> results = gls.solve(xData, yData, sigma, basisFuncs, 1000.0, 2);
    final Function<Double, Double> spline = results.getFunction();
    final GeneralizedLeastSquareResults<Double> resultsLog = gls.solve(lnX, yData, sigma, basisFuncsLog, 1000.0, 2);
    final Function<Double, Double> splineLog = resultsLog.getFunction();
    final GeneralizedLeastSquareResults<Double> resultsVar = gls.solve(xData, yData2, sigma, basisFuncs, 1000.0, 2);
    final Function<Double, Double> splineVar = resultsVar.getFunction();
    final GeneralizedLeastSquareResults<Double> resultsVarLog = gls.solve(lnX, yData2, sigma, basisFuncsLog, 1000.0, 2);
    final Function<Double, Double> splineVarLog = resultsVarLog.getFunction();

    if (PRINT) {
      System.out.println("Chi^2:\t" + results.getChiSq());
      System.out.println("weights:\t" + results.getFitParameters());

      for (int i = 0; i < 101; i++) {
        final double logX = -5 + 8 * i / 100.;
        final double x = Math.exp(logX);
        System.out.println(x + "\t" + +logX + "\t" + spline.apply(x) + "\t"
            + splineLog.apply(logX) + "\t" + splineVar.apply(x) + "\t" + splineVarLog.apply(logX));
      }
      for (int i = 0; i < n; i++) {
        System.out.println(lnX[i] + "\t" + yData[i]);
      }
    }

  }

  public void testPSplineFit2D() {

    final PSplineFitter psf = new PSplineFitter();
    final GeneralizedLeastSquareResults<double[]> results = psf.solve(X_SIN_EXP, Y_SIN_EXP, SIGMA_COS_EXP, new double[] {0.0, 0.0 }, new double[] {10.0, 10.0 }, new int[] {10, 10 },
        new int[] {3, 3 },
        new double[] {0.001, 0.001 }, new int[] {3, 3 });

    assertEquals(0.0, results.getChiSq(), 1e-9);
    final Function<double[], Double> spline = results.getFunction();
    assertEquals(0.5333876489112092, spline.apply(new double[] {4, 3 }), 1e-8);

    /*
     * Print out function for debugging
     */
    if (PRINT) {
      System.out.println("Chi^2:\t" + results.getChiSq());
      System.out.println("weights:\t" + results.getFitParameters());

      final double[] x = new double[2];

      for (int i = 0; i < 101; i++) {
        x[0] = 0 + i * 10.0 / 100.0;
        System.out.print("\t" + x[0]);
      }
      System.out.print("\n");
      for (int i = 0; i < 101; i++) {
        x[0] = -0. + i * 10 / 100.0;
        System.out.print(x[0]);
        for (int j = 0; j < 101; j++) {
          x[1] = -0.0 + j * 10.0 / 100.0;
          final double y = spline.apply(x);
          System.out.print("\t" + y);
        }
        System.out.print("\n");
      }
    }
  }
}
