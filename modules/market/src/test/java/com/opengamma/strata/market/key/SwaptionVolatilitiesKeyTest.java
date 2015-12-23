/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.value.SwaptionVolatilities;

/**
 * Test {@link SwaptionVolatilitiesKey}.
 */
@Test
public class SwaptionVolatilitiesKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionVolatilitiesKey test = SwaptionVolatilitiesKey.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionVolatilitiesKey test = SwaptionVolatilitiesKey.of(GBP_LIBOR_3M);
    coverImmutableBean(test);
    SwaptionVolatilitiesKey test2 = SwaptionVolatilitiesKey.of(GBP_LIBOR_6M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwaptionVolatilitiesKey test = SwaptionVolatilitiesKey.of(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
