/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Test {@link SwapIndex}.
 */
public class SwapIndexTest {

  private static ZoneId LONDON = ZoneId.of("Europe/London");
  private static ZoneId NEY_YORK = ZoneId.of("America/New_York");
  private static ZoneId FRANKFURT = ZoneId.of("Europe/Berlin");  // Frankfurt not defined in TZDB

  @Test
  public void test_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SwapIndex.of("foo"));
  }

  @Test
  public void test_swapIndicies() {
    ImmutableMap<String, SwapIndex> mapAll = SwapIndices.ENUM_LOOKUP.lookupAll();
    ImmutableList<SwapIndex> indexAll = mapAll.values().asList();
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
      SwapIndex index = mapAll.get(name);
      assertThat(SwapIndex.of(name)).isEqualTo(index);
      assertThat(index.isActive()).isTrue();
      FixedIborSwapTemplate temp = index.getTemplate();
      FixedIborSwapConvention conv = temp.getConvention();
      Tenor tenor = temp.getTenor();
      LocalTime time = index.getFixingTime();
      ZoneId zone = index.getFixingZone();
      // test consistency between name and template
      assertThat(name.contains(tenor.toString())).isTrue();
      if (name.startsWith("USD")) {
        assertThat(name.contains("1100") || name.contains("1500")).isTrue();
        assertThat(conv.equals(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M)).isTrue();
        assertThat(zone.equals(NEY_YORK)).isTrue();
      }
      if (name.startsWith("GBP")) {
        assertThat(name.contains("1100")).isTrue();
        if (tenor.equals(Tenor.TENOR_1Y)) {
          assertThat(conv.equals(FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M)).isTrue();
        } else {
          assertThat(conv.equals(FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M)).isTrue();
        }
        assertThat(zone.equals(LONDON)).isTrue();
      }
      if (name.startsWith("EUR")) {
        assertThat(name.contains("1100") || name.contains("1200")).isTrue();
        if (tenor.equals(Tenor.TENOR_1Y)) {
          assertThat(conv.equals(FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M)).isTrue();
        } else {
          assertThat(conv.equals(FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M)).isTrue();
        }
        assertThat(zone.equals(FRANKFURT)).isTrue();
      }
      if (name.contains("1100")) {
        assertThat(time.equals(LocalTime.of(11, 0))).isTrue();
      }
      if (name.contains("1200")) {
        assertThat(time.equals(LocalTime.of(12, 0))).isTrue();
      }
      if (name.contains("1500")) {
        assertThat(time.equals(LocalTime.of(15, 0))).isTrue();
      }
      assertThat(index.calculateFixingDateTime(date(2015, 6, 30))).isEqualTo(date(2015, 6, 30).atTime(time).atZone(zone));
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
        {SwapIndices.GBP_LIBOR_1100_2Y, "GBP-LIBOR-1100-2Y"},
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
    ImmutableSwapIndex index = ImmutableSwapIndex.builder()
        .name("FooIndex")
        .fixingTime(LocalTime.of(12, 30))
        .fixingZone(ZoneId.of("Africa/Abidjan"))
        .template(FixedIborSwapTemplate.of(Tenor.TENOR_9M, FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M))
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(SwapIndices.class);
  }

  @Test
  public void test_serialization() {
    SwapIndex index = ImmutableSwapIndex.builder()
        .name("FooIndex")
        .fixingTime(LocalTime.of(12, 30))
        .fixingZone(ZoneId.of("Africa/Abidjan"))
        .template(FixedIborSwapTemplate.of(Tenor.TENOR_9M, FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M))
        .build();
    assertSerialization(index);
  }

}
