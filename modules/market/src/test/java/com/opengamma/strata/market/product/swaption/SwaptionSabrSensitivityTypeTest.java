/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.product.swaption;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SwaptionSabrSensitivityType}.
 */
@Test
public class SwaptionSabrSensitivityTypeTest {

  public void test_basics() {
    assertEquals(SwaptionSabrSensitivityType.ALPHA.name(), "ALPHA");
    assertEquals(SwaptionSabrSensitivityType.ALPHA.toString(), "Alpha");
    assertEquals(SwaptionSabrSensitivityType.BETA.toString(), "Beta");
  }

  public void coverage() {
    coverEnum(SwaptionSabrSensitivityType.class);
  }

}
