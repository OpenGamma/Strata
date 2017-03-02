/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Construct a curve a + b*x + c*x^2 (where a, b, and c are the parameters), then make some VectorFunctions
 * that sample the curve at some values of x, thus providing a mapping from the model parameters to the curve
 * value at the sample positions. 
 */
@Test
public class DoublesVectorFunctionProviderTest {

  @Test
  public void test() {
    final ParameterizedCurve curve = new ParameterizedCurve() {

      @Override
      public Double evaluate(final Double x, final DoubleArray parameters) {
        return parameters.get(0) + parameters.get(1) * x + parameters.get(2) * x * x;
      }

      @Override
      public int getNumberOfParameters() {
        return 3;
      }
    };

    final DoublesVectorFunctionProvider pro = new DoublesVectorFunctionProvider() {
      @Override
      public VectorFunction from(final double[] x) {
        final ParameterizedCurveVectorFunction vf = new ParameterizedCurveVectorFunction(x, curve);
        return vf;
      }
    };

    //a = -2, b = 1, c = 0.5
    final DoubleArray parms = DoubleArray.of(-2.0, 1.0, 0.5);

    //sample the curve at x = -1, 0, and 1 
    VectorFunction f = pro.from(new Double[] {-1.0, 0.0, 1.0 });
    DoubleArray y = f.apply(parms);
    AssertMatrix.assertEqualsVectors(DoubleArray.of(-2.5, -2.0, -0.5), y, 1e-15);

    final List<Double> l = new ArrayList<>(3);
    l.add(0.0);
    l.add(2.0);
    l.add(4.0);
    f = pro.from(l);
    y = f.apply(parms);
    AssertMatrix.assertEqualsVectors(DoubleArray.of(-2.0, 2.0, 10.0), y, 1e-15);
  }

}
