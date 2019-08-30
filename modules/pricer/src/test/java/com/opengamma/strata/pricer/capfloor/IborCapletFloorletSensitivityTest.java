/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

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
 * Test {@link IborCapletFloorletSensitivity}.
 */
public class IborCapletFloorletSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final double FORWARD = 0.015d;
  private static final double STRIKE = 0.001d;
  private static final double SENSITIVITY = 0.54d;
  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("Test");
  private static final IborCapletFloorletVolatilitiesName NAME2 = IborCapletFloorletVolatilitiesName.of("Test2");

  @Test
  public void test_of() {
    IborCapletFloorletSensitivity test =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getForward()).isEqualTo(FORWARD);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, USD, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    double sensi = 23.5;
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, sensi);
    IborCapletFloorletSensitivity test = base.withSensitivity(sensi);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    IborCapletFloorletSensitivity a1 =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity a2 =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity b =
        IborCapletFloorletSensitivity.of(NAME2, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity c =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY + 1, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity d =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, 0.009, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity e =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, 0.005, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity f =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, USD, SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) > 0).isTrue();
    assertThat(e.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(f) < 0).isTrue();
    assertThat(f.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IborCapletFloorletSensitivity test1 = base.convertedTo(USD, matrix);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, USD, SENSITIVITY * rate);
    assertThat(test1).isEqualTo(expected);
    IborCapletFloorletSensitivity test2 = base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    double factor = 3.5d;
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY * factor);
    IborCapletFloorletSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, 1d / SENSITIVITY);
    IborCapletFloorletSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborCapletFloorletSensitivity test1 =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    coverImmutableBean(test1);
    IborCapletFloorletSensitivity test2 =
        IborCapletFloorletSensitivity.of(NAME2, EXPIRY + 2d, 0.98, 0.99, USD, 32d);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletSensitivity test =
        IborCapletFloorletSensitivity.of(NAME, EXPIRY, STRIKE, FORWARD, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
