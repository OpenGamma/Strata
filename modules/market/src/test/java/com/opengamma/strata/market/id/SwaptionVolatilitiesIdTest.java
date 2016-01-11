/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;
import com.opengamma.strata.market.view.SwaptionVolatilities;

/**
 * Test {@link SwaptionVolatilitiesId}.
 */
@Test
public class SwaptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), IborIndices.GBP_LIBOR_3M);
    assertEquals(test.getMarketDataType(), SwaptionVolatilities.class);
    assertEquals(test.toMarketDataKey(), SwaptionVolatilitiesKey.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of(GBP_LIBOR_3M);
    coverImmutableBean(test);
    SwaptionVolatilitiesId test2 = SwaptionVolatilitiesId.of(GBP_LIBOR_6M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
