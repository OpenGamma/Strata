/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.BitSet;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform.LimitType;

/**
 * Test.
 */
public class NonLinearTransformFunctionTest {

  private static final ParameterLimitsTransform[] NULL_TRANSFORMS;
  private static final ParameterLimitsTransform[] TRANSFORMS;

  private static final Function<DoubleArray, DoubleArray> FUNCTION = new Function<DoubleArray, DoubleArray>() {
    @Override
    public DoubleArray apply(DoubleArray x) {
      ArgChecker.isTrue(x.size() == 2);
      double x1 = x.get(0);
      double x2 = x.get(1);
      return DoubleArray.of(
          Math.sin(x1) * Math.cos(x2),
          Math.sin(x1) * Math.sin(x2),
          Math.cos(x1));
    }
  };

  private static final Function<DoubleArray, DoubleMatrix> JACOBIAN = new Function<DoubleArray, DoubleMatrix>() {
    @Override
    public DoubleMatrix apply(DoubleArray x) {
      ArgChecker.isTrue(x.size() == 2);
      double x1 = x.get(0);
      double x2 = x.get(1);
      double[][] y = new double[3][2];
      y[0][0] = Math.cos(x1) * Math.cos(x2);
      y[0][1] = -Math.sin(x1) * Math.sin(x2);
      y[1][0] = Math.cos(x1) * Math.sin(x2);
      y[1][1] = Math.sin(x1) * Math.cos(x2);
      y[2][0] = -Math.sin(x1);
      y[2][1] = 0;
      return DoubleMatrix.copyOf(y);
    }
  };

  static {
    NULL_TRANSFORMS = new ParameterLimitsTransform[2];
    NULL_TRANSFORMS[0] = new NullTransform();
    NULL_TRANSFORMS[1] = new NullTransform();

    TRANSFORMS = new ParameterLimitsTransform[2];
    TRANSFORMS[0] = new DoubleRangeLimitTransform(0, Math.PI);
    TRANSFORMS[1] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
  }

  @Test
  public void testNullTransform() {
    BitSet fixed = new BitSet();
    fixed.set(0);
    DoubleArray start = DoubleArray.of(Math.PI / 4, 1);
    UncoupledParameterTransforms transforms = new UncoupledParameterTransforms(start, NULL_TRANSFORMS, fixed);
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(FUNCTION, JACOBIAN, transforms);
    Function<DoubleArray, DoubleArray> func = transFunc.getFittingFunction();
    Function<DoubleArray, DoubleMatrix> jacFunc = transFunc.getFittingJacobian();

    DoubleArray x = DoubleArray.of(0.5);
    final double rootHalf = Math.sqrt(0.5);
    DoubleArray y = func.apply(x);
    assertThat(y.size()).isEqualTo(3);
    assertThat(y.get(0)).isCloseTo(rootHalf * Math.cos(0.5), offset(1e-9));
    assertThat(y.get(1)).isCloseTo(rootHalf * Math.sin(0.5), offset(1e-9));
    assertThat(rootHalf).isCloseTo(y.get(2), offset(1e-9));

    DoubleMatrix jac = jacFunc.apply(x);
    assertThat(jac.rowCount()).isEqualTo(3);
    assertThat(jac.columnCount()).isEqualTo(1);
    assertThat(jac.get(0, 0)).isCloseTo(-rootHalf * Math.sin(0.5), offset(1e-9));
    assertThat(jac.get(1, 0)).isCloseTo(rootHalf * Math.cos(0.5), offset(1e-9));
    assertThat(jac.get(2, 0)).isCloseTo(0, offset(1e-9));
  }

  @Test
  public void testNonLinearTransform() {
    BitSet fixed = new BitSet();
    DoubleArray start = DoubleArray.filled(2);
    UncoupledParameterTransforms transforms = new UncoupledParameterTransforms(start, TRANSFORMS, fixed);
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(FUNCTION, JACOBIAN, transforms);
    Function<DoubleArray, DoubleArray> func = transFunc.getFittingFunction();
    Function<DoubleArray, DoubleMatrix> jacFunc = transFunc.getFittingJacobian();

    VectorFieldFirstOrderDifferentiator diff = new VectorFieldFirstOrderDifferentiator();
    Function<DoubleArray, DoubleMatrix> jacFuncFD = diff.differentiate(func);

    DoubleArray testPoint = DoubleArray.of(4.5, -2.1);
    DoubleMatrix jac = jacFunc.apply(testPoint);
    DoubleMatrix jacFD = jacFuncFD.apply(testPoint);
    assertThat(jac.rowCount()).isEqualTo(3);
    assertThat(jac.columnCount()).isEqualTo(2);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 2; j++) {
        assertThat(jac.get(i, j)).isCloseTo(jacFD.get(i, j), offset(1e-6));
      }
    }
  }
}
