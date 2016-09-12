/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class GoldenSectionMinimizer1DTest extends Minimizer1DTestCase {
  private static final ScalarMinimizer MINIMIZER = new GoldenSectionMinimizer1D();

  @Test
  public void test() {
    super.assertInputs(MINIMIZER);
    super.assertMinimizer(MINIMIZER);
  }
}
