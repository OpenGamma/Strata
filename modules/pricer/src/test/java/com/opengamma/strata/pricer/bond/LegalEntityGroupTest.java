/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link LegalEntityGroup}.
 */
@Test
public class LegalEntityGroupTest {

  public void coverage() {
    LegalEntityGroup test = LegalEntityGroup.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
