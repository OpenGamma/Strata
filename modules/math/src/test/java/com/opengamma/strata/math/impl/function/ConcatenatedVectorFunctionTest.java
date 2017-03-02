/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Create a few {@link VectorFunction} (as anonymous inner classes) and check they concatenate correctly 
 */
@Test
public class ConcatenatedVectorFunctionTest {

  private static final VectorFieldFirstOrderDifferentiator DIFF = new VectorFieldFirstOrderDifferentiator();

  private static final int NUM_FUNCS = 3;
  private static final VectorFunction[] F = new VectorFunction[NUM_FUNCS];

  private static final DoubleArray[] X = new DoubleArray[3];
  private static final DoubleArray[] Y_EXP = new DoubleArray[3];
  private static final DoubleMatrix[] JAC_EXP = new DoubleMatrix[3];

  static {
    F[0] = new VectorFunction() {

      @Override
      public DoubleArray apply(DoubleArray x) {
        return DoubleArray.filled(1, x.get(0) + 2 * x.get(1));
      }

      @Override
      public DoubleMatrix calculateJacobian(DoubleArray x) {
        return DoubleMatrix.of(1, 2, 1d, 2d);
      }

      @Override
      public int getLengthOfDomain() {
        return 2;
      }

      @Override
      public int getLengthOfRange() {
        return 1;
      }
    };

    F[1] = new VectorFunction() {

      @Override
      public DoubleArray apply(DoubleArray x) {
        double x1 = x.get(0);
        double x2 = x.get(1);
        double y1 = x1 * x2;
        double y2 = x2 * x2;
        return DoubleArray.of(y1, y2);
      }

      @Override
      public DoubleMatrix calculateJacobian(DoubleArray x) {
        double x1 = x.get(0);
        double x2 = x.get(1);
        double j11 = x2;
        double j12 = x1;
        double j21 = 0.0;
        double j22 = 2 * x2;
        return DoubleMatrix.of(2, 2, j11, j12, j21, j22);
      }

      @Override
      public int getLengthOfDomain() {
        return 2;
      }

      @Override
      public int getLengthOfRange() {
        return 2;
      }
    };

    F[2] = new VectorFunction() {

      @Override
      public DoubleArray apply(DoubleArray x) {
        double x1 = x.get(0);
        double y1 = x1;
        double y2 = Math.sin(x1);
        return DoubleArray.of(y1, y2);
      }

      @Override
      public DoubleMatrix calculateJacobian(DoubleArray x) {
        double x1 = x.get(0);
        double j11 = 1.0;
        double j21 = Math.cos(x1);
        return DoubleMatrix.of(2, 1, j11, j21);
      }

      @Override
      public int getLengthOfDomain() {
        return 1;
      }

      @Override
      public int getLengthOfRange() {
        return 2;
      }
    };

    X[0] = DoubleArray.of(-2., 2.);
    X[1] = DoubleArray.of(1., 2.);
    X[2] = DoubleArray.of(Math.PI);

    Y_EXP[0] = DoubleArray.of(2.0);
    Y_EXP[1] = DoubleArray.of(2.0, 4.0);
    Y_EXP[2] = DoubleArray.of(Math.PI, 0.0);
    JAC_EXP[0] = DoubleMatrix.of(1, 2, 1d, 2d);
    JAC_EXP[1] = DoubleMatrix.of(2, 2, 2d, 1d, 0d, 4d);
    JAC_EXP[2] = DoubleMatrix.of(2, 1, 1d, -1d);
  }

  /**
   * /check individual functions first
   */
  @Test
  public void functionsTest() {

    for (int i = 0; i < 3; i++) {
      DoubleArray y = F[i].apply(X[i]);
      DoubleMatrix jac = F[i].calculateJacobian(X[i]);
      AssertMatrix.assertEqualsVectors(Y_EXP[i], y, 1e-15);
      AssertMatrix.assertEqualsMatrix(JAC_EXP[i], jac, 1e-15);
    }
  }

  @Test
  public void conCatTest() {
    DoubleArray cx = X[0].concat(X[1]).concat(X[2]);
    DoubleArray cyExp = Y_EXP[0].concat(Y_EXP[1]).concat(Y_EXP[2]);
    ConcatenatedVectorFunction cf = new ConcatenatedVectorFunction(F);
    DoubleArray cy = cf.apply(cx);
    AssertMatrix.assertEqualsVectors(cyExp, cy, 1e-15);

    DoubleMatrix cJac = cf.calculateJacobian(cx);
    DoubleMatrix fdJac = DIFF.differentiate(cf).apply(cx);
    AssertMatrix.assertEqualsMatrix(fdJac, cJac, 1e-10);
  }

}
