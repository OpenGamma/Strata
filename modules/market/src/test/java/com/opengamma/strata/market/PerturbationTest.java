/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link Perturbation}.
 */
@Test
public class PerturbationTest {

  public void test_none() {
    String str = "Foo";
    assertEquals(Perturbation.none().applyTo(str), str);
  }

}
