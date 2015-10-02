/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Create a few {@link VectorFunction} (as anonymous inner classes) and check they concatenate correctly 
 */
@Test
public class ConcatenatedVectorFunctionTest {

  private static final VectorFieldFirstOrderDifferentiator DIFF = new VectorFieldFirstOrderDifferentiator();

  private static final int NUM_FUNCS = 3;
  private static final VectorFunction[] F = new VectorFunction[NUM_FUNCS];

  private static final DoubleMatrix1D[] X = new DoubleMatrix1D[3];
  private static final DoubleMatrix1D[] Y_EXP = new DoubleMatrix1D[3];
  private static final DoubleMatrix2D[] JAC_EXP = new DoubleMatrix2D[3];

  static {
    F[0] = new VectorFunction() {

      @Override
      public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
        return new DoubleMatrix1D(1, x.getEntry(0) + 2 * x.getEntry(1));
      }

      @Override
      public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
        final DoubleMatrix2D jac = new DoubleMatrix2D(1, 2);
        jac.getData()[0][0] = 1.0;
        jac.getData()[0][1] = 2.0;
        return jac;
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
      public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
        final double x1 = x.getEntry(0);
        final double x2 = x.getEntry(1);
        final double y1 = x1 * x2;
        final double y2 = x2 * x2;
        return new DoubleMatrix1D(y1, y2);
      }

      @Override
      public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
        final double x1 = x.getEntry(0);
        final double x2 = x.getEntry(1);
        final double j11 = x2;
        final double j12 = x1;
        final double j21 = 0.0;
        final double j22 = 2 * x2;
        return new DoubleMatrix2D(new double[][] { {j11, j12 }, {j21, j22 } });
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
      public DoubleMatrix1D evaluate(final DoubleMatrix1D x) {
        final double x1 = x.getEntry(0);
        final double y1 = x1;
        final double y2 = Math.sin(x1);
        return new DoubleMatrix1D(y1, y2);
      }

      @Override
      public DoubleMatrix2D calculateJacobian(final DoubleMatrix1D x) {
        final double x1 = x.getEntry(0);
        final double j11 = 1.0;
        final double j21 = Math.cos(x1);
        return new DoubleMatrix2D(new double[][] { {j11 }, {j21 } });
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

    X[0] = new DoubleMatrix1D(-2., 2.);
    X[1] = new DoubleMatrix1D(1., 2.);
    X[2] = new DoubleMatrix1D(Math.PI);

    Y_EXP[0] = new DoubleMatrix1D(2.0);
    Y_EXP[1] = new DoubleMatrix1D(2.0, 4.0);
    Y_EXP[2] = new DoubleMatrix1D(Math.PI, 0.0);
    JAC_EXP[0] = new DoubleMatrix2D(new double[][] {{1.0, 2.0 } });
    JAC_EXP[1] = new DoubleMatrix2D(new double[][] { {2.0, 1.0 }, {0.0, 4.0 } });
    JAC_EXP[2] = new DoubleMatrix2D(new double[][] { {1.0 }, {-1.0 } });
  }

  /**
   * /check individual functions first
   */
  @Test
  public void functionsTest() {

    for (int i = 0; i < 3; i++) {
      final DoubleMatrix1D y = F[i].evaluate(X[i]);
      final DoubleMatrix2D jac = F[i].calculateJacobian(X[i]);
      AssertMatrix.assertEqualsVectors(Y_EXP[i], y, 1e-15);
      AssertMatrix.assertEqualsMatrix(JAC_EXP[i], jac, 1e-15);
    }
  }

  @Test
  public void conCatTest() {

    final DoubleMatrix1D cx = conCat(X);
    final DoubleMatrix1D cyExp = conCat(Y_EXP);
    final ConcatenatedVectorFunction cf = new ConcatenatedVectorFunction(F);
    final DoubleMatrix1D cy = cf.evaluate(cx);
    AssertMatrix.assertEqualsVectors(cyExp, cy, 1e-15);

    final DoubleMatrix2D cJac = cf.calculateJacobian(cx);
    final DoubleMatrix2D fdJac = DIFF.differentiate(cf).evaluate(cx);
    AssertMatrix.assertEqualsMatrix(fdJac, cJac, 1e-10);
  }

  private DoubleMatrix1D conCat(final DoubleMatrix1D[] x) {
    final int n = x.length;
    int pos = 0;
    for (int i = 0; i < n; i++) {
      pos += x[i].getNumberOfElements();
    }
    final DoubleMatrix1D res = new DoubleMatrix1D(pos);
    pos = 0;
    for (int i = 0; i < n; i++) {
      final int m = x[i].getNumberOfElements();
      System.arraycopy(x[i].getData(), 0, res.getData(), pos, m);
      pos += m;
    }
    return res;
  }

}
