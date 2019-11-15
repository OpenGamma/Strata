/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static com.opengamma.strata.math.impl.interpolation.PenaltyMatrixGenerator.getPenaltyMatrix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Test {@link NonLinearLeastSquareWithPenalty}.
 */
public class NonLinearLeastSquareWithPenaltyTest {

  private static final MatrixAlgebra MA = new CommonsMatrixAlgebra();

  private static final NonLinearLeastSquareWithPenalty NLLSWP = new NonLinearLeastSquareWithPenalty();

  @Test
  public void linearTest() {
    boolean print = false;
    if (print) {
      System.out.println("NonLinearLeastSquareWithPenaltyTest.linearTest");
    }
    int nWeights = 20;
    int diffOrder = 2;
    double lambda = 100.0;
    DoubleMatrix penalty = (DoubleMatrix) MA.scale(getPenaltyMatrix(nWeights, diffOrder), lambda);
    int[] onIndex = new int[] {1, 4, 11, 12, 15, 17};
    double[] obs = new double[] {0, 1.0, 1.0, 1.0, 0.0, 0.0};
    int n = onIndex.length;

    Function<DoubleArray, DoubleArray> func = new Function<DoubleArray, DoubleArray>() {

      @Override
      public DoubleArray apply(DoubleArray x) {
        return DoubleArray.of(n, i -> x.get(onIndex[i]));
      }
    };

    Function<DoubleArray, DoubleMatrix> jac = new Function<DoubleArray, DoubleMatrix>() {

      @Override
      public DoubleMatrix apply(DoubleArray x) {
        return DoubleMatrix.of(
            n,
            nWeights,
            (i, j) -> j == onIndex[i] ? 1d : 0d);
      }
    };

    Well44497b random = new Well44497b(0L);
    DoubleArray start = DoubleArray.of(nWeights, i -> random.nextDouble());

    LeastSquareWithPenaltyResults lsRes = NLLSWP.solve(
        DoubleArray.copyOf(obs),
        DoubleArray.filled(n, 0.01),
        func,
        jac,
        start,
        penalty);
    if (print) {
      System.out.println("chi2: " + lsRes.getChiSq());
      System.out.println(lsRes.getFitParameters());
    }
    for (int i = 0; i < n; i++) {
      assertThat(obs[i]).isCloseTo(lsRes.getFitParameters().get(onIndex[i]), offset(0.01));
    }
    double expPen = 20.87912357454752;
    assertThat(expPen).isCloseTo(lsRes.getPenalty(), offset(1e-9));
  }

}
