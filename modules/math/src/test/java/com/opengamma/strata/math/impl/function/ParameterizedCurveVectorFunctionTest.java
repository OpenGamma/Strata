/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Test simple a simple function a * Math.sinh(b * x)
 */
public class ParameterizedCurveVectorFunctionTest {

  private static final ParameterizedCurve PCURVE;

  static {
    PCURVE = new ParameterizedCurve() {

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
    final ParameterizedCurveVectorFunctionProvider pro = new ParameterizedCurveVectorFunctionProvider(PCURVE);
    final double[] points = new double[] {-1.0, 0.0, 1.0};
    final VectorFunction f = pro.from(points);
    assertThat(2).isEqualTo(f.getLengthOfDomain());
    assertThat(3).isEqualTo(f.getLengthOfRange());
    final DoubleArray x = DoubleArray.of(0.5, 2.0); //the parameters a & b
    final DoubleArray y = f.apply(x);
    assertThat(0.5 * Math.sinh(-2.0)).isCloseTo(y.get(0), offset(1e-14));
    assertThat(0.0).isCloseTo(y.get(1), offset(1e-14));
    assertThat(0.5 * Math.sinh(2.0)).isCloseTo(y.get(2), offset(1e-14));

    final DoubleMatrix jac = f.calculateJacobian(x);
    final DoubleMatrix fdJac = (new VectorFieldFirstOrderDifferentiator().differentiate(f)).apply(x);
    AssertMatrix.assertEqualsMatrix(fdJac, jac, 1e-9);
  }
}
