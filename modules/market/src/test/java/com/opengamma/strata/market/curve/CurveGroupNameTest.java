/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveGroupName}.
 */
@Test
public class CurveGroupNameTest {

  public void coverage() {
    CurveGroupName test = CurveGroupName.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
