/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance;

import com.opengamma.analytics.util.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class CdsPricingExampleTest {

  @Test
  public void test_the_example() {

    pvShouldBe(0.004943638574331999);

    ir01ParallelParShouldBe(963.5778398220427);

    cs01ParallelParShouldBe(51873.837977122515);

    ir01BucketedParShouldBe(
        -1.0080670211464167,
        3.1341894390061498,
        1.5775036020204425,
        12.721042910590768,
        52.718546616844830,
        138.10654797125608,
        206.87827731855214,
        275.05216884659603,
        257.43132639257240,
        17.147143614944070,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0
    );

    cs01BucketedParShouldBe(
        46.60949367238209,
        103.86381249362603,
        252.1060386202298,
        364.7911099907942,
        484.21517529617995,
        50640.11123423558
    );

  }

  //------------------------------------------------------------
  // Test Harness

  private static final double epsilon = 1e-9D;

  private void pvShouldBe(double expected) {
    Assert.assertEquals(CdsPricingExample.calcPv(), expected, epsilon);
  }

  private void ir01ParallelParShouldBe(double expected) {
    Assert.assertEquals(CdsPricingExample.calcIr01ParallelPar(), expected, epsilon);
  }

  private void cs01ParallelParShouldBe(double expected) {
    Assert.assertEquals(CdsPricingExample.calcCs01ParallelPar(), expected, epsilon);
  }

  private void ir01BucketedParShouldBe(double... expected) {
    checkVector(CdsPricingExample.calcIr01BucketedPar(), expected);
  }

  private void cs01BucketedParShouldBe(double... expected) {
    checkVector(CdsPricingExample.calcCs01BucketedPar(), expected);
  }

  private void checkVector(double[] result, double[] expected) {
    Double[] r = ArrayUtils.toObject(result);
    Double[] e = ArrayUtils.toObject(expected);
    Assert.assertEquals(r, e);
  }
}
