/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static com.opengamma.strata.math.impl.interpolation.PenaltyMatrixGenerator.getPenaltyMatrix;
import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Test {@link NonLinearLeastSquareWithPenalty}.
 */
@Test
public class NonLinearLeastSquareWithPenaltyTest {

  private static final MatrixAlgebra MA = new CommonsMatrixAlgebra();

  private static NonLinearLeastSquareWithPenalty NLLSWP = new NonLinearLeastSquareWithPenalty();
  static int N_SWAPS = 8;

  public void linearTest() {
    boolean print = false;
    if (print) {
      System.out.println("NonLinearLeastSquareWithPenaltyTest.linearTest");
    }
    int nWeights = 20;
    int diffOrder = 2;
    double lambda = 100.0;
    DoubleMatrix2D penalty = (DoubleMatrix2D) MA.scale(getPenaltyMatrix(nWeights, diffOrder), lambda);
    int[] onIndex = new int[] {1, 4, 11, 12, 15, 17};
    double[] obs = new double[] {0, 1.0, 1.0, 1.0, 0.0, 0.0};
    int n = onIndex.length;

    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        double[] temp = new double[n];
        for (int i = 0; i < n; i++) {
          temp[i] = x.getEntry(onIndex[i]);
        }
        return new DoubleMatrix1D(temp);
      }
    };

    Function1D<DoubleMatrix1D, DoubleMatrix2D> jac = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
        DoubleMatrix2D res = new DoubleMatrix2D(n, nWeights);
        for (int i = 0; i < n; i++) {
          res.getData()[i][onIndex[i]] = 1.0;
        }
        return res;
      }
    };

    Well44497b random = new Well44497b(0L);
    double[] temp = new double[nWeights];
    for (int i = 0; i < nWeights; i++) {
      temp[i] = random.nextDouble();
    }
    DoubleMatrix1D start = new DoubleMatrix1D(temp);

    LeastSquareWithPenaltyResults lsRes = NLLSWP.solve(
        new DoubleMatrix1D(obs),
        new DoubleMatrix1D(n, 0.01),
        func,
        jac,
        start,
        penalty);
    if (print) {
      System.out.println("chi2: " + lsRes.getChiSq());
      System.out.println(lsRes.getFitParameters());
    }
    for (int i = 0; i < n; i++) {
      assertEquals(obs[i], lsRes.getFitParameters().getEntry(onIndex[i]), 0.01);
    }
    double expPen = 20.87912357454752;
    assertEquals(expPen, lsRes.getPenalty(), 1e-9);
  }

}
