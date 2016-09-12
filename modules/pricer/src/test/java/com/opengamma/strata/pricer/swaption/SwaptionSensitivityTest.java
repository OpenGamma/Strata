/**
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link SwaptionSensitivity}.
 */
@Test
public class SwaptionSensitivityTest {

  private static final double EXPIRY = 1d;
  private static final double TENOR = 3d;
  private static final double STRIKE = 7d;
  private static final double FORWARD = 9d;
  private static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test");
  private static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test2");

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertEquals(test.getVolatilitiesName(), NAME);
    assertEquals(test.getExpiry(), EXPIRY);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSensitivity(), 32d);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertSame(base.withCurrency(GBP), base);

    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d);
    SwaptionSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 20d);
    SwaptionSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    SwaptionSensitivity a1 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity a2 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity b = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d);
    SwaptionSensitivity c = SwaptionSensitivity.of(NAME, EXPIRY + 1, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity d = SwaptionSensitivity.of(NAME, EXPIRY, TENOR + 1, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity e = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE + 1, FORWARD, GBP, 32d);
    SwaptionSensitivity f = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD + 1, GBP, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(a1.compareKey(e) < 0, true);
    assertEquals(a1.compareKey(f) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    FxRate rate = FxRate.of(GBP, USD, 1.5d);
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, USD, 32d * 1.5d);
    assertEquals(base.convertedTo(USD, rate), expected);
    assertEquals(base.convertedTo(GBP, rate), base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d * 3.5d);
    SwaptionSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity expected = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 1 / 32d);
    SwaptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    SwaptionSensitivity base1 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity base2 = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    SwaptionSensitivity base = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    SwaptionSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    coverImmutableBean(test);
    SwaptionSensitivity test2 = SwaptionSensitivity.of(
        NAME2, EXPIRY + 1, TENOR + 1, STRIKE + 1, FORWARD + 1, USD, 32d);
    coverBeanEquals(test, test2);
    ZeroRateSensitivity test3 = ZeroRateSensitivity.of(USD, 0.5d, 2d);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    SwaptionSensitivity test = SwaptionSensitivity.of(NAME, EXPIRY, TENOR, STRIKE, FORWARD, GBP, 32d);
    assertSerialization(test);
  }

}
