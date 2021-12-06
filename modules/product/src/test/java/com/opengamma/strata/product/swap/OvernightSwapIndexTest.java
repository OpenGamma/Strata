/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test {@link OvernightSwapIndex}.
 */
public class OvernightSwapIndexTest {

  private static ZoneId LONDON = ZoneId.of("Europe/London");
  private static ZoneId NEY_YORK = ZoneId.of("America/New_York");
  private static ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

  @Test
  public void test_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightSwapIndex.of("foo"));
  }

  @Test
  public void test_swapIndicies() {
    ImmutableMap<String, OvernightSwapIndex> mapAll = OvernightSwapIndices.ENUM_LOOKUP.lookupAll();
    ImmutableList<OvernightSwapIndex> indexAll = mapAll.values().asList();
    ImmutableList<String> nameAll = mapAll.keySet().asList();
    int size = indexAll.size();
    for (int i = 0; i < size; ++i) {
      // check no duplication
      for (int j = i + 1; j < size; ++j) {
        assertThat(nameAll.get(i).equals(nameAll.get(j))).isFalse();
        assertThat(indexAll.get(i).equals(indexAll.get(j))).isFalse();
      }
    }
    for (String name : nameAll) {
      OvernightSwapIndex index = mapAll.get(name);
      assertThat(OvernightSwapIndex.of(name)).isEqualTo(index);
      assertThat(index.isActive()).isTrue();
      FixedOvernightSwapTemplate temp = index.getTemplate();
      FixedOvernightSwapConvention conv = temp.getConvention();
      Tenor tenor = temp.getTenor();
      LocalTime time = index.getFixingTime();
      ZoneId zone = index.getFixingZone();
      // test consistency between name and template
      assertThat(name.contains(tenor.toString())).isTrue();
      if (name.startsWith("USD")) {
        assertThat(name.contains("1100")).isTrue();
        assertThat(conv.equals(FixedOvernightSwapConventions.USD_FIXED_1Y_SOFR_OIS)).isTrue();
        assertThat(zone.equals(NEY_YORK)).isTrue();
      }
      if (name.startsWith("GBP")) {
        assertThat(name.contains("1100")).isTrue();
        assertThat(conv.equals(FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS)).isTrue();
        assertThat(zone.equals(LONDON)).isTrue();
      }
      if (name.startsWith("JPY")) {
        assertThat(name.contains("1030") || name.contains("1530")).isTrue();
        assertThat(conv.equals(FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS)).isTrue();
        assertThat(zone.equals(TOKYO)).isTrue();
      }
      if (name.contains("1100")) {
        assertThat(time.equals(LocalTime.of(11, 0))).isTrue();
      }
      if (name.contains("1030")) {
        assertThat(time.equals(LocalTime.of(10, 30))).isTrue();
      }
      if (name.contains("1530")) {
        assertThat(time.equals(LocalTime.of(15, 30))).isTrue();
      }
      assertThat(index.calculateFixingDateTime(date(2015, 6, 30)))
          .isEqualTo(date(2015, 6, 30).atTime(time).atZone(zone));
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {IborIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
        {OvernightIndices.GBP_SONIA, "GBP-SONIA"},
        {PriceIndices.GB_HICP, "GB-HICP"},
        {FxIndices.EUR_CHF_ECB, "EUR/CHF-ECB"},
        {SwapIndices.EUR_EURIBOR_1100_12Y, "EUR-EURIBOR-1100-12Y"},

        {OvernightSwapIndices.GBP_SONIA_1100_10Y, "GBP-SONIA-1100-10Y"}, 
        {OvernightSwapIndices.USD_SOFR_1100_15Y, "USD-SOFR-1100-15Y"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(Index convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(Index convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(Index convention, String name) {
    assertThat(Index.of(name)).isEqualTo(convention);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableOvernightSwapIndex index = ImmutableOvernightSwapIndex.builder()
        .name("FooIndex")
        .fixingTime(LocalTime.of(12, 30))
        .fixingZone(ZoneId.of("Africa/Abidjan"))
        .template(FixedOvernightSwapTemplate.of(Tenor.TENOR_9M, FixedOvernightSwapConventions.CHF_FIXED_1Y_SARON_OIS))
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(SwapIndices.class);
  }

  @Test
  public void test_serialization() {
    ImmutableOvernightSwapIndex index = ImmutableOvernightSwapIndex.builder()
        .name("FooIndex")
        .fixingTime(LocalTime.of(12, 30))
        .fixingZone(ZoneId.of("Africa/Abidjan"))
        .template(FixedOvernightSwapTemplate.of(Tenor.TENOR_9M, FixedOvernightSwapConventions.CHF_FIXED_1Y_SARON_OIS))
        .build();
    assertSerialization(index);
  }

}
