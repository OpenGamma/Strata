/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test.
 */
public class TransformParametersTest {
  private static final DoubleArray INIT = DoubleArray.of(1, 2, 3, 4);
  private static final ParameterLimitsTransform[] NULLS =
      new ParameterLimitsTransform[] {new NullTransform(), new NullTransform(), new NullTransform(), new NullTransform()};
  private static final BitSet FIXED = new BitSet(4);
  private static final UncoupledParameterTransforms PARAMS;

  static {
    FIXED.set(0);
    PARAMS = new UncoupledParameterTransforms(INIT, NULLS, FIXED);
  }

  @Test
  public void testNullStartValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UncoupledParameterTransforms(null, NULLS, FIXED));
  }

  @Test
  public void testNullTransforms() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UncoupledParameterTransforms(INIT, null, FIXED));
  }

  @Test
  public void testEmptyTransforms() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UncoupledParameterTransforms(INIT, new ParameterLimitsTransform[0], FIXED));
  }

  @Test
  public void testNullBitSet() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UncoupledParameterTransforms(INIT, NULLS, null));
  }

  @Test
  public void testAllFixed() {
    final BitSet allFixed = new BitSet();
    allFixed.set(0);
    allFixed.set(1);
    allFixed.set(2);
    allFixed.set(3);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UncoupledParameterTransforms(INIT, NULLS, allFixed));
  }

  @Test
  public void testTransformNullParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.transform(null));
  }

  @Test
  public void testTransformWrongParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.transform(DoubleArray.of(1, 2)));
  }

  @Test
  public void testInverseTransformNullParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.inverseTransform(null));
  }

  @Test
  public void testInverseTransformWrongParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.inverseTransform(DoubleArray.of(1, 2)));
  }

  @Test
  public void testJacobianNullParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.jacobian(null));
  }

  @Test
  public void testJacobianWrongParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.jacobian(DoubleArray.of(1, 2)));
  }

  @Test
  public void testInverseJacobianNullParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.inverseJacobian(null));
  }

  @Test
  public void testInverseJacobianWrongParameters() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PARAMS.inverseJacobian(DoubleArray.of(1, 2)));
  }

  @Test
  public void test() {
    assertThat(PARAMS.getNumberOfModelParameters()).isEqualTo(4);
    assertThat(PARAMS.getNumberOfFittingParameters()).isEqualTo(3);
    UncoupledParameterTransforms other = new UncoupledParameterTransforms(INIT, NULLS, FIXED);
    assertThat(PARAMS).isEqualTo(other);
    assertThat(PARAMS.hashCode()).isEqualTo(other.hashCode());
    other = new UncoupledParameterTransforms(DoubleArray.of(1, 2, 4, 5), NULLS, FIXED);
    assertThat(other.equals(PARAMS)).isFalse();
    other = new UncoupledParameterTransforms(INIT, new ParameterLimitsTransform[] {new DoubleRangeLimitTransform(1, 2),
        new NullTransform(), new NullTransform(), new NullTransform()}, FIXED);
    assertThat(other.equals(PARAMS)).isFalse();
    other = new UncoupledParameterTransforms(INIT, NULLS, new BitSet(4));
    assertThat(other.equals(PARAMS)).isFalse();
  }

  @Test
  public void testTransformAndInverse() {
    final DoubleArray functionParameters = DoubleArray.of(1, 2, 6, 4);
    assertThat(PARAMS.inverseTransform(PARAMS.transform(functionParameters))).isEqualTo(functionParameters);
  }
}
