/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class LeastSquaresRegressionTest {

  @Test
  public void test() {
    final LeastSquaresRegression regression = new OrdinaryLeastSquaresRegression();
    try {
      regression.checkData(null, null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    double[][] x = new double[0][0];
    try {
      regression.checkData(x, null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    double[] y = new double[0];
    try {
      regression.checkData(x, (double[]) null, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    x = new double[1][2];
    y = new double[3];
    try {
      regression.checkData(x, (double[]) null, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    x = new double[][] {new double[] {1., 2., 3. }, new double[] {4., 5. }, new double[] {6., 7., 8. }, new double[] {9., 0., 0. } };
    try {
      regression.checkData(x, (double[]) null, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    x[1] = new double[] {4., 5., 6. };
    try {
      regression.checkData(x, (double[]) null, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    y = new double[] {1., 2., 3., 4. };
    double[] w1 = new double[0];
    try {
      regression.checkData(x, w1, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    double[][] w = new double[0][0];
    try {
      regression.checkData(x, w, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    w1 = new double[3];
    try {
      regression.checkData(x, w1, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    w = new double[3][0];
    try {
      regression.checkData(x, w, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    w = new double[][] {new double[] {1., 2., 3. }, new double[] {4., 5. }, new double[] {6., 7., 8. }, new double[] {9., 0., 0. } };
    try {
      regression.checkData(x, w, y);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }
}
