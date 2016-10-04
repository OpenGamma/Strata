/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link RepoGroup}.
 */
@Test
public class RepoGroupTest {

  public void coverage() {
    RepoGroup test = RepoGroup.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
