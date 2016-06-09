/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.model;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SabrParameterType}.
 */
@Test
public class SabrParameterTypeTest {

  public void test_basics() {
    assertEquals(SabrParameterType.ALPHA.name(), "ALPHA");
    assertEquals(SabrParameterType.ALPHA.toString(), "Alpha");
    assertEquals(SabrParameterType.BETA.toString(), "Beta");
  }

  public void coverage() {
    coverEnum(SabrParameterType.class);
  }

}
