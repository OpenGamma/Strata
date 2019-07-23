/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Version}.
 */
public class VersionTest {

  @Test
  public void test_version() {
    assertThat(Version.getVersionString()).isNotEmpty();
    // this line fails when tests are run in IntelliJ (works in Eclipse)
    // assertEquals(Version.getVersionString().contains("$"), false);
  }

}
