/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ExplainKey}.
 */
@Test
public class ExplainKeyTest {

  //-----------------------------------------------------------------------
  public void coverage() {
    ExplainKey<String> test = ExplainKey.of("Foo");
    assertEquals(test.toString(), "Foo");
    assertSerialization(test);
    assertJodaConvert(ExplainKey.class, test);
  }

}
