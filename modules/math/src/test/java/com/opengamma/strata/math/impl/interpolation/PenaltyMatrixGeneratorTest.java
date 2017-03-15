/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * 
 */
@Test
public class PenaltyMatrixGeneratorTest {
  private static final MatrixAlgebra MA = new OGMatrixAlgebra();

  @Test
  public void differenceMatrix1DTest() {
    int n = 7;

    DoubleMatrix d0 = PenaltyMatrixGenerator.getDifferenceMatrix(n, 0); //zeroth order
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(n), d0, 1e-15);

    DoubleArray zeroVector = DoubleArray.filled(n);
    DoubleMatrix d1 = PenaltyMatrixGenerator.getDifferenceMatrix(n, 1); //first order difference matrix
    assertEquals(n, d1.rowCount());
    assertEquals(n, d1.columnCount());
    AssertMatrix.assertEqualsVectors(zeroVector, d1.row(0), 1e-15); //first row should be zero

    DoubleArray x = DoubleArray.filled(n, 1.0);
    DoubleArray d1x = (DoubleArray) MA.multiply(d1, x);
    //a constant vector should have zero first order differences 
    AssertMatrix.assertEqualsVectors(zeroVector, d1x, 1e-14);

    DoubleMatrix d2 = PenaltyMatrixGenerator.getDifferenceMatrix(n, 2); //second order difference matrix
    assertEquals(n, d2.rowCount());
    assertEquals(n, d2.columnCount());
    AssertMatrix.assertEqualsVectors(zeroVector, d2.row(0), 1e-15); //first two rows should be zero
    AssertMatrix.assertEqualsVectors(zeroVector, d2.row(1), 1e-15);

    DoubleArray x2 = DoubleArray.of(n, i -> i);
    d1x = (DoubleArray) MA.multiply(d1, x2);
    //first element of the diff vector is set to zero 
    DoubleArray ones = DoubleArray.filled(n, 1.0).with(0, 0);
    //vector with differences of one 
    AssertMatrix.assertEqualsVectors(ones, d1x, 1e-14);

    DoubleArray d2x = (DoubleArray) MA.multiply(d2, x2);
    //a linear vector should have zero second order differences 
    AssertMatrix.assertEqualsVectors(zeroVector, d2x, 1e-14);

    DoubleMatrix d3 = PenaltyMatrixGenerator.getDifferenceMatrix(n, 3); //third order difference matrix
    assertEquals(n, d3.rowCount());
    assertEquals(n, d3.columnCount());
    AssertMatrix.assertEqualsVectors(zeroVector, d3.row(0), 1e-15); //first three rows should be zero
    AssertMatrix.assertEqualsVectors(zeroVector, d3.row(1), 1e-15);
    AssertMatrix.assertEqualsVectors(zeroVector, d3.row(2), 1e-15);

    DoubleArray x3 = DoubleArray.of(n, i -> 0.5 + i + 0.1 * i * i);
    d1x = (DoubleArray) MA.multiply(d1, x3);
    // expected first order diff, first element is zero
    DoubleArray exp = DoubleArray.of(n, i -> 0.9 + 0.2 * i).with(0, 0);
    AssertMatrix.assertEqualsVectors(exp, d1x, 1e-14);

    // expected second order diff, first two elements are zero 
    exp = DoubleArray.filled(n, 0.2).with(0, 0).with(1, 0);
    d2x = (DoubleArray) MA.multiply(d2, x3);
    AssertMatrix.assertEqualsVectors(exp, d2x, 1e-14);

    DoubleArray d3x = (DoubleArray) MA.multiply(d3, x3);
    //a quadratic vector should have zero third order differences 
    AssertMatrix.assertEqualsVectors(zeroVector, d3x, 1e-14);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void diffOrderTooHighTest() {
    PenaltyMatrixGenerator.getDifferenceMatrix(6, 6);
  }

  @Test
  public void penaltyMatrix1DTest() {
    int n = 10;
    DoubleMatrix p0 = PenaltyMatrixGenerator.getPenaltyMatrix(n, 0); //zeroth order
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(n), p0, 1e-15);

    //constant
    DoubleArray x = DoubleArray.filled(n, 2.0);
    DoubleMatrix p = PenaltyMatrixGenerator.getPenaltyMatrix(n, 2);
    double r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);

    DoubleArray x2 = DoubleArray.of(n, i -> i);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0.0, r);

    DoubleArray x3 = DoubleArray.of(n, i -> 0.4 + 0.4 * i + i * i);
    r = MA.getInnerProduct(x3, MA.multiply(p, x3));
    //The second order diff is 2; for 2nd order difference use 8 values (n-2), so expect 8 * 2^2 = 32
    assertEquals(32.0, r, 1e-11);

    p = PenaltyMatrixGenerator.getPenaltyMatrix(n, 3);
    r = MA.getInnerProduct(x3, MA.multiply(p, x3));
    assertEquals(0.0, r, 1e-13);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void penaltyMatrixDiffOrderTooHighTest() {
    PenaltyMatrixGenerator.getPenaltyMatrix(6, 10);
  }

  @Test
  public void penaltyMatrix2DTest() {

    int n1 = 8;
    int n2 = 13;
    //constant
    DoubleArray x = DoubleArray.filled(n1 * n2, 2.0);
    DoubleMatrix p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 1, 0);
    double r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);

    //viewed as an x-y grid, this is flat in the x direction 
    double[][] data = new double[n1][n2];
    for (int i = 0; i < n1; i++) {
      for (int j = 0; j < n2; j++) {
        data[i][j] = 0.4 + j;
      }
    }
    x = PenaltyMatrixGenerator.flattenMatrix(DoubleMatrix.copyOf(data));
    r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);

    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 1, 1);
    r = MA.getInnerProduct(x, MA.multiply(p, x));
    //8*12
    assertEquals(96, r, 1e-12);

    double[] xArray = x.toArray();
    for (int i = 0; i < n1; i++) {
      for (int j = 0; j < n2; j++) {
        xArray[i * n2 + j] = 0.4 + j - 0.5 * i * i + 3 * i * j;
      }
    }
    DoubleArray x2 = DoubleArray.copyOf(xArray);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 2, 0);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    //6*13
    assertEquals(78, r, 1e-11);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 3, 0);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0, r, 2e-10);

    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 2, 1);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0, r, 2e-10);

    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, 1, 1);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(17232, r, 2e-10);

    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2}, new int[] {2, 1}, new double[] {1 / 78.0, 1. / 17232.});
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(2.0, r, 2e-10);
  }

  @Test
  public void penaltyMatrix3DTest() {

    int n1 = 5;
    int n2 = 13;
    int n3 = 4;

    //constant
    DoubleArray x = DoubleArray.filled(n1 * n2 * n3, 2.0);
    DoubleMatrix p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 1, 0);
    double r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 1, 1);
    r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 1, 2);
    r = MA.getInnerProduct(x, MA.multiply(p, x));
    assertEquals(0.0, r);

    double[] data = x.toArray();
    for (int i = 0; i < n1; i++) {
      for (int j = 0; j < n2; j++) {
        for (int k = 0; k < n3; k++) {
          data[i * n2 * n3 + j * n3 + k] = 0.4 + i - k + j * j - 3.0 * i * k + 4 * i * j;
        }
      }
    }
    DoubleArray x2 = DoubleArray.copyOf(data);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 2, 0);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0.0, r, 1e-11);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 3, 1);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0.0, r, 3e-10);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 2, 2);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    assertEquals(0.0, r, 5e-11);
    p = PenaltyMatrixGenerator.getPenaltyMatrix(new int[] {n1, n2, n3}, 2, 1);
    r = MA.getInnerProduct(x2, MA.multiply(p, x2));
    //4*11*5*4
    assertEquals(880, r, 1e-9);
  }

  @DataProvider
  Object[][] data() {
    Object[][] obj = new Object[1][4];
    double[] x = new double[] {0.0, 0.3, 0.7, 0.8, 1.2, 2.0};
    obj[0][0] = x;
    int n = x.length;

    DoubleArray y = DoubleArray.of(n, i -> 0.3 + 0.7 * x[i] - 0.4 * x[i] * x[i]);
    DoubleArray dydx = DoubleArray.of(n, i -> 0.7 - 0.8 * x[i]);
    DoubleArray d2ydx2 = DoubleArray.filled(n, -0.8);
    obj[0][1] = y;
    obj[0][2] = dydx;
    obj[0][3] = d2ydx2;
    return obj;
  }

  @Test(dataProvider = "data")
  public void derivativeMatrix1DTest(double[] x, DoubleArray y, DoubleArray dydx, DoubleArray d2ydx2) {

    int n = x.length;
    DoubleMatrix d0 = PenaltyMatrixGenerator.getDerivativeMatrix(x, 0, true);
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(n), d0, 1e-14);

    DoubleMatrix d1 = PenaltyMatrixGenerator.getDerivativeMatrix(x, 1, true);
    DoubleMatrix d2 = PenaltyMatrixGenerator.getDerivativeMatrix(x, 2, true);
    DoubleArray d1y = (DoubleArray) MA.multiply(d1, y);
    DoubleArray d2y = (DoubleArray) MA.multiply(d2, y);
    AssertMatrix.assertEqualsVectors(dydx, d1y, 1e-13);
    AssertMatrix.assertEqualsVectors(d2ydx2, d2y, 1e-13);

  }

  @Test(dataProvider = "data")
  public void penaltyMatrix1DTest(double[] x, DoubleArray y, DoubleArray dydx, DoubleArray d2ydx2) {
    int n = x.length;
    double expected = 0.0;
    for (int i = 0; i < n; i++) {
      if (i > 0 && i < (n - 1)) { // we not not use the end points
        expected += FunctionUtils.square(d2ydx2.get(i));
      }
    }
    double scale = Math.pow(2.0, 4); //((2.0-0.0)^2^2)
    expected *= scale;

    DoubleMatrix p2 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 2);
    double r = MA.getInnerProduct(y, MA.multiply(p2, y));
    assertEquals(expected, r, 1e-11);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void orderG2Test() {
    double[] x = new double[] {0.0, 0.3, 0.7, 0.8, 1.2, 2.0};
    @SuppressWarnings("unused")
    DoubleMatrix d3 = PenaltyMatrixGenerator.getDerivativeMatrix(x, 3, true);
  }

  /**
   * The penalty matrix is scaled such that the result of x^T*P*x is insensitive to the scale of x 
   */
  @Test
  public void penaltyMatrixScaleTest() {
    double[] x = new double[] {0.0, 0.3, 0.7, 0.8, 1.2, 2.0};
    double scale = 5.0; //scale the x-axis by a factor of 5
    int n = x.length;
    double[] xScaled = new double[n];

    DoubleArray y = DoubleArray.of(n, i -> 0.3 + x[i] + Math.sin(x[i]));
    for (int i = 0; i < n; i++) {
      xScaled[i] = x[i] * scale;
    }

    //first order
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 1);
    DoubleMatrix p1s = PenaltyMatrixGenerator.getPenaltyMatrix(xScaled, 1);
    double r = MA.getInnerProduct(y, MA.multiply(p1, y));
    double rs = MA.getInnerProduct(y, MA.multiply(p1s, y));
    assertEquals(r, rs, 1e-10);

    //second order
    DoubleMatrix p2 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 2);
    DoubleMatrix p2s = PenaltyMatrixGenerator.getPenaltyMatrix(xScaled, 2);
    r = MA.getInnerProduct(y, MA.multiply(p2, y));
    rs = MA.getInnerProduct(y, MA.multiply(p2s, y));
    assertEquals(r, rs, 1e-10);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void xNotUniqueTest() {
    double[] x = new double[] {0.0, 0.3, 0.8, 0.8, 1.2, 2.0};
    @SuppressWarnings("unused")
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void xNotAscendingTest() {
    double[] x = new double[] {0.0, 0.3, 0.8, 0.7, 1.2, 2.0};
    @SuppressWarnings("unused")
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void emptyXTest() {
    @SuppressWarnings("unused")
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(new double[0], 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negRangeTest() {
    double[] x = new double[] {0.0, -2.0};
    @SuppressWarnings("unused")
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void singlePointTest() {
    double[] x = new double[] {0.2};
    @SuppressWarnings("unused")
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 1);
  }

  @Test
  public void zeroOrderSinglePointTest() {
    double[] x = new double[] {0.2};
    DoubleMatrix p1 = PenaltyMatrixGenerator.getPenaltyMatrix(x, 0);
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(1), p1, 1e-15);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void twoPointsTest() {
    double[] x = new double[] {0.2, 0.5};
    @SuppressWarnings("unused")
    DoubleMatrix d1 = PenaltyMatrixGenerator.getDerivativeMatrix(x, 1, true);
  }

  /**
   * create a quadratic function on a non-uniform 2D grid, then flatten this to a vector and check the first and
   * second order differentiation matrices and penalty matrices work in both dimensions    
   */
  @Test
  public void penalty2DTest() {
    double[] x = new double[] {0.0, 0.3, 0.7, 0.8, 1.2, 2.0};
    double[] y = new double[] {-20.0, -10.0, 0.0, 5.0, 15.0, 19.0, 20.0};
    int nx = x.length;
    int ny = y.length;

    DoubleMatrix p0 = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 0, 0);
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(nx * ny), p0, 1e-14);
    p0 = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 0, 1);
    AssertMatrix.assertEqualsMatrix(DoubleMatrix.identity(nx * ny), p0, 1e-14);

    DoubleMatrix diffX1DFirstOrder = PenaltyMatrixGenerator.getDerivativeMatrix(x, 1, true);
    DoubleMatrix diffY1DFirstOrder = PenaltyMatrixGenerator.getDerivativeMatrix(y, 1, true);
    DoubleMatrix diffX1DSecOrder = PenaltyMatrixGenerator.getDerivativeMatrix(x, 2, true);
    DoubleMatrix diffY1DSecOrder = PenaltyMatrixGenerator.getDerivativeMatrix(y, 2, true);
    DoubleMatrix diffX2DFirstOrder = PenaltyMatrixGenerator.getMatrixForFlattened(new int[] {nx, ny}, diffX1DFirstOrder, 0);
    DoubleMatrix diffY2DFirstOrder = PenaltyMatrixGenerator.getMatrixForFlattened(new int[] {nx, ny}, diffY1DFirstOrder, 1);
    DoubleMatrix diffX2DSecOrder = PenaltyMatrixGenerator.getMatrixForFlattened(new int[] {nx, ny}, diffX1DSecOrder, 0);
    DoubleMatrix diffY2DSecOrder = PenaltyMatrixGenerator.getMatrixForFlattened(new int[] {nx, ny}, diffY1DSecOrder, 1);

    DoubleArray z = DoubleArray.filled(nx * ny);
    DoubleArray dzdx = DoubleArray.filled(nx * ny);
    DoubleArray d2zdx2 = DoubleArray.filled(nx * ny);
    DoubleArray dzdy = DoubleArray.filled(nx * ny);
    DoubleArray d2zdy2 = DoubleArray.filled(nx * ny);
    double dzdxSum = 0;
    double d2zdx2Sum = 0;
    double dzdySum = 0;
    double d2zdy2Sum = 0;
    for (int i = 0; i < nx; i++) {
      double xi = x[i];
      for (int j = 0; j < ny; j++) {
        double yj = y[j];
        int index = i * ny + j;
        z = z.with(index, 0.3 + xi + 0.4 * xi * xi + 0.01 * yj - 1e-4 * yj * yj + 0.1 * xi * yj);
        dzdx = dzdx.with(index, 1.0 + 0.8 * xi + 0.1 * yj);
        d2zdx2 = d2zdx2.with(index, 0.8);
        dzdy = dzdy.with(index, 0.01 - 2e-4 * yj + 0.1 * xi);
        d2zdy2 = d2zdy2.with(index, -2e-4);

        //The penalty matrix does not use end points, so don't include them here 
        if (i != 0 & i != (nx - 1)) {
          dzdxSum += FunctionUtils.square(dzdx.get(index));
          d2zdx2Sum += FunctionUtils.square(d2zdx2.get(index));
        }
        if (j != 0 & j != (ny - 1)) {
          dzdySum += FunctionUtils.square(dzdy.get(index));
          d2zdy2Sum += FunctionUtils.square(d2zdy2.get(index));
        }

      }
    }

    AssertMatrix.assertEqualsVectors(dzdx, (DoubleArray) MA.multiply(diffX2DFirstOrder, z), 1e-12);
    AssertMatrix.assertEqualsVectors(dzdy, (DoubleArray) MA.multiply(diffY2DFirstOrder, z), 1e-12);
    AssertMatrix.assertEqualsVectors(d2zdx2, (DoubleArray) MA.multiply(diffX2DSecOrder, z), 1e-12);
    AssertMatrix.assertEqualsVectors(d2zdy2, (DoubleArray) MA.multiply(diffY2DSecOrder, z), 1e-12);

    DoubleMatrix p1x = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 1, 0);
    DoubleMatrix p2x = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 2, 0);
    DoubleMatrix p1y = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 1, 1);
    DoubleMatrix p2y = PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, 2, 1);
    double r1x = MA.getInnerProduct(z, MA.multiply(p1x, z));
    double r2x = MA.getInnerProduct(z, MA.multiply(p2x, z));
    double r1y = MA.getInnerProduct(z, MA.multiply(p1y, z));
    double r2y = MA.getInnerProduct(z, MA.multiply(p2y, z));

    double xRange = x[nx - 1] - x[0];
    double yRange = y[ny - 1] - y[0];

    assertEquals("first order x", Math.pow(xRange, 2) * dzdxSum, r1x, 1e-10);
    assertEquals("second order x", Math.pow(xRange, 4) * d2zdx2Sum, r2x, 1e-9);
    assertEquals("first order y", Math.pow(yRange, 2) * dzdySum, r1y, 1e-10);
    assertEquals("second order y", Math.pow(yRange, 4) * d2zdy2Sum, r2y, 1e-8);

    double lambdaX = 0.7;
    double lambdaY = Math.PI;
    //second order in x and first order in y
    DoubleMatrix p =
        PenaltyMatrixGenerator.getPenaltyMatrix(new double[][] {x, y}, new int[] {2, 1}, new double[] {lambdaX, lambdaY});
    double r = MA.getInnerProduct(z, MA.multiply(p, z));
    double expR = Math.pow(xRange, 4) * d2zdx2Sum * lambdaX + Math.pow(yRange, 2) * dzdySum * lambdaY;
    assertEquals(expR, r, 1e-9);
  }
}
