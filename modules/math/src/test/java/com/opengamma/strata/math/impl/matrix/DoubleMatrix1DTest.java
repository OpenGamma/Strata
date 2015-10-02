/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class DoubleMatrix1DTest {
  private static final DoubleMatrix1D PRIMITIVES = new DoubleMatrix1D(new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
  private static final DoubleMatrix1D OBJECTS = new DoubleMatrix1D(new Double[] {1., 2., 3., 4., 5., 6., 7., 8., 9., 10. });

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullPrimitiveArray() {
    new DoubleMatrix1D((double[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectArray() {
    new DoubleMatrix1D((Double[]) null);
  }

  @Test
  public void testEmptyArray() {
    final DoubleMatrix1D d = new DoubleMatrix1D(new double[0]);
    assertTrue(Arrays.equals(new double[0], d.getData()));
  }

  @Test
  public void testArrays() {
    final int n = 10;
    double[] x = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = i;
    }
    DoubleMatrix1D d = new DoubleMatrix1D(x);
    assertEquals(d.getNumberOfElements(), n);
    final double[] y = d.getData();
    for (int i = 0; i < n; i++) {
      assertEquals(x[i], y[i], 1e-15);
    }
    for (int i = 0; i < n; i++) {
      y[i] = Double.valueOf(i);
    }
    d = new DoubleMatrix1D(y);
    x = d.getData();
    for (int i = 0; i < n; i++) {
      assertEquals(x[i], y[i], 0);
      assertEquals(x[i], d.getEntry(i), 0);
    }
  }

  @Test
  public void testEqualsAndHashCode() {
    double[] primitives = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    Double[] objects = new Double[] {1., 2., 3., 4., 5., 6., 7., 8., 9., 10. };
    assertEquals(PRIMITIVES, new DoubleMatrix1D(primitives));
    assertEquals(PRIMITIVES, new DoubleMatrix1D(objects));
    assertEquals(OBJECTS, OBJECTS);
    assertEquals(PRIMITIVES.hashCode(), new DoubleMatrix1D(primitives).hashCode());
    assertEquals(PRIMITIVES.hashCode(), new DoubleMatrix1D(objects).hashCode());
    assertEquals(OBJECTS.hashCode(), OBJECTS.hashCode());
    primitives = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 11 };
    objects = new Double[] {1., 2., 3., 4., 5., 6., 7., 8., 9., 11. };
    assertFalse(PRIMITIVES.equals(new DoubleMatrix1D(primitives)));
    assertFalse(OBJECTS.equals(new DoubleMatrix1D(objects)));
  }
}
