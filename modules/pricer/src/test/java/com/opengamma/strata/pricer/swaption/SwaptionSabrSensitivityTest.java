/**
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link SwaptionSabrSensitivity}.
 */
@Test
public class SwaptionSabrSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final double TENOR = 3d;
  private static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test");
  private static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertEquals(test.getVolatilitiesName(), NAME);
    assertEquals(test.getExpiry(), EXPIRY);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getSensitivityType(), SabrParameterType.ALPHA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSensitivity(), 32d);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertSame(base.withCurrency(GBP), base);

    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, USD, 32d);
    SwaptionSabrSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 20d);
    SwaptionSabrSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
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
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, USD, 32d * 1.5d);
    assertEquals(base.convertedTo(USD, rate), expected);
    assertEquals(base.convertedTo(GBP, rate), base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d * 3.5d);
    SwaptionSabrSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 1 / 32d);
    SwaptionSabrSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    SwaptionSabrSensitivity base1 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity base2 = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    SwaptionSabrSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        NAME, EXPIRY, TENOR, SabrParameterType.ALPHA, GBP, 32d);
    assertSerialization(test);
  }

}
