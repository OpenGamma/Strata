/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.Tenor.TENOR_1D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the tenor class.
 */
public class MarketTenorTest {

  private static final DaysAdjustment SPOT_LAG_2 = DaysAdjustment.ofBusinessDays(2, HolidayCalendarIds.GBLO);
  private static final DaysAdjustment SPOT_LAG_1 = DaysAdjustment.ofBusinessDays(1, HolidayCalendarIds.GBLO);
  private static final DaysAdjustment SPOT_LAG_0 = DaysAdjustment.ofBusinessDays(0, HolidayCalendarIds.GBLO);

  public void test_on() {
    MarketTenor test = MarketTenor.ON;
    assertThat(test.getCode()).isEqualTo("ON");
    assertThat(test.getTenor()).isEqualTo(TENOR_1D);
    assertThat(test.isNonStandardSpotLag()).isTrue();
    assertThat(test.adjustSpotLag(SPOT_LAG_2)).isEqualTo(SPOT_LAG_0);
    assertThat(test.adjustSpotLag(SPOT_LAG_1)).isEqualTo(SPOT_LAG_0);
    assertThat(test.adjustSpotLag(SPOT_LAG_0)).isEqualTo(SPOT_LAG_0);
  }

  @Test
  public void test_tn() {
    MarketTenor test = MarketTenor.TN;
    assertThat(test.getCode()).isEqualTo("TN");
    assertThat(test.getTenor()).isEqualTo(TENOR_1D);
    assertThat(test.isNonStandardSpotLag()).isTrue();
    assertThat(test.adjustSpotLag(SPOT_LAG_2)).isEqualTo(SPOT_LAG_1);
    assertThat(test.adjustSpotLag(SPOT_LAG_1)).isEqualTo(SPOT_LAG_1);
    assertThat(test.adjustSpotLag(SPOT_LAG_0)).isEqualTo(SPOT_LAG_1);
  }

  @Test
  public void test_sn() {
    MarketTenor test = MarketTenor.SN;
    assertThat(test.getCode()).isEqualTo("SN");
    assertThat(test.getTenor()).isEqualTo(TENOR_1D);
    assertThat(test.isNonStandardSpotLag()).isFalse();
    assertThat(test.adjustSpotLag(SPOT_LAG_2)).isEqualTo(SPOT_LAG_2);
    assertThat(test.adjustSpotLag(SPOT_LAG_1)).isEqualTo(SPOT_LAG_1);
    assertThat(test.adjustSpotLag(SPOT_LAG_0)).isEqualTo(SPOT_LAG_0);
  }

  @Test
  public void test_sw() {
    MarketTenor test = MarketTenor.SW;
    assertThat(test.getCode()).isEqualTo("SW");
    assertThat(test.getTenor()).isEqualTo(TENOR_1W);
    assertThat(test.isNonStandardSpotLag()).isFalse();
    assertThat(test.adjustSpotLag(SPOT_LAG_2)).isEqualTo(SPOT_LAG_2);
    assertThat(test.adjustSpotLag(SPOT_LAG_1)).isEqualTo(SPOT_LAG_1);
    assertThat(test.adjustSpotLag(SPOT_LAG_0)).isEqualTo(SPOT_LAG_0);
  }

  @Test
  public void test_ofSpot() {
    MarketTenor test = MarketTenor.ofSpot(TENOR_3Y);
    assertThat(test.getCode()).isEqualTo("3Y");
    assertThat(test.getTenor()).isEqualTo(TENOR_3Y);
    assertThat(test.isNonStandardSpotLag()).isFalse();
    assertThat(test.adjustSpotLag(SPOT_LAG_2)).isEqualTo(SPOT_LAG_2);
    assertThat(test.adjustSpotLag(SPOT_LAG_1)).isEqualTo(SPOT_LAG_1);
    assertThat(test.adjustSpotLag(SPOT_LAG_0)).isEqualTo(SPOT_LAG_0);
  }

  @Test
  public void test_ofSpot_special() {
    assertThat(MarketTenor.ofSpot(TENOR_1W)).isEqualTo(MarketTenor.SW);
    assertThat(MarketTenor.ofSpot(TENOR_1D)).isEqualTo(MarketTenor.SN);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_String_roundTrip() {
    assertThat(MarketTenor.parse(MarketTenor.ON.toString())).isEqualTo(MarketTenor.ON);
    assertThat(MarketTenor.parse(MarketTenor.TN.toString())).isEqualTo(MarketTenor.TN);
    assertThat(MarketTenor.parse(MarketTenor.SN.toString())).isEqualTo(MarketTenor.SN);
    assertThat(MarketTenor.parse(MarketTenor.SW.toString())).isEqualTo(MarketTenor.SW);
    assertThat(MarketTenor.parse(MarketTenor.ofSpot(TENOR_3Y).toString())).isEqualTo(MarketTenor.ofSpot(TENOR_3Y));
  }

  public static Object[][] data_parseGood() {
    return new Object[][] {
        {"ON", MarketTenor.ON},
        {"TN", MarketTenor.TN},
        {"SN", MarketTenor.SN},
        {"SW", MarketTenor.SW},
        {"2M", MarketTenor.ofSpot(TENOR_2M)},
        {"P2M", MarketTenor.ofSpot(TENOR_2M)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good_noP(String input, MarketTenor expected) {
    assertThat(MarketTenor.parse(input)).isEqualTo(expected);
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"2"},
        {"2K"},
        {"-2D"},
        {"PON"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> MarketTenor.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compare() {
    List<MarketTenor> tenors = ImmutableList.of(
        MarketTenor.ON,
        MarketTenor.TN,
        MarketTenor.SN,
        MarketTenor.ofSpot(Tenor.ofDays(2)),
        MarketTenor.ofSpot(Tenor.ofDays(6)),
        MarketTenor.SW,
        MarketTenor.ofSpot(Tenor.ofDays(8)),
        MarketTenor.ofSpot(Tenor.ofMonths(1)));

    List<MarketTenor> test = new ArrayList<>(tenors);
    Collections.shuffle(test);
    Collections.sort(test);
    assertThat(test).isEqualTo(tenors);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    MarketTenor a1 = MarketTenor.SN;
    MarketTenor a2 = MarketTenor.ofSpot(TENOR_1D);
    MarketTenor b = MarketTenor.ON;
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(a2)).isEqualTo(true);

    assertThat(a2.equals(a1)).isEqualTo(true);
    assertThat(a2.equals(a2)).isEqualTo(true);
    assertThat(a2.equals(b)).isEqualTo(false);

    assertThat(b.equals(a1)).isEqualTo(false);
    assertThat(b.equals(a2)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);

    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_equals_bad() {
    assertThat(MarketTenor.ON)
        .isEqualTo(MarketTenor.ON)
        .isNotEqualTo(MarketTenor.TN)
        .isNotEqualTo(MarketTenor.SN)
        .isNotEqualTo(MarketTenor.SW)
        .isNotEqualTo("BAD")
        .isNotEqualTo(null);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(MarketTenor.ON);
    assertSerialization(MarketTenor.SN);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(MarketTenor.class, MarketTenor.ON);
  }

}
