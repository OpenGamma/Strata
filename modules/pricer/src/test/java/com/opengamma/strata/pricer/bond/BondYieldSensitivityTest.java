/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link BondYieldSensitivity}
 */
public class BondYieldSensitivityTest {

  private static final BondVolatilitiesName NAME = BondVolatilitiesName.of("BOND-VOL");
  private static final double OPTION_EXPIRY = 1d;
  private static final double DURATION = 5.6d;
  private static final double STRIKE_YIELD = 0.01;
  private static final double FORWARD_YIELD = 0.012;
  private static final double SENSITIVITY = 32d;

  @Test
  public void test_of() {
    BondYieldSensitivity test = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getExpiry()).isEqualTo(OPTION_EXPIRY);
    assertThat(test.getDuration()).isEqualTo(DURATION);
    assertThat(test.getStrike()).isEqualTo(STRIKE_YIELD);
    assertThat(test.getForward()).isEqualTo(FORWARD_YIELD);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    assertThat(base.withCurrency(GBP)).isSameAs(base);
    BondYieldSensitivity expected = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, USD, SENSITIVITY);
    BondYieldSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity expected = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, 20.d);
    BondYieldSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    BondYieldSensitivity a1 = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity a2 = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity b = BondYieldSensitivity.of(BondVolatilitiesName.of("FOO-BOND-FUT"),
        OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity c = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY + 1.0d, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity e = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, 0.0150, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity f = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, 0.02, GBP, SENSITIVITY);
    BondYieldSensitivity g = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, USD, SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(e.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(f) < 0).isTrue();
    assertThat(f.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(g) < 0).isTrue();
    assertThat(g.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    BondYieldSensitivity test1 = (BondYieldSensitivity) base.convertedTo(USD, matrix);
    BondYieldSensitivity expected = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, USD, SENSITIVITY * rate);
    assertThat(test1).isEqualTo(expected);
    BondYieldSensitivity test2 = (BondYieldSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity expected = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY * 3.5d);
    BondYieldSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity expected = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, 1.0d / SENSITIVITY);
    BondYieldSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    BondYieldSensitivity base = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    BondYieldSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BondYieldSensitivity test1 = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    coverImmutableBean(test1);
    BondYieldSensitivity test2 = BondYieldSensitivity.of(
        BondVolatilitiesName.of("FOO-BOND"),
        OPTION_EXPIRY + 1,
        DURATION + 1.0d,
        0.0050,
        0.0050,
        USD,
        SENSITIVITY + 1.0d);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    BondYieldSensitivity test = BondYieldSensitivity.of(
        NAME, OPTION_EXPIRY, DURATION, STRIKE_YIELD, FORWARD_YIELD, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
