/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateIndex}.
 */
public class FloatingRateIndexTest {

  @Test
  public void test_parse_noTenor() {
    assertThat(FloatingRateIndex.parse("GBP-LIBOR")).isEqualTo(IborIndices.GBP_LIBOR_3M);
    assertThat(FloatingRateIndex.parse("GBP-LIBOR-1M")).isEqualTo(IborIndices.GBP_LIBOR_1M);
    assertThat(FloatingRateIndex.parse("GBP-LIBOR-3M")).isEqualTo(IborIndices.GBP_LIBOR_3M);
    assertThat(FloatingRateIndex.parse("GBP-SONIA")).isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(FloatingRateIndex.parse("GB-RPI")).isEqualTo(PriceIndices.GB_RPI);
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse(null));
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse("NotAnIndex"));
  }

  @Test
  public void test_parse_withTenor() {
    assertThat(FloatingRateIndex.parse("GBP-LIBOR", Tenor.TENOR_6M)).isEqualTo(IborIndices.GBP_LIBOR_6M);
    assertThat(FloatingRateIndex.parse("GBP-LIBOR-1M", Tenor.TENOR_6M)).isEqualTo(IborIndices.GBP_LIBOR_1M);
    assertThat(FloatingRateIndex.parse("GBP-LIBOR-3M", Tenor.TENOR_6M)).isEqualTo(IborIndices.GBP_LIBOR_3M);
    assertThat(FloatingRateIndex.parse("GBP-SONIA", Tenor.TENOR_6M)).isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(FloatingRateIndex.parse("GB-RPI", Tenor.TENOR_6M)).isEqualTo(PriceIndices.GB_RPI);
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse(null, Tenor.TENOR_6M));
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateIndex.parse("NotAnIndex", Tenor.TENOR_6M));
  }

  @Test
  public void test_tryParse_noTenor() {
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR")).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_3M));
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR-1M")).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_1M));
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR-3M")).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_3M));
    assertThat(FloatingRateIndex.tryParse("GBP-SONIA")).isEqualTo(Optional.of(OvernightIndices.GBP_SONIA));
    assertThat(FloatingRateIndex.tryParse("GB-RPI")).isEqualTo(Optional.of(PriceIndices.GB_RPI));
    assertThat(FloatingRateIndex.tryParse(null)).isEqualTo(Optional.empty());
    assertThat(FloatingRateIndex.tryParse("NotAnIndex")).isEqualTo(Optional.empty());
  }

  @Test
  public void test_tryParse_withTenor() {
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR", Tenor.TENOR_6M)).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_6M));
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR-1M", Tenor.TENOR_6M)).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_1M));
    assertThat(FloatingRateIndex.tryParse("GBP-LIBOR-3M", Tenor.TENOR_6M)).isEqualTo(Optional.of(IborIndices.GBP_LIBOR_3M));
    assertThat(FloatingRateIndex.tryParse("GBP-SONIA", Tenor.TENOR_6M)).isEqualTo(Optional.of(OvernightIndices.GBP_SONIA));
    assertThat(FloatingRateIndex.tryParse("GB-RPI", Tenor.TENOR_6M)).isEqualTo(Optional.of(PriceIndices.GB_RPI));
    assertThat(FloatingRateIndex.tryParse(null, Tenor.TENOR_6M)).isEqualTo(Optional.empty());
    assertThat(FloatingRateIndex.tryParse("NotAnIndex", Tenor.TENOR_6M)).isEqualTo(Optional.empty());
  }

}
