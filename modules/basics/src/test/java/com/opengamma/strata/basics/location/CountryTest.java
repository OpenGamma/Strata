/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.location;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test Country.
 */
public class CountryTest {

  @Test
  public void test_constants() {
    assertThat(Country.of("EU")).isEqualTo(Country.EU);
    assertThat(Country.of("AT")).isEqualTo(Country.AT);
    assertThat(Country.of("BE")).isEqualTo(Country.BE);
    assertThat(Country.of("CH")).isEqualTo(Country.CH);
    assertThat(Country.of("CZ")).isEqualTo(Country.CZ);
    assertThat(Country.of("DE")).isEqualTo(Country.DE);
    assertThat(Country.of("DK")).isEqualTo(Country.DK);
    assertThat(Country.of("ES")).isEqualTo(Country.ES);
    assertThat(Country.of("FI")).isEqualTo(Country.FI);
    assertThat(Country.of("FR")).isEqualTo(Country.FR);
    assertThat(Country.of("GB")).isEqualTo(Country.GB);
    assertThat(Country.of("GR")).isEqualTo(Country.GR);
    assertThat(Country.of("HU")).isEqualTo(Country.HU);
    assertThat(Country.of("IE")).isEqualTo(Country.IE);
    assertThat(Country.of("IS")).isEqualTo(Country.IS);
    assertThat(Country.of("IT")).isEqualTo(Country.IT);
    assertThat(Country.of("LU")).isEqualTo(Country.LU);
    assertThat(Country.of("NL")).isEqualTo(Country.NL);
    assertThat(Country.of("NO")).isEqualTo(Country.NO);
    assertThat(Country.of("PL")).isEqualTo(Country.PL);
    assertThat(Country.of("PT")).isEqualTo(Country.PT);
    assertThat(Country.of("SE")).isEqualTo(Country.SE);
    assertThat(Country.of("SK")).isEqualTo(Country.SK);
    assertThat(Country.of("TR")).isEqualTo(Country.TR);

    assertThat(Country.of("AR")).isEqualTo(Country.AR);
    assertThat(Country.of("BR")).isEqualTo(Country.BR);
    assertThat(Country.of("CA")).isEqualTo(Country.CA);
    assertThat(Country.of("CL")).isEqualTo(Country.CL);
    assertThat(Country.of("MX")).isEqualTo(Country.MX);
    assertThat(Country.of("US")).isEqualTo(Country.US);

    assertThat(Country.of("AU")).isEqualTo(Country.AU);
    assertThat(Country.of("CN")).isEqualTo(Country.CN);
    assertThat(Country.of("EG")).isEqualTo(Country.EG);
    assertThat(Country.of("HK")).isEqualTo(Country.HK);
    assertThat(Country.of("ID")).isEqualTo(Country.ID);
    assertThat(Country.of("IL")).isEqualTo(Country.IL);
    assertThat(Country.of("IN")).isEqualTo(Country.IN);
    assertThat(Country.of("JP")).isEqualTo(Country.JP);
    assertThat(Country.of("KR")).isEqualTo(Country.KR);
    assertThat(Country.of("MY")).isEqualTo(Country.MY);
    assertThat(Country.of("NZ")).isEqualTo(Country.NZ);
    assertThat(Country.of("RU")).isEqualTo(Country.RU);
    assertThat(Country.of("SA")).isEqualTo(Country.SA);
    assertThat(Country.of("SG")).isEqualTo(Country.SG);
    assertThat(Country.of("TH")).isEqualTo(Country.TH);
    assertThat(Country.of("ZA")).isEqualTo(Country.ZA);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_getAvailable() {
    Set<Country> available = Country.getAvailableCountries();
    assertThat(available.contains(Country.US)).isTrue();
    assertThat(available.contains(Country.EU)).isTrue();
    assertThat(available.contains(Country.JP)).isTrue();
    assertThat(available.contains(Country.GB)).isTrue();
    assertThat(available.contains(Country.CH)).isTrue();
    assertThat(available.contains(Country.AU)).isTrue();
    assertThat(available.contains(Country.CA)).isTrue();
  }

  @Test
  public void test_new_Country_included_in_getAvailable() {
    Set<Country> available = Country.getAvailableCountries();
    assertThat(available.size()).isGreaterThan(0);
    Country.of("XZ");
    Set<Country> updatedAvailable = Country.getAvailableCountries();
    assertThat(updatedAvailable.size() - available.size()).isEqualTo(1);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_of_String() {
    Country test = Country.of("SE");
    assertThat(test.getCode()).isEqualTo("SE");
    assertThat(test).isSameAs(Country.of("SE"));
  }

  @Test
  public void test_of_String_unknownCountryCreated() {
    Country test = Country.of("AA");
    assertThat(test.getCode()).isEqualTo("AA");
    assertThat(test).isSameAs(Country.of("AA"));
  }

  public static Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"A"},
        {"gb"},
        {"ABC"},
        {"123"},
        {" GB"},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ofBad")
  public void test_of_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Country.of(input));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_parse_String() {
    Country test = Country.parse("GB");
    assertThat(test.getCode()).isEqualTo("GB");
    assertThat(test).isSameAs(Country.GB);
  }

  @Test
  public void test_parse_String_unknownCountryCreated() {
    Country test = Country.parse("zy");
    assertThat(test.getCode()).isEqualTo("ZY");
    assertThat(test).isSameAs(Country.of("ZY"));
  }

  @Test
  public void test_parse_String_lowerCase() {
    Country test = Country.parse("gb");
    assertThat(test.getCode()).isEqualTo("GB");
    assertThat(test).isSameAs(Country.GB);
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"A"},
        {"ABC"},
        {"123"},
        {" GB"},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Country.parse(input));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    Country a = Country.EU;
    Country b = Country.GB;
    Country c = Country.JP;
    assertThat(0).isEqualTo(a.compareTo(a));
    assertThat(0).isEqualTo(b.compareTo(b));
    assertThat(0).isEqualTo(c.compareTo(c));

    assertThat(a.compareTo(b) < 0).isTrue();
    assertThat(b.compareTo(a) > 0).isTrue();

    assertThat(a.compareTo(c) < 0).isTrue();
    assertThat(c.compareTo(a) > 0).isTrue();

    assertThat(b.compareTo(c) < 0).isTrue();
    assertThat(c.compareTo(b) > 0).isTrue();
  }

  @Test
  public void test_compareTo_null() {
    assertThatNullPointerException().isThrownBy(() -> Country.EU.compareTo(null));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_from3CharString_constants() {
    assertThat(Country.of3Char("GBR")).isEqualTo(Country.GB);
    assertThat(Country.of3Char("FRA")).isEqualTo(Country.FR);
    assertThat(Country.of3Char("USA")).isEqualTo(Country.US);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_from3CharString_nonConstants() {
    assertThat(Country.of3Char("CRI")).isEqualTo(Country.of("CR"));
    assertThat(Country.of3Char("GIB")).isEqualTo(Country.of("GI"));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_from3CharString_missing() {
    assertThatIllegalArgumentException().isThrownBy(() -> Country.of3Char("ZZZ"));
    assertThatIllegalArgumentException().isThrownBy(() -> Country.of3Char(null));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_get3CharString() {
    assertThat(Country.GB.getCode3Char()).isEqualTo("GBR");
    assertThat(Country.FR.getCode3Char()).isEqualTo("FRA");
    assertThat(Country.US.getCode3Char()).isEqualTo("USA");
    assertThat(Country.of("CR").getCode3Char()).isEqualTo("CRI");
    assertThat(Country.of("GI").getCode3Char()).isEqualTo("GIB");
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_get3CharString_missing() {
    assertThatIllegalArgumentException().isThrownBy(() -> Country.of("ZZ").getCode3Char());
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    Country a1 = Country.GB;
    Country a2 = Country.of("GB");
    Country b = Country.EU;
    assertThat(a1)
        .isEqualTo(a1)
        .isEqualTo(a2)
        .isNotEqualByComparingTo(b)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_toString() {
    Country test = Country.GB;
    assertThat("GB").isEqualTo(test.toString());
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(Country.GB);
    assertSerialization(Country.of("US"));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(Country.class, Country.GB);
    assertJodaConvert(Country.class, Country.of("US"));
  }

}
