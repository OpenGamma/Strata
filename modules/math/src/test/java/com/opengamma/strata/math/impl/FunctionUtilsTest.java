/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test.
 */
@Test
public class FunctionUtilsTest {
  private static final double EPS = 1e-15;

  @Test
  public void testSquare() {
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertEquals(FunctionUtils.square(x), x * x, EPS);
    }
  }

  @Test
  public void testCube() {
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertEquals(FunctionUtils.cube(x), x * x * x, EPS);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullIndices() {
    FunctionUtils.toTensorIndex(null, new int[] {1, 2, 3 });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDimensions1() {
    FunctionUtils.toTensorIndex(new int[] {1, 2, 3 }, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongLength() {
    FunctionUtils.toTensorIndex(new int[] {1, 2 }, new int[] {1, 2, 3 });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDimensions2() {
    FunctionUtils.fromTensorIndex(2, null);
  }

  @Test
  public void testTensorIndexTest1() {

    final int[] indices = new int[] {2 };
    final int[] dimensions = new int[] {5 };
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    assertEquals(indices[0], index, 0);

    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertEquals(indices[0], res[0], 0);

  }

  @Test
  public void testTensorIndexTest2() {

    final int[] indices = new int[] {2, 3 };
    final int[] dimensions = new int[] {5, 7 };
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertEquals(indices[0], res[0], 0);
    assertEquals(indices[1], res[1], 0);
  }

  @Test
  public void testTensorIndexTest3() {

    final int[] indices = new int[] {2, 3, 1 };
    final int[] dimensions = new int[] {5, 7, 3 };
    final int index = FunctionUtils.toTensorIndex(indices, dimensions);
    final int[] res = FunctionUtils.fromTensorIndex(index, dimensions);
    assertEquals(indices[0], res[0], 0);
    assertEquals(indices[1], res[1], 0);
    assertEquals(indices[2], res[2], 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfBounds() {
    final int[] indices = new int[] {2, 7, 1 };
    final int[] dimensions = new int[] {5, 7, 3 };
    FunctionUtils.toTensorIndex(indices, dimensions);
  }

  @Test
  public void getLowerBoundIndexTest() {
    int i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1.), -0.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2.), -0.);
    assertEquals(i, 0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), 2.5);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), 2.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(1., 2., 3.), -2.);
    assertEquals(i, 0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), -0.);
    assertEquals(i, 2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), 0.);
    assertEquals(i, 2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., 0.), -0.);
    assertEquals(i, 2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-2., -1., -0.), -0.);
    assertEquals(i, 2);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., 0., 1.), -0.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., 0., 1.), 0.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., -0., 1.), 0.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-1., -0., 1.), -0.);
    assertEquals(i, 1);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(0., 1., 2.), -0.);
    assertEquals(i, 0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(0., 1., 2.), 0.);
    assertEquals(i, 0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-0., 1., 2.), 0.);
    assertEquals(i, 0);
    i = FunctionUtils.getLowerBoundIndex(DoubleArray.of(-0., 1., 2.), -0.);
    assertEquals(i, 0);
  }
}
