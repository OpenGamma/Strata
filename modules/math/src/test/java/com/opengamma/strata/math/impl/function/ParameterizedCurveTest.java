/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

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
      public Double evaluate(final Double x, final DoubleArray parameters) {
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

    ParameterizedFunction<Double, DoubleArray, DoubleArray> parmSense =
        new ParameterizedFunction<Double, DoubleArray, DoubleArray>() {

          @Override
          public DoubleArray evaluate(Double x, DoubleArray parameters) {
            double a = parameters.get(0);
            double b = parameters.get(1);
            DoubleArray res = DoubleArray.of(Math.sin(b * x), x * a * Math.cos(b * x), 1.0);
            return res;
          }

          @Override
          public int getNumberOfParameters() {
            return 0;
          }
        };

    DoubleArray params = DoubleArray.of(0.7, -0.3, 1.2);
    Function<Double, DoubleArray> paramsSenseFD = testCurve.getYParameterSensitivity(params);
    Function<Double, DoubleArray> paramsSenseAnal = parmSense.asFunctionOfArguments(params);

    for (int i = 0; i < 20; i++) {
      double x = Math.PI * (-0.5 + i / 19.);
      DoubleArray s1 = paramsSenseAnal.apply(x);
      DoubleArray s2 = paramsSenseFD.apply(x);
      for (int j = 0; j < 3; j++) {
        assertEquals(s1.get(j), s2.get(j), 1e-10);
      }
    }

  }

}
