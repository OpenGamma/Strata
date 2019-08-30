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

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link IborCapletFloorletSabrSensitivity}.
 */
public class IborCapletFloorletSabrSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("Test");
  private static final IborCapletFloorletVolatilitiesName NAME2 = IborCapletFloorletVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    IborCapletFloorletSabrSensitivity test = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getSensitivityType()).isEqualTo(SabrParameterType.ALPHA);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(32d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, USD, 32d);
    IborCapletFloorletSabrSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 20d);
    IborCapletFloorletSabrSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    IborCapletFloorletSabrSensitivity a1 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity a2 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity b = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, USD, 32d);
    IborCapletFloorletSabrSensitivity c = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY + 1, SabrParameterType.ALPHA, GBP, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, USD, 32d * 1.5d);
    assertThat(base.convertedTo(USD, rate)).isEqualTo(expected);
    assertThat(base.convertedTo(GBP, rate)).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d * 3.5d);
    IborCapletFloorletSabrSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 1 / 32d);
    IborCapletFloorletSabrSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    IborCapletFloorletSabrSensitivity base1 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity base2 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborCapletFloorletSabrSensitivity test = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    coverImmutableBean(test);
    IborCapletFloorletSabrSensitivity test2 = IborCapletFloorletSabrSensitivity.of(
        NAME2, EXPIRY + 1, SabrParameterType.BETA, GBP, 2d);
    coverBeanEquals(test, test2);
    ZeroRateSensitivity test3 = ZeroRateSensitivity.of(USD, 0.5d, 2d);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletSabrSensitivity test = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertSerialization(test);
  }

}
