/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link FxOptionSensitivity}.
 */
public class FxOptionSensitivityTest {

  private static final double EXPIRY = 2d;
  private static final CurrencyPair PAIR = CurrencyPair.of(EUR, GBP);
  private static final double FORWARD = 0.8d;
  private static final double STRIKE = 0.95d;
  private static final double SENSI_VALUE = 1.24d;
  private static final FxOptionVolatilitiesName NAME = FxOptionVolatilitiesName.of("Test");
  private static final FxOptionVolatilitiesName NAME2 = FxOptionVolatilitiesName.of("Test2");

  @Test
  public void test_of() {
    FxOptionSensitivity test = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getForward()).isEqualTo(FORWARD);
    assertThat(test.getCurrencyPair()).isEqualTo(PAIR);
    assertThat(test.getSensitivity()).isEqualTo(SENSI_VALUE);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
  }

  @Test
  public void test_withCurrency() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test1 = base.withCurrency(EUR);
    assertThat(test1.getCurrency()).isEqualTo(EUR);
    assertThat(test1.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test1.getForward()).isEqualTo(FORWARD);
    assertThat(test1.getCurrencyPair()).isEqualTo(PAIR);
    assertThat(test1.getSensitivity()).isEqualTo(SENSI_VALUE);
    assertThat(test1.getStrike()).isEqualTo(STRIKE);
    FxOptionSensitivity test2 = base.withCurrency(GBP);
    assertThat(test2).isEqualTo(base);
  }

  @Test
  public void test_withSensitivity() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    double newSensi = 22.5;
    FxOptionSensitivity test = base.withSensitivity(newSensi);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getForward()).isEqualTo(FORWARD);
    assertThat(test.getCurrencyPair()).isEqualTo(PAIR);
    assertThat(test.getSensitivity()).isEqualTo(newSensi);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
  }

  @Test
  public void test_compareExcludingSensitivity() {
    FxOptionSensitivity a1 = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity a2 = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity b = FxOptionSensitivity.of(
        NAME, CurrencyPair.of(EUR, USD), EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity c = FxOptionSensitivity.of(NAME, PAIR, EXPIRY + 1, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity d = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, 0.96, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity e = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, 0.81, GBP, SENSI_VALUE);
    FxOptionSensitivity f = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, EUR, SENSI_VALUE);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(e.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(f) > 0).isTrue();
    assertThat(f.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  @Test
  public void test_multipliedBy() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    double factor = 5.2d;
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE * factor);
    FxOptionSensitivity test = base.multipliedBy(factor);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_mapSensitivity() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, 1.0 / SENSI_VALUE);
    FxOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_normalize() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  @Test
  public void test_buildInto() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  @Test
  public void test_build() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  @Test
  public void test_cloned() {
    FxOptionSensitivity base = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  @Test
  public void coverage() {
    FxOptionSensitivity test1 = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    coverImmutableBean(test1);
    FxOptionSensitivity test2 = FxOptionSensitivity.of(NAME2, CurrencyPair.of(EUR, USD), EXPIRY, 0.8, 0.9, EUR, 1.1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxOptionSensitivity test = FxOptionSensitivity.of(NAME, PAIR, EXPIRY, STRIKE, FORWARD, GBP, SENSI_VALUE);
    assertSerialization(test);
  }

}
