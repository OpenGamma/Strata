/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Set up a simple parameterised surface (based on the function a * Math.sin(b * x + c * y) + Math.cos(y), where a, b, & c are the parameters)
 * and check the finite difference sensitivity (the default behaviour of getYParameterSensitivity) agrees with the analytic 
 * calculation for a range of points along the curve.
 */
@Test
public class ParameterizedSurfaceTest {

  @Test
  public void test() {

    /**
     * Take the form $y = a\sin(bx + cy) + cos(y)$
     */
    final ParameterizedSurface testSurface = new ParameterizedSurface() {

      @Override
      public Double evaluate(final DoublesPair xy, final DoubleMatrix1D parameters) {
        assertEquals(3, parameters.getNumberOfElements());
        final double a = parameters.getEntry(0);
        final double b = parameters.getEntry(1);
        final double c = parameters.getEntry(2);
        return a * Math.sin(b * xy.getFirst() + c * xy.getSecond()) + Math.cos(xy.getSecond());
      }

      @Override
      public int getNumberOfParameters() {
        return 3;
      }
    };

    final ParameterizedFunction<DoublesPair, DoubleMatrix1D, DoubleMatrix1D> parmSense = new ParameterizedFunction<DoublesPair, DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(final DoublesPair xy, final DoubleMatrix1D parameters) {
        final double a = parameters.getEntry(0);
        final double b = parameters.getEntry(1);
        final double c = parameters.getEntry(2);
        final DoubleMatrix1D res = new DoubleMatrix1D(Math.sin(b * xy.getFirst() + c * xy.getSecond()), xy.getFirst() * a * Math.cos(b * xy.getFirst() + c * xy.getSecond()), xy.getSecond() * a *
            Math.cos(b * xy.getFirst() + c * xy.getSecond()));
        return res;
      }

      @Override
      public int getNumberOfParameters() {
        return 3;
      }
    };

    final DoubleMatrix1D params = new DoubleMatrix1D(0.7, -0.3, 1.2);
    final Function1D<DoublesPair, DoubleMatrix1D> paramsSenseFD = testSurface.getZParameterSensitivity(params);
    final Function1D<DoublesPair, DoubleMatrix1D> paramsSenseAnal = parmSense.asFunctionOfArguments(params);

    for (int i = 0; i < 20; i++) {
      final double x = Math.PI * (-0.5 + i / 19.);
      for (int j = 0; j < 20; j++) {
        final double y = Math.PI * (-0.5 + j / 19.);
        final DoublesPair xy = DoublesPair.of(x, y);
        final DoubleMatrix1D s1 = paramsSenseAnal.evaluate(xy);
        final DoubleMatrix1D s2 = paramsSenseFD.evaluate(xy);
        for (int k = 0; k < 3; k++) {
          assertEquals(s1.getEntry(k), s2.getEntry(k), 1e-10);
        }
      }
    }

  }
}
