/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link SwaptionSensitivity}.
 */
public class SwaptionSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final double TENOR = 3d;
  private static final double STRIKE = 7d;
  private static final double FORWARD = 9d;
  private static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test");
  private static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getForward()).isEqualTo(FORWARD);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(32d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d);
    SwaptionSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 20d);
    SwaptionSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    SwaptionSensitivity a1 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity a2 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity b = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d);
    SwaptionSensitivity c = SwaptionSensitivity.of(NAME, EXPIRY + 1, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity d = SwaptionSensitivity.of(NAME, EXPIRY, TENOR + 1, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity e = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE + 1, FORWARD, GBP, 32d);
    SwaptionSensitivity f = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD + 1, GBP, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(a1.compareKey(f) < 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d * 1.5d);
    assertThat(base.convertedTo(USD, rate)).isEqualTo(expected);
    assertThat(base.convertedTo(GBP, rate)).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d * 3.5d);
    SwaptionSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 1 / 32d);
    SwaptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    SwaptionSensitivity base1 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity base2 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    coverImmutableBean(test);
    SwaptionSensitivity test2 = SwaptionSensitivity.of(
        NAME2, EXPIRY + 1, TENOR + 1, STRIKE + 1, FORWARD + 1, USD, 32d);
    coverBeanEquals(test, test2);
    ZeroRateSensitivity test3 = ZeroRateSensitivity.of(USD, 0.5d, 2d);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertSerialization(test);
  }

}
