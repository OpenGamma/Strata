/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link BondFutureOptionSensitivity}.
 */
public class BondFutureOptionSensitivityTest {
  private static final BondFutureVolatilitiesName NAME = BondFutureVolatilitiesName.of("GOVT1-BOND-FUT");
  private static final double OPTION_EXPIRY = 1d;
  private static final LocalDate FUTURE_EXPIRY = date(2015, 8, 28);
  private static final double STRIKE_PRICE = 0.98;
  private static final double FUTURE_PRICE = 0.99;
  private static final double SENSITIVITY = 32d;

  @Test
  public void test_of() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getExpiry()).isEqualTo(OPTION_EXPIRY);
    assertThat(test.getFutureExpiryDate()).isEqualTo(FUTURE_EXPIRY);
    assertThat(test.getStrikePrice()).isEqualTo(STRIKE_PRICE);
    assertThat(test.getFuturePrice()).isEqualTo(FUTURE_PRICE);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertThat(base.withCurrency(GBP)).isSameAs(base);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY);
    BondFutureOptionSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 20d);
    BondFutureOptionSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    BondFutureOptionSensitivity a1 = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity a2 = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity b = BondFutureOptionSensitivity.of(BondFutureVolatilitiesName.of("FOO-BOND-FUT"),
        OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity c = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY + 1, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity d = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, date(2015, 9, 28), STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity e = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, 0.995, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity f = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, 0.975, GBP, SENSITIVITY);
    BondFutureOptionSensitivity g = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) > 0).isTrue();
    assertThat(b.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(e.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(f) > 0).isTrue();
    assertThat(f.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(g) < 0).isTrue();
    assertThat(g.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    BondFutureOptionSensitivity test1 = (BondFutureOptionSensitivity) base.convertedTo(USD, matrix);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(NAME, OPTION_EXPIRY,
        FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY * rate);
    assertThat(test1).isEqualTo(expected);
    BondFutureOptionSensitivity test2 = (BondFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY * 3.5d);
    BondFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 1 / SENSITIVITY);
    BondFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BondFutureOptionSensitivity test1 = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    coverImmutableBean(test1);
    BondFutureOptionSensitivity test2 = BondFutureOptionSensitivity.of(
        BondFutureVolatilitiesName.of("FOO-BOND-FUT"),
        OPTION_EXPIRY + 1,
        date(2015, 9, 28),
        0.985,
        0.995,
        USD,
        SENSITIVITY);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
