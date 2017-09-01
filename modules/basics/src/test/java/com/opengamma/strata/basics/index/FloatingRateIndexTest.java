/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateIndex}.
 */
@Test
public class FloatingRateIndexTest {

  public void test_parse_noTenor() {
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR"), IborIndices.GBP_LIBOR_3M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-1M"), IborIndices.GBP_LIBOR_1M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-3M"), IborIndices.GBP_LIBOR_3M);
    assertEquals(FloatingRateIndex.parse("GBP-SONIA"), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateIndex.parse("GB-RPI"), PriceIndices.GB_RPI);
    assertThrowsIllegalArg(() -> FloatingRateIndex.parse(null));
    assertThrowsIllegalArg(() -> FloatingRateIndex.parse("NotAnIndex"));
  }

  public void test_parse_withTenor() {
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR", Tenor.TENOR_6M), IborIndices.GBP_LIBOR_6M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-1M"), IborIndices.GBP_LIBOR_1M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-3M"), IborIndices.GBP_LIBOR_3M);
    assertEquals(FloatingRateIndex.parse("GBP-SONIA"), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateIndex.parse("GB-RPI"), PriceIndices.GB_RPI);
    assertThrowsIllegalArg(() -> FloatingRateIndex.parse(null));
    assertThrowsIllegalArg(() -> FloatingRateIndex.parse("NotAnIndex"));
  }

}
