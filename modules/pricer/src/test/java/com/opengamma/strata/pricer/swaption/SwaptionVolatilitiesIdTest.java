/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SwaptionVolatilitiesId}.
 */
@Test
public class SwaptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    assertEquals(test.getName(), SwaptionVolatilitiesName.of("Foo"));
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
    assertEquals(test.getMarketDataName(), SwaptionVolatilitiesName.of("Foo"));
  }

  public void test_of_object() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of(SwaptionVolatilitiesName.of("Foo"));
    assertEquals(test.getName(), SwaptionVolatilitiesName.of("Foo"));
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
    assertEquals(test.getMarketDataName(), SwaptionVolatilitiesName.of("Foo"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    coverImmutableBean(test);
    SwaptionVolatilitiesId test2 = SwaptionVolatilitiesId.of("Bar");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    assertSerialization(test);
  }

}
