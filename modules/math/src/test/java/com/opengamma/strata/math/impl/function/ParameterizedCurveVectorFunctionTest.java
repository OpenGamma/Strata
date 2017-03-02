/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Test simple a simple function a * Math.sinh(b * x)
 */
@Test
public class ParameterizedCurveVectorFunctionTest {

  private static final ParameterizedCurve s_PCurve;

  static {
    s_PCurve = new ParameterizedCurve() {

      @Override
      public Double evaluate(final Double x, final DoubleArray parameters) {
        final double a = parameters.get(0);
        final double b = parameters.get(1);
        return a * Math.sinh(b * x);
      }

      @Override
      public int getNumberOfParameters() {
        return 2;
      }
    };
  }

  @Test
  public void test() {
    final ParameterizedCurveVectorFunctionProvider pro = new ParameterizedCurveVectorFunctionProvider(s_PCurve);
    final double[] points = new double[] {-1.0, 0.0, 1.0 };
    final VectorFunction f = pro.from(points);
    assertEquals(2, f.getLengthOfDomain());
    assertEquals(3, f.getLengthOfRange());
    final DoubleArray x = DoubleArray.of(0.5, 2.0); //the parameters a & b
    final DoubleArray y = f.apply(x);
    assertEquals(0.5 * Math.sinh(-2.0), y.get(0), 1e-14);
    assertEquals(0.0, y.get(1), 1e-14);
    assertEquals(0.5 * Math.sinh(2.0), y.get(2), 1e-14);

    final DoubleMatrix jac = f.calculateJacobian(x);
    final DoubleMatrix fdJac = (new VectorFieldFirstOrderDifferentiator().differentiate(f)).apply(x);
    AssertMatrix.assertEqualsMatrix(fdJac, jac, 1e-9);
  }
}
