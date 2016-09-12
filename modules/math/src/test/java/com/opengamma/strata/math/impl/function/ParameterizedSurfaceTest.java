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
import com.opengamma.strata.collect.tuple.DoublesPair;

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
      public Double evaluate(final DoublesPair xy, final DoubleArray parameters) {
        assertEquals(3, parameters.size());
        final double a = parameters.get(0);
        final double b = parameters.get(1);
        final double c = parameters.get(2);
        return a * Math.sin(b * xy.getFirst() + c * xy.getSecond()) + Math.cos(xy.getSecond());
      }

      @Override
      public int getNumberOfParameters() {
        return 3;
      }
    };

    final ParameterizedFunction<DoublesPair, DoubleArray, DoubleArray> parmSense =
        new ParameterizedFunction<DoublesPair, DoubleArray, DoubleArray>() {

          @Override
          public DoubleArray evaluate(final DoublesPair xy, final DoubleArray parameters) {
            double a = parameters.get(0);
            double b = parameters.get(1);
            double c = parameters.get(2);
            DoubleArray res = DoubleArray.of(
                Math.sin(b * xy.getFirst() + c * xy.getSecond()),
                xy.getFirst() * a * Math.cos(b * xy.getFirst() + c * xy.getSecond()), xy.getSecond() * a *
                    Math.cos(b * xy.getFirst() + c * xy.getSecond()));
            return res;
          }

          @Override
          public int getNumberOfParameters() {
            return 3;
          }
        };

    final DoubleArray params = DoubleArray.of(0.7, -0.3, 1.2);
    final Function<DoublesPair, DoubleArray> paramsSenseFD = testSurface.getZParameterSensitivity(params);
    final Function<DoublesPair, DoubleArray> paramsSenseAnal = parmSense.asFunctionOfArguments(params);

    for (int i = 0; i < 20; i++) {
      final double x = Math.PI * (-0.5 + i / 19.);
      for (int j = 0; j < 20; j++) {
        final double y = Math.PI * (-0.5 + j / 19.);
        final DoublesPair xy = DoublesPair.of(x, y);
        final DoubleArray s1 = paramsSenseAnal.apply(xy);
        final DoubleArray s2 = paramsSenseFD.apply(xy);
        for (int k = 0; k < 3; k++) {
          assertEquals(s1.get(k), s2.get(k), 1e-10);
        }
      }
    }

  }
}
