/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.location.Country.GB;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link PriceIndex}.
 */
public class PriceIndexTest {

  @Test
  public void test_gbpHicp() {
    PriceIndex test = PriceIndex.of("GB-HICP");
    assertThat(test.getName()).isEqualTo("GB-HICP");
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getRegion()).isEqualTo(GB);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getPublicationFrequency()).isEqualTo(Frequency.P1M);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("GB-HICP"));
    assertThat(test.toString()).isEqualTo("GB-HICP");
  }

  @Test
  public void test_getFloatingRateName() {
    for (PriceIndex index : PriceIndex.extendedEnum().lookupAll().values()) {
      assertThat(index.getFloatingRateName()).isEqualTo(FloatingRateName.of(index.getName()));
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PriceIndices.GB_HICP, "GB-HICP"},
        {PriceIndices.GB_RPI, "GB-RPI"},
        {PriceIndices.GB_RPIX, "GB-RPIX"},
        {PriceIndices.CH_CPI, "CH-CPI"},
        {PriceIndices.EU_AI_CPI, "EU-AI-CPI"},
        {PriceIndices.EU_EXT_CPI, "EU-EXT-CPI"},
        {PriceIndices.JP_CPI_EXF, "JP-CPI-EXF"},
        {PriceIndices.US_CPI_U, "US-CPI-U"},
        {PriceIndices.FR_EXT_CPI, "FR-EXT-CPI"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(PriceIndex convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PriceIndex convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PriceIndex convention, String name) {
    assertThat(PriceIndex.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(PriceIndex convention, String name) {
    ImmutableMap<String, PriceIndex> map = PriceIndex.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> PriceIndex.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> PriceIndex.of(null));
  }

  @Test
  public void test_gb_rpi() {
    assertThat(PriceIndices.GB_RPI.getCurrency()).isEqualTo(GBP);
    assertThat(PriceIndices.GB_RPI.getDayCount()).isEqualTo(DayCounts.ONE_ONE);
    assertThat(PriceIndices.GB_RPI.getDefaultFixedLegDayCount()).isEqualTo(DayCounts.ONE_ONE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(PriceIndices.class);
    coverImmutableBean((ImmutableBean) PriceIndices.US_CPI_U);
    coverBeanEquals((ImmutableBean) PriceIndices.US_CPI_U, ImmutablePriceIndex.builder()
        .name("Test")
        .region(Country.AR)
        .currency(Currency.ARS)
        .publicationFrequency(Frequency.P6M)
        .build());
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PriceIndex.class, PriceIndices.US_CPI_U);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PriceIndices.US_CPI_U);
  }

}
