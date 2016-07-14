/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Tests {@link IborFutureOptionSensitivity}.
 */
@Test
public class IborFutureOptionSensitivityTest {

  private static final IborFutureOptionVolatilitiesName NAME = IborFutureOptionVolatilitiesName.of("Test");
  private static final IborFutureOptionVolatilitiesName NAME2 = IborFutureOptionVolatilitiesName.of("Test2");

  public void test_of() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertEquals(test.getVolatilitiesName(), NAME);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), 12d);
    assertEquals(test.getFixingDate(), date(2015, 8, 28));
    assertEquals(test.getStrikePrice(), 0.98);
    assertEquals(test.getFuturePrice(), 0.99);
    assertEquals(test.getSensitivity(), 32d);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSame(base.withCurrency(GBP), base);

    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    IborFutureOptionSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 20d);
    IborFutureOptionSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    IborFutureOptionSensitivity a1 = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity a2 = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity b = IborFutureOptionSensitivity.of(
        NAME2, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity c = IborFutureOptionSensitivity.of(
        NAME, 13d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity d = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 9, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity e = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.99, 0.99, GBP, 32d);
    IborFutureOptionSensitivity f = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 1.00, GBP, 32d);
    IborFutureOptionSensitivity g = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(b.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(e) < 0, true);
    assertEquals(e.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(f) < 0, true);
    assertEquals(f.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(g) < 0, true);
    assertEquals(g.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    LocalDate fixingDate = date(2015, 8, 28);
    double strike = 0.98d;
    double forward = 0.99d;
    double sensi = 32d;
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, GBP, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IborFutureOptionSensitivity test1 = (IborFutureOptionSensitivity) base.convertedTo(USD, matrix);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, USD, sensi * rate);
    assertEquals(test1, expected);
    IborFutureOptionSensitivity test2 = (IborFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d * 3.5d);
    IborFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 1 / 32d);
    IborFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    coverImmutableBean(test);
    IborFutureOptionSensitivity test2 = IborFutureOptionSensitivity.of(
        NAME2, 13d, date(2015, 8, 29), 0.99, 0.995, USD, 33d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSerialization(test);
  }

}
