/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class LeastSquareResultsTest {
  private static final DoubleArray PARAMS = DoubleArray.of(1.0, 2.0);
  private static final DoubleMatrix COVAR = DoubleMatrix.copyOf(new double[][] { {0.1, 0.2}, {0.2, 0.3}});
  private static final DoubleMatrix INV_JAC = DoubleMatrix.copyOf(new double[][] { {0.5, 0.6}, {0.7, 0.8}});

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeChiSq1() {
    new LeastSquareResults(-1, PARAMS, COVAR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullParams1() {
    new LeastSquareResults(1, null, COVAR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCovar1() {
    new LeastSquareResults(1, PARAMS, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullWrongSize1() {
    new LeastSquareResults(1, DoubleArray.of(1.2), COVAR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotSquare1() {
    new LeastSquareResults(1, PARAMS, DoubleMatrix.copyOf(new double[][] {{0.2, 0.3}}));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeChiSq2() {
    new LeastSquareResults(-1, PARAMS, COVAR, INV_JAC);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullParams2() {
    new LeastSquareResults(1, null, COVAR, INV_JAC);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCovar2() {
    new LeastSquareResults(1, PARAMS, null, INV_JAC);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullWrongSize2() {
    new LeastSquareResults(1, DoubleArray.of(1.2), COVAR, INV_JAC);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNotSquare2() {
    new LeastSquareResults(1, PARAMS, DoubleMatrix.copyOf(new double[][] {{0.2, 0.3}}), INV_JAC);
  }

  @Test
  public void testRecall() {
    final double chiSq = 12.46;
    LeastSquareResults res = new LeastSquareResults(chiSq, PARAMS, COVAR);
    assertEquals(chiSq, res.getChiSq(), 0.0);
    for (int i = 0; i < 2; i++) {
      assertEquals(PARAMS.get(i), res.getFitParameters().get(i), 0);
      for (int j = 0; j < 2; j++) {
        assertEquals(COVAR.get(i, j), res.getCovariance().get(i, j), 0);
      }
    }
    res = new LeastSquareResults(chiSq, PARAMS, COVAR, INV_JAC);
    assertEquals(chiSq, res.getChiSq(), 0.0);
    for (int i = 0; i < 2; i++) {
      assertEquals(PARAMS.get(i), res.getFitParameters().get(i), 0);
      for (int j = 0; j < 2; j++) {
        assertEquals(COVAR.get(i, j), res.getCovariance().get(i, j), 0);
        assertEquals(INV_JAC.get(i, j), res.getFittingParameterSensitivityToData().get(i, j), 0);
      }
    }
  }

  @Test
  public void testHashCode() {
    LeastSquareResults ls1 = new LeastSquareResults(1.0, PARAMS, COVAR);
    LeastSquareResults ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] { {0.1, 0.2}, {0.2, 0.3}}));
    assertEquals(ls1.hashCode(), ls2.hashCode(), 0);
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] { {0.1, 0.2}, {0.2, 0.3}}), null);
    assertEquals(ls1.hashCode(), ls2.hashCode(), 0);
    ls1 = new LeastSquareResults(1.0, PARAMS, COVAR, INV_JAC);
    ls2 = new LeastSquareResults(1.0,
        DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] { {0.1, 0.2}, {0.2, 0.3}}),
        DoubleMatrix.copyOf(new double[][] { {0.5, 0.6}, {0.7, 0.8}}));
    assertEquals(ls1.hashCode(), ls2.hashCode(), 0);
  }

  @Test
  public void testEquals() {
    LeastSquareResults ls1 = new LeastSquareResults(1.0, PARAMS, COVAR);
    LeastSquareResults ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] { {0.1, 0.2}, {0.2, 0.3}}));
    assertEquals(ls1, ls2);
    ls2 = new LeastSquareResults(1.0, PARAMS, COVAR, null);
    assertEquals(ls1, ls2);
    ls2 = new LeastSquareResults(1.1, PARAMS, COVAR);
    assertFalse(ls1.equals(ls2));
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.1, 2.0), DoubleMatrix.copyOf(new double[][] {
      {0.1, 0.2 }, {0.2, 0.3 } }));
    assertFalse(ls1.equals(ls2));
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0), DoubleMatrix.copyOf(new double[][] {
      {0.1, 0.2 }, {0.2, 0.4 } }));
    assertFalse(ls1.equals(ls2));
    ls2 = new LeastSquareResults(1.0, PARAMS, COVAR, INV_JAC);
    assertFalse(ls1.equals(ls2));
    ls1 = new LeastSquareResults(1, PARAMS, COVAR, INV_JAC);
    ls2 = new LeastSquareResults(1, PARAMS, COVAR, COVAR);
    assertFalse(ls1.equals(ls2));
  }

}
