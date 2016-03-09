/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.product.SecurityId;

/**
 * Test {@link BondFutureOptionSensitivity}.
 */
@Test
public class BondFutureOptionSensitivityTest {
  private static final SecurityId FUTURE_SECURITY_ID = SecurityId.of("OG-Ticker", "GOVT1-BOND-FUT");
  private static final ZonedDateTime OPTION_EXPIRY = dateUtc(2015, 8, 27);
  private static final LocalDate FUTURE_EXPIRY = date(2015, 8, 28);
  private static final double STRIKE_PRICE = 0.98;
  private static final double FUTURE_PRICE = 0.99;
  private static final double SENSITIVITY = 32d;

  public void test_of() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertEquals(test.getFutureSecurityId(), FUTURE_SECURITY_ID);
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
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertSame(base.withCurrency(GBP), base);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY);
    BondFutureOptionSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 20d);
    BondFutureOptionSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    BondFutureOptionSensitivity a1 = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity a2 = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity b = BondFutureOptionSensitivity.of(SecurityId.of("OG-Ticker", "FOO-BOND-FUT"),
        OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity c = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, dateUtc(2015, 7, 27), FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity d = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, date(2015, 9, 28), STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity e = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, 0.995, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity f = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, 0.975, GBP, SENSITIVITY);
    BondFutureOptionSensitivity g = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) > 0, true);
    assertEquals(b.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(c) > 0, true);
    assertEquals(c.compareKey(a1) < 0, true);
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
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    BondFutureOptionSensitivity test1 = (BondFutureOptionSensitivity) base.convertedTo(USD, matrix);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(FUTURE_SECURITY_ID, OPTION_EXPIRY,
        FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, USD, SENSITIVITY * rate);
    assertEquals(test1, expected);
    BondFutureOptionSensitivity test2 = (BondFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY * 3.5d);
    BondFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity expected = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, 1 / SENSITIVITY);
    BondFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    BondFutureOptionSensitivity base = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    BondFutureOptionSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BondFutureOptionSensitivity test1 = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    coverImmutableBean(test1);
    BondFutureOptionSensitivity test2 = BondFutureOptionSensitivity.of(
        SecurityId.of("OG-Ticker", "FOO-BOND-FUT"),
        dateUtc(2015, 9, 27),
        date(2015, 9, 28),
        0.985,
        0.995,
        USD,
        SENSITIVITY);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    BondFutureOptionSensitivity test = BondFutureOptionSensitivity.of(
        FUTURE_SECURITY_ID, OPTION_EXPIRY, FUTURE_EXPIRY, STRIKE_PRICE, FUTURE_PRICE, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
