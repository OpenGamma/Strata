/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link Version}.
 */
@Test
public class VersionTest {

  public void test_version() {
    assertEquals(Version.getVersionString().isEmpty(), false);
    assertEquals(Version.getVersionString().contains("$"), false);
  }

}
