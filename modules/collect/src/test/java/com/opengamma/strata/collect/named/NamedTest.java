/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link Named}.
 */
@Test
public class NamedTest {

  public void test_of() {
    SampleNamed test = Named.of(SampleNamed.class, "Standard");
    assertEquals(test, SampleNameds.STANDARD);
    assertThrowsIllegalArg(() -> Named.of(SampleNamed.class, "Rubbish"));
  }

}
