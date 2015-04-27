/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test.
 */
@Test
public class ZeroRateSensitivityTest {

  public void test_of() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDate(), date(2015, 8, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getCurrency(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    assertSame(base.withCurrency(GBP), base);
    assertSame(base.withCurrency(USD), base);  // currency change ignored
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 20d);
    ZeroRateSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    ZeroRateSensitivity a1 = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity a2 = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity b = ZeroRateSensitivity.of(USD, date(2015, 8, 27), 32d);
    ZeroRateSensitivity c = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), 32d);
    IborRateSensitivity other = IborRateSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 32d);
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b) < 0, true);
    assertEquals(b.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(c) < 0, true);
    assertEquals(c.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(other) > 0, true);
    assertEquals(other.compareExcludingSensitivity(a1) < 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d * 3.5d);
    ZeroRateSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 1 / 32d);
    ZeroRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    ZeroRateSensitivity base1 = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    ZeroRateSensitivity base2 = ZeroRateSensitivity.of(GBP, date(2015, 10, 27), 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    coverImmutableBean(test);
    ZeroRateSensitivity test2 = ZeroRateSensitivity.of(USD, date(2015, 7, 27), 16d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, date(2015, 8, 27), 32d);
    assertSerialization(test);
  }

}
