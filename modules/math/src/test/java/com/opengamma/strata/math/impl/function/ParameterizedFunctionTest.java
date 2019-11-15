/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test.
 */
public class ParameterizedFunctionTest {

  private static ParameterizedFunction<Double, double[], Double> ARRAY_PARAMS =
      new ParameterizedFunction<Double, double[], Double>() {

        @Override
        public Double evaluate(final Double x, final double[] a) {
          final int n = a.length;
          double sum = 0.0;
          for (int i = n - 1; i > 0; i--) {
            sum += a[i];
            sum *= x;
          }
          sum += a[0];
          return sum;
        }

        @Override
        public int getNumberOfParameters() {
          return 0;
        }
      };

  private static ParameterizedFunction<Double, DoubleArray, Double> VECTOR_PARAMS =
      new ParameterizedFunction<Double, DoubleArray, Double>() {

        @Override
        public Double evaluate(final Double x, final DoubleArray a) {
          ArgChecker.notNull(a, "parameters");
          if (a.size() != 2) {
            throw new IllegalArgumentException("wrong number of parameters");
          }
          return a.get(0) * Math.sin(a.get(1) * x);
        }

        @Override
        public int getNumberOfParameters() {
          return 0;
        }
      };

  @Test
  public void testCubic() {
    final double[] parms = new double[] {3.0, -1.0, 1.0, 1.0};
    assertThat(13.0).isEqualTo(ARRAY_PARAMS.evaluate(2.0, parms));

    final Function<Double, Double> func = ARRAY_PARAMS.asFunctionOfArguments(parms);
    assertThat(4.0).isEqualTo(func.apply(-1.0));

    final Function<double[], Double> paramFunc = ARRAY_PARAMS.asFunctionOfParameters(0.0);
    assertThat(10.0).isEqualTo(paramFunc.apply(new double[] {10, 312, 423, 534}));
  }

  @Test
  public void testSin() {
    final DoubleArray parms = DoubleArray.of(-1.0, 0.5);
    assertThat(-Math.sin(1.0)).isEqualTo(VECTOR_PARAMS.evaluate(2.0, parms));

    final Function<Double, Double> func = VECTOR_PARAMS.asFunctionOfArguments(parms);
    assertThat(1.0).isEqualTo(func.apply(-Math.PI));

    final ScalarFirstOrderDifferentiator diff = new ScalarFirstOrderDifferentiator();
    final Function<Double, Double> grad = diff.differentiate(func);
    assertThat(-0.5).isCloseTo(grad.apply(0.0), offset(1e-8));

    final Function<DoubleArray, Double> paramFunc = VECTOR_PARAMS.asFunctionOfParameters(1.0);
    final ScalarFieldFirstOrderDifferentiator vdiff = new ScalarFieldFirstOrderDifferentiator();
    final Function<DoubleArray, DoubleArray> vgrad = vdiff.differentiate(paramFunc);
    final DoubleArray res = vgrad.apply(DoubleArray.of(Math.PI, 0));
    assertThat(0.0).isCloseTo(res.get(0), offset(1e-8));
    assertThat(Math.PI).isCloseTo(res.get(1), offset(1e-8));
  }
}
