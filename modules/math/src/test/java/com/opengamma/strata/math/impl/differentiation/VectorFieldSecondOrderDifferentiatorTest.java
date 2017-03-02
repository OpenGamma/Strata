/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class VectorFieldSecondOrderDifferentiatorTest {

  private static Function<DoubleArray, DoubleArray> FUNC = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(DoubleArray x) {
      double a = x.get(0);
      double theta = x.get(1);
      double c1 = Math.cos(theta);
      return DoubleArray.of(a * c1 * c1, a * (1 - c1 * c1));
    }
  };

  private static Function<DoubleArray, Boolean> DOMAIN = new Function<DoubleArray, Boolean>() {

    @Override
    public Boolean apply(DoubleArray x) {
      double a = x.get(0);
      double theta = x.get(1);
      if (a <= 0) {
        return false;
      }
      if (theta < 0.0 || theta > Math.PI) {
        return false;
      }
      return true;
    }
  };

  private static Function<DoubleArray, DoubleMatrix> DW1 = new Function<DoubleArray, DoubleMatrix>() {
    @Override
    public DoubleMatrix apply(DoubleArray x) {
      double a = x.get(0);
      double theta = x.get(1);
      double[][] temp = new double[2][2];
      double c1 = Math.cos(theta);
      double s1 = Math.sin(theta);
      temp[0][0] = 0.0;
      temp[1][1] = 2 * a * (1 - 2 * c1 * c1);
      temp[0][1] = -2 * s1 * c1;
      temp[1][0] = temp[0][1];
      return DoubleMatrix.copyOf(temp);
    }
  };

  private static Function<DoubleArray, DoubleMatrix> DW2 = new Function<DoubleArray, DoubleMatrix>() {
    @Override
    public DoubleMatrix apply(DoubleArray x) {
      double a = x.get(0);
      double theta = x.get(1);
      double[][] temp = new double[2][2];
      double c1 = Math.cos(theta);
      double s1 = Math.sin(theta);
      temp[0][0] = 0.0;
      temp[1][1] = 2 * a * (2 * c1 * c1 - 1);
      temp[0][1] = 2 * s1 * c1;
      temp[1][0] = temp[0][1];
      return DoubleMatrix.copyOf(temp);
    }
  };

  @Test
  public void test() {
    double a = 2.3;
    double theta = 0.34;
    DoubleArray x = DoubleArray.of(a, theta);

    VectorFieldSecondOrderDifferentiator fd = new VectorFieldSecondOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix[]> fdFuncs = fd.differentiate(FUNC);
    DoubleMatrix[] fdValues = fdFuncs.apply(x);

    DoubleMatrix t1 = DW1.apply(x);
    DoubleMatrix t2 = DW2.apply(x);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals("first observation " + i + " " + j, t1.get(i, j), fdValues[0].get(i, j), 1e-6);
        assertEquals("second observation " + i + " " + j, t2.get(i, j), fdValues[1].get(i, j), 1e-6);
      }
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void outsideDomainTest() {
    VectorFieldSecondOrderDifferentiator fd = new VectorFieldSecondOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix[]> fdFuncs = fd.differentiate(FUNC, DOMAIN);
    fdFuncs.apply(DoubleArray.of(-1.0, 0.3));
  }

  @Test
  public void domainTest() {

    DoubleArray[] x = new DoubleArray[4];
    x[0] = DoubleArray.of(2.3, 0.34);
    x[1] = DoubleArray.of(1e-8, 1.45);
    x[2] = DoubleArray.of(1.2, 0.0);
    x[3] = DoubleArray.of(1.2, Math.PI);

    VectorFieldSecondOrderDifferentiator fd = new VectorFieldSecondOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix[]> fdFuncs = fd.differentiate(FUNC, DOMAIN);

    for (int k = 0; k < 4; k++) {
      DoubleMatrix[] fdValues = fdFuncs.apply(x[k]);
      DoubleMatrix t1 = DW1.apply(x[k]);
      DoubleMatrix t2 = DW2.apply(x[k]);
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 2; j++) {
          assertEquals("first observation " + i + " " + j, t1.get(i, j), fdValues[0].get(i, j), 1e-6);
          assertEquals("second observation " + i + " " + j, t2.get(i, j), fdValues[1].get(i, j), 1e-6);
        }
      }
    }
  }

  @Test
  public void tes_differentiateNoCrosst() {
    Function<DoubleArray, DoubleArray> func = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray x) {
        double x1 = x.get(0);
        double x2 = x.get(1);
        return DoubleArray.of(x1 * x1 * x1 + x2 * x2, Math.exp(1.5 * x1), Math.log(2d * x1) * Math.sin(x2));
      }
    };
    double[][] keys = new double[][] { {1.5, -2.1 }, {2.3445, 0.5 } };
    Function<DoubleArray, DoubleMatrix> funcExp = new Function<DoubleArray, DoubleMatrix>() {
      @Override
      public DoubleMatrix apply(DoubleArray x) {
        double x1 = x.get(0);
        double x2 = x.get(1);
        double[][] mat = new double[][] { {6d * x1, 2.25 * Math.exp(1.5 * x1), -Math.sin(x2) / x1 / x1 },
          {2d, 0d, -Math.log(2d * x1) * Math.sin(x2) } };
        return DoubleMatrix.ofUnsafe(mat);
      }
    };
    VectorFieldSecondOrderDifferentiator fd = new VectorFieldSecondOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix> fdFuncs = fd.differentiateNoCross(func);
    for (double[] key : keys) {
      DoubleMatrix cmp = fdFuncs.apply(DoubleArray.ofUnsafe(key));
      DoubleMatrix exp = funcExp.apply(DoubleArray.ofUnsafe(key));
      assertTrue(DoubleArrayMath.fuzzyEquals(cmp.column(0).toArray(), exp.row(0).toArray(), 1.0e-5));
      assertTrue(DoubleArrayMath.fuzzyEquals(cmp.column(1).toArray(), exp.row(1).toArray(), 1.0e-5));
    }
  }

}
