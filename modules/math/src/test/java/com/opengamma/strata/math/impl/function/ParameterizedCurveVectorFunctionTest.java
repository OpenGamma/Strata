/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
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
      public Double evaluate(final Double x, final DoubleMatrix1D parameters) {
        final double a = parameters.getEntry(0);
        final double b = parameters.getEntry(1);
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
    final DoubleMatrix1D x = new DoubleMatrix1D(0.5, 2.0); //the parameters a & b
    final DoubleMatrix1D y = f.evaluate(x);
    assertEquals(0.5 * Math.sinh(-2.0), y.getEntry(0), 1e-14);
    assertEquals(0.0, y.getEntry(1), 1e-14);
    assertEquals(0.5 * Math.sinh(2.0), y.getEntry(2), 1e-14);

    final DoubleMatrix2D jac = f.calculateJacobian(x);
    final DoubleMatrix2D fdJac = (new VectorFieldFirstOrderDifferentiator().differentiate(f)).evaluate(x);
    AssertMatrix.assertEqualsMatrix(fdJac, jac, 1e-9);
  }
}
