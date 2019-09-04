/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test.
 */
public class FunctionUtilsTest {
  private static final double EPS = 1e-15;

  @Test
  @SuppressWarnings("deprecation")
  public void testSquare() {
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertThat(FunctionUtils.square(x)).isCloseTo(x * x, offset(EPS));
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testCube() {
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertThat(FunctionUtils.cube(x)).isCloseTo(x * x * x, offset(EPS));
    }
  }

  @Test
  public void testNullIndices() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FunctionUtils.toTensorIndex(null, new int[] {1, 2, 3}));
  }

  @Test
  public void testNullDimensions1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FunctionUtils.toTensorIndex(new int[] {1, 2, 3}, null));
  }

  @Test
  public void testWrongLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FunctionUtils.toTensorIndex(new int[] {1, 2}, new int[] {1, 2, 3}));
  }

  @Test
  public void testNullDimensions2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FunctionUtils.fromTensorIndex(2, null));
  }

  @Test
  public void testTensorIndexTest1() {

    final int[] indices = new int[] {2};
    final int[] dimensions = new int[] {5};
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    assertThat(indices[0]).isEqualTo(index);

    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertThat(indices[0]).isEqualTo(res[0]);

  }

  @Test
  public void testTensorIndexTest2() {

    final int[] indices = new int[] {2, 3};
    final int[] dimensions = new int[] {5, 7};
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertThat(indices[0]).isEqualTo(res[0]);
    assertThat(indices[1]).isEqualTo(res[1]);
  }

  @Test
  public void testTensorIndexTest3() {

    final int[] indices = new int[] {2, 3, 1};
    final int[] dimensions = new int[] {5, 7, 3};
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertThat(indices[0]).isEqualTo(res[0]);
    assertThat(indices[1]).isEqualTo(res[1]);
    assertThat(indices[2]).isEqualTo(res[2]);
  }

  @Test
  public void testOutOfBounds() {
    final int[] indices = new int[] {2, 7, 1};
    final int[] dimensions = new int[] {5, 7, 3};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FunctionUtils.toTensorIndex(indices, dimensions));
  }

  @Test
  public void getLowerBoundIndexTest() {
    int i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1.), -0.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2.), -0.);
    assertThat(i).isEqualTo(0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), 2.5);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), 2.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), -2.);
    assertThat(i).isEqualTo(0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), -0.);
    assertThat(i).isEqualTo(2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), 0.);
    assertThat(i).isEqualTo(2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), -0.);
    assertThat(i).isEqualTo(2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., -0.), -0.);
    assertThat(i).isEqualTo(2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., 0., 1.), -0.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., 0., 1.), 0.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., -0., 1.), 0.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., -0., 1.), -0.);
    assertThat(i).isEqualTo(1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(0., 1., 2.), -0.);
    assertThat(i).isEqualTo(0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(0., 1., 2.), 0.);
    assertThat(i).isEqualTo(0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-0., 1., 2.), 0.);
    assertThat(i).isEqualTo(0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-0., 1., 2.), -0.);
    assertThat(i).isEqualTo(0);
  }
}
