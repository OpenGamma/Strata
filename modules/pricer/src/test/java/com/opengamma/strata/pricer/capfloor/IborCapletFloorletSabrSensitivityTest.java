/**
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
 * Test {@link IborCapletFloorletSabrSensitivity}.
 */
@Test
public class IborCapletFloorletSabrSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("Test");
  private static final IborCapletFloorletVolatilitiesName NAME2 = IborCapletFloorletVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  public void test_of() {
    IborCapletFloorletSabrSensitivity test = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertEquals(test.getVolatilitiesName(), NAME);
    assertEquals(test.getExpiry(), EXPIRY);
    assertEquals(test.getSensitivityType(), SabrParameterType.ALPHA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSensitivity(), 32d);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertSame(base.withCurrency(GBP), base);

    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, USD, 32d);
    IborCapletFloorletSabrSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 20d);
    IborCapletFloorletSabrSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
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
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, USD, 32d * 1.5d);
    assertEquals(base.convertedTo(USD, rate), expected);
    assertEquals(base.convertedTo(GBP, rate), base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d * 3.5d);
    IborCapletFloorletSabrSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity expected = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 1 / 32d);
    IborCapletFloorletSabrSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    IborCapletFloorletSabrSensitivity base1 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity base2 = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    IborCapletFloorletSabrSensitivity base = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    IborCapletFloorletSabrSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    IborCapletFloorletSabrSensitivity test = IborCapletFloorletSabrSensitivity.of(
        NAME, EXPIRY, SabrParameterType.ALPHA, GBP, 32d);
    assertSerialization(test);
  }

}
