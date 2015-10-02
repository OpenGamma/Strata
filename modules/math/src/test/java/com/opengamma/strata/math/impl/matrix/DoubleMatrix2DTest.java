/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class DoubleMatrix2DTest {
  private static final DoubleMatrix2D PRIMITIVES = new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3, 4 }, new double[] {5, 6, 7, 8 }, new double[] {9, 10, 11, 12 } });
  private static final DoubleMatrix2D OBJECTS = new DoubleMatrix2D(new Double[][] {new Double[] {1., 2., 3., 4. }, new Double[] {5., 6., 7., 8. },
    new Double[] {9., 10., 11., 12. } });

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullPrimitiveArray() {
    new DoubleMatrix2D((double[][]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectArray() {
    new DoubleMatrix2D((Double[][]) null);
  }

  @Test
  public void testEmptyPrimitiveArray() {
    final DoubleMatrix2D d = new DoubleMatrix2D(new double[0][0]);
    final double[][] primitive = d.getData();
    assertEquals(primitive.length, 0);
    assertEquals(d.getNumberOfColumns(), 0);
    assertEquals(d.getNumberOfRows(), 0);
    assertEquals(d.getNumberOfElements(), 0);
  }

  @Test
  public void testEmptyObjectArray() {
    final DoubleMatrix2D d = new DoubleMatrix2D(new Double[0][0]);
    final double[][] primitive = d.getData();
    assertEquals(primitive.length, 0);
    assertEquals(d.getNumberOfColumns(), 0);
    assertEquals(d.getNumberOfRows(), 0);
    assertEquals(d.getNumberOfElements(), 0);
  }

  @Test
  public void testArrays() {
    final int n = 10;
    final int m = 30;
    double[][] x = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        x[i][j] = i * j;
      }
    }
    DoubleMatrix2D d = new DoubleMatrix2D(x);
    assertEquals(d.getNumberOfRows(), m);
    assertEquals(d.getNumberOfColumns(), n);
    assertEquals(d.getNumberOfElements(), m * n);
    final double[][] y = d.getData();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        assertEquals(x[i][j], y[i][j], 1e-15);
      }
    }
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        y[i][j] = Double.valueOf(i * j);
      }
    }
    d = new DoubleMatrix2D(y);
    x = d.getData();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        assertEquals(x[i][j], y[i][j], 0);
        assertEquals(x[i][j], d.getEntry(i, j), 0);
      }
    }
  }

  @Test
  public void testEqualsAndHashCode() {
    DoubleMatrix2D primitives = new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3, 4 }, new double[] {5, 6, 7, 8 }, new double[] {9, 10, 11, 12 } });
    DoubleMatrix2D objects = new DoubleMatrix2D(new Double[][] {new Double[] {1., 2., 3., 4. }, new Double[] {5., 6., 7., 8. }, new Double[] {9., 10., 11., 12. } });
    assertEquals(primitives, PRIMITIVES);
    assertEquals(objects, OBJECTS);
    assertEquals(PRIMITIVES, OBJECTS);
    assertEquals(primitives.hashCode(), PRIMITIVES.hashCode());
    assertEquals(objects.hashCode(), OBJECTS.hashCode());
    assertEquals(PRIMITIVES.hashCode(), OBJECTS.hashCode());
    primitives = new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3, 4 }, new double[] {5, 6, 7, 8 }, new double[] {9, 10, 11, 13 } });
    objects = new DoubleMatrix2D(new Double[][] {new Double[] {1., 2., 3., 4. }, new Double[] {5., 6., 7., 8. }, new Double[] {9., 10., 11., 13. } });
    assertFalse(primitives.equals(PRIMITIVES));
    assertFalse(objects.equals(OBJECTS));
  }

}
