/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class CdsPricingExampleTest {

  private static final double epsilon = 1e-9D;
  @Test
  public void test_the_example() {
    Assert.assertEquals(CdsPricingExample.calcPv(), 0.004943638574331999, epsilon);
  }
}
