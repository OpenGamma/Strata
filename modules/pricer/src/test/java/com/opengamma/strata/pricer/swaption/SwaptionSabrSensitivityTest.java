/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link SwaptionSabrSensitivity}.
 */
public class SwaptionSabrSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final double TENOR = 3d;
  private static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test");
  private static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getSensitivityType()).isEqualTo(SabrParameterType.ALPHA);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(32d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, USD, 32d);
    SwaptionSabrSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 20d);
    SwaptionSabrSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    SwaptionSabrSensitivity a1 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity a2 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity b = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, USD, 32d);
    SwaptionSabrSensitivity c = SwaptionSabrSensitivity.of(
        NAME, EXPIRY + 1, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity d = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR + 1, SabrParameterType.ALPHA, GBP, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, USD, 32d * 1.5d);
    assertThat(base.convertedTo(USD, rate)).isEqualTo(expected);
    assertThat(base.convertedTo(GBP, rate)).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d * 3.5d);
    SwaptionSabrSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 1 / 32d);
    SwaptionSabrSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    SwaptionSabrSensitivity base1 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity base2 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    coverImmutableBean(test);
    SwaptionSabrSensitivity test2 = SwaptionSabrSensitivity.of(
        NAME2, EXPIRY + 1, TENOR + 1, SabrParameterType.BETA, GBP, 2d);
    coverBeanEquals(test, test2);
    ZeroRateSensitivity test3 = ZeroRateSensitivity.of(USD, 0.5d, 2d);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertSerialization(test);
  }

}
