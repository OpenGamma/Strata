/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

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
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse(null));
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse("NotAnIndex"));
  }

  public void test_parse_withTenor() {
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR", Tenor.TENOR_6M), IborIndices.GBP_LIBOR_6M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-1M", Tenor.TENOR_6M), IborIndices.GBP_LIBOR_1M);
    assertEquals(FloatingRateIndex.parse("GBP-LIBOR-3M", Tenor.TENOR_6M), IborIndices.GBP_LIBOR_3M);
    assertEquals(FloatingRateIndex.parse("GBP-SONIA", Tenor.TENOR_6M), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateIndex.parse("GB-RPI", Tenor.TENOR_6M), PriceIndices.GB_RPI);
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse(null, Tenor.TENOR_6M));
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse("NotAnIndex", Tenor.TENOR_6M));
  }

  public void test_tryParse_noTenor() {
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR"), Optional.of(IborIndices.GBP_LIBOR_3M));
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR-1M"), Optional.of(IborIndices.GBP_LIBOR_1M));
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR-3M"), Optional.of(IborIndices.GBP_LIBOR_3M));
    assertEquals(FloatingRateIndex.tryParse("GBP-SONIA"), Optional.of(OvernightIndices.GBP_SONIA));
    assertEquals(FloatingRateIndex.tryParse("GB-RPI"), Optional.of(PriceIndices.GB_RPI));
    assertEquals(FloatingRateIndex.tryParse(null), Optional.empty());
    assertEquals(FloatingRateIndex.tryParse("NotAnIndex"), Optional.empty());
  }

  public void test_tryParse_withTenor() {
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR", Tenor.TENOR_6M), Optional.of(IborIndices.GBP_LIBOR_6M));
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR-1M", Tenor.TENOR_6M), Optional.of(IborIndices.GBP_LIBOR_1M));
    assertEquals(FloatingRateIndex.tryParse("GBP-LIBOR-3M", Tenor.TENOR_6M), Optional.of(IborIndices.GBP_LIBOR_3M));
    assertEquals(FloatingRateIndex.tryParse("GBP-SONIA", Tenor.TENOR_6M), Optional.of(OvernightIndices.GBP_SONIA));
    assertEquals(FloatingRateIndex.tryParse("GB-RPI", Tenor.TENOR_6M), Optional.of(PriceIndices.GB_RPI));
    assertEquals(FloatingRateIndex.tryParse(null, Tenor.TENOR_6M), Optional.empty());
    assertEquals(FloatingRateIndex.tryParse("NotAnIndex", Tenor.TENOR_6M), Optional.empty());
  }

}
