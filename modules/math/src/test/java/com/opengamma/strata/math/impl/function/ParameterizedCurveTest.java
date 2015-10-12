/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Set up a simple parameterised curve
 * (based on the function a * Math.sin(b * x) + c, where a, b, & c are the parameters)
 * and check the finite difference sensitivity (the default behaviour of getYParameterSensitivity)
 * agrees with the analytic calculation for a range of points along the curve.
 */
@Test
public class ParameterizedCurveTest {

  @Test
  public void test() {
    /**
     * Take the form $y = a\sin(bx) + c$
     */
    final ParameterizedCurve testCurve = new ParameterizedCurve() {

      @Override
      public Double evaluate(final Double x, final DoubleMatrix1D parameters) {
        assertEquals(3, parameters.size());
        double a = parameters.get(0);
        double b = parameters.get(1);
        double c = parameters.get(2);
        return a * Math.sin(b * x) + c;
      }

      @Override
      public int getNumberOfParameters() {
        return 3;
      }

    };

    ParameterizedFunction<Double, DoubleMatrix1D, DoubleMatrix1D> parmSense =
        new ParameterizedFunction<Double, DoubleMatrix1D, DoubleMatrix1D>() {

          @Override
          public DoubleMatrix1D evaluate(Double x, DoubleMatrix1D parameters) {
            double a = parameters.get(0);
            double b = parameters.get(1);
            DoubleMatrix1D res = DoubleMatrix1D.of(Math.sin(b * x), x * a * Math.cos(b * x), 1.0);
            return res;
          }

          @Override
          public int getNumberOfParameters() {
            return 0;
          }
        };

    DoubleMatrix1D params = DoubleMatrix1D.of(0.7, -0.3, 1.2);
    Function1D<Double, DoubleMatrix1D> paramsSenseFD = testCurve.getYParameterSensitivity(params);
    Function1D<Double, DoubleMatrix1D> paramsSenseAnal = parmSense.asFunctionOfArguments(params);

    for (int i = 0; i < 20; i++) {
      double x = Math.PI * (-0.5 + i / 19.);
      DoubleMatrix1D s1 = paramsSenseAnal.evaluate(x);
      DoubleMatrix1D s2 = paramsSenseFD.evaluate(x);
      for (int j = 0; j < 3; j++) {
        assertEquals(s1.get(j), s2.get(j), 1e-10);
      }
    }

  }

}
