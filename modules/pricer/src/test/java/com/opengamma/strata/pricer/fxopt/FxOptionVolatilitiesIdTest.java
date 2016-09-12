/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link FxOptionVolatilitiesId}.
 */
@Test
public class FxOptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    assertEquals(test.getName(), FxOptionVolatilitiesName.of("Foo"));
    assertEquals(test.getMarketDataType(), FxOptionVolatilities.class);
    assertEquals(test.getMarketDataName(), FxOptionVolatilitiesName.of("Foo"));
  }

  public void test_of_object() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of(FxOptionVolatilitiesName.of("Foo"));
    assertEquals(test.getName(), FxOptionVolatilitiesName.of("Foo"));
    assertEquals(test.getMarketDataType(), FxOptionVolatilities.class);
    assertEquals(test.getMarketDataName(), FxOptionVolatilitiesName.of("Foo"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    coverImmutableBean(test);
    FxOptionVolatilitiesId test2 = FxOptionVolatilitiesId.of("Bar");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    assertSerialization(test);
  }

}
