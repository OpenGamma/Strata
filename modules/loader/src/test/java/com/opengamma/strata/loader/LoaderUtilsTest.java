/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.index.PriceIndices;

/**
 * Test {@link LoaderUtils}.
 */
@Test
public class LoaderUtilsTest {

  public void test_findIndex() {
    assertEquals(LoaderUtils.findIndex("GBP-LIBOR-3M"), IborIndices.GBP_LIBOR_3M);
    assertEquals(LoaderUtils.findIndex("GBP-SONIA"), OvernightIndices.GBP_SONIA);
    assertEquals(LoaderUtils.findIndex("GB-RPI"), PriceIndices.GB_RPI);
    assertEquals(LoaderUtils.findIndex("GBP/USD-WM"), FxIndices.GBP_USD_WM);
    assertThrowsIllegalArg(() -> LoaderUtils.findIndex("Rubbish"));
  }

  public void coverage() {
    coverPrivateConstructor(LoaderUtils.class);
  }

}
