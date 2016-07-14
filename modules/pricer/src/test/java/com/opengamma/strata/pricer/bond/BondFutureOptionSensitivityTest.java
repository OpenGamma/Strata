/**
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
 * Test {@link BondFutureOptionSensitivity}.
 */
@Test
public class BondFutureOptionSensitivityTest {
  private static final BondFutureVolatilitiesName NAME = BondFutureVolatilitiesName.of("GOVT1-BOND-FUT");
  private static final double OPTION_EXPIRY = 1d;
  private static final LocalDate FUTURE_EXPIRY = date(2015, 8, 28);
  private static final double STRIKE_PRICE = 0.98;
  private static final double FUTURE_PRICE = 0.99;
  private static final double SENSITIVITY = 32d;

  public void test_of() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertEquals(test.getVolatilitiesName(), NAME);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), OPTION_EXPIRY);
    assertEquals(test.getFutureExpiryDate(), FUTURE_EXPIRY);
    assertEquals(test.getStrikePrice(), STRIKE_PRICE);
    assertEquals(test.getFuturePrice(), FUTURE_PRICE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertSame(base.withCurrency(GBP), base);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY);
    BondFutureOptionSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 20d);
    BondFutureOptionSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
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
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) > 0, true);
    assertEquals(b.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(e) < 0, true);
    assertEquals(e.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(f) > 0, true);
    assertEquals(f.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(g) < 0, true);
    assertEquals(g.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    BondFutureOptionSensitivity test1 = (BondFutureOptionSensitivity) base.convertedTo(USD, matrix);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(NAME, OPTION_EXPIRY,
        FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY * rate);
    assertEquals(test1, expected);
    BondFutureOptionSensitivity test2 = (BondFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY * 3.5d);
    BondFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 1 / SENSITIVITY);
    BondFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        NAME, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
