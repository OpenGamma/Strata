/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test.
 */
@Test
public class ParameterizedFunctionTest {

  private static ParameterizedFunction<Double, double[], Double> ARRAY_PARAMS = new ParameterizedFunction<Double, double[], Double>() {

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

  private static ParameterizedFunction<Double, DoubleArray, Double> VECTOR_PARAMS = new ParameterizedFunction<Double, DoubleArray, Double>() {

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
    final double[] parms = new double[] {3.0, -1.0, 1.0, 1.0 };
    assertEquals(13.0, ARRAY_PARAMS.evaluate(2.0, parms), 0.0);

    final Function1D<Double, Double> func = ARRAY_PARAMS.asFunctionOfArguments(parms);
    assertEquals(4.0, func.evaluate(-1.0), 0.0);

    final Function1D<double[], Double> param_func = ARRAY_PARAMS.asFunctionOfParameters(0.0);
    assertEquals(10.0, param_func.evaluate(new double[] {10, 312, 423, 534 }), 0.0);
  }

  @Test
  public void testSin() {
    final DoubleArray parms = DoubleArray.of(-1.0, 0.5);
    assertEquals(-Math.sin(1.0), VECTOR_PARAMS.evaluate(2.0, parms), 0.0);

    final Function1D<Double, Double> func = VECTOR_PARAMS.asFunctionOfArguments(parms);
    assertEquals(1.0, func.evaluate(-Math.PI), 0.0);

    final ScalarFirstOrderDifferentiator diff = new ScalarFirstOrderDifferentiator();
    final Function1D<Double, Double> grad = diff.differentiate(func);
    assertEquals(-0.5, grad.evaluate(0.0), 1e-8);

    final Function1D<DoubleArray, Double> params_func = VECTOR_PARAMS.asFunctionOfParameters(1.0);
    final ScalarFieldFirstOrderDifferentiator vdiff = new ScalarFieldFirstOrderDifferentiator();
    final Function1D<DoubleArray, DoubleArray> vgrad = vdiff.differentiate(params_func);
    final DoubleArray res = vgrad.evaluate(DoubleArray.of(Math.PI, 0));
    assertEquals(0.0, res.get(0), 1e-8);
    assertEquals(Math.PI, res.get(1), 1e-8);
  }
}
