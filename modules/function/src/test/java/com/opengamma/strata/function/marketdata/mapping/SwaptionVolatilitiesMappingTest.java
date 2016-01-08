/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.id.SwaptionVolatilitiesId;
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;

/**
 * Test {@link SwaptionVolatilitiesMapping}.
 */
@Test
public class SwaptionVolatilitiesMappingTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionVolatilitiesMapping test = SwaptionVolatilitiesMapping.of();
    assertEquals(test.getMarketDataKeyType(), SwaptionVolatilitiesKey.class);
    assertEquals(
        test.getIdForKey(SwaptionVolatilitiesKey.of(GBP_LIBOR_3M)),
        SwaptionVolatilitiesId.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionVolatilitiesMapping test = SwaptionVolatilitiesMapping.of();
    coverImmutableBean(test);
  }

}
