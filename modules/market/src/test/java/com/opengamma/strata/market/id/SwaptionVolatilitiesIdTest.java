/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.view.SwaptionVolatilities;

/**
 * Test {@link SwaptionVolatilitiesId}.
 */
@Test
public class SwaptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    assertEquals(test.getName(), "Foo");
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
    assertEquals(test.toMarketDataId(null), test);
    assertEquals(test.toMarketDataKey(), test);
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
