/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * Test {@link FxOptionSensitivity}.
 */
@Test
public class FxOptionSensitivityTest {

  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 1, 23);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE);
  private static final CurrencyPair PAIR = CurrencyPair.of(EUR, GBP);
  private static final double FORWARD = 0.8d;
  private static final double STRIKE = 0.95d;
  private static final double SENSI_VALUE = 1.24d;

  public void test_of() {
    FxOptionSensitivity test = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiryDateTime(), EXPIRY_DATE_TIME);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getCurrencyPair(), PAIR);
    assertEquals(test.getSensitivity(), SENSI_VALUE);
    assertEquals(test.getStrike(), STRIKE);
  }

  public void test_withCurrency() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test1 = base.withCurrency(EUR);
    assertEquals(test1.getCurrency(), EUR);
    assertEquals(test1.getExpiryDateTime(), EXPIRY_DATE_TIME);
    assertEquals(test1.getForward(), FORWARD);
    assertEquals(test1.getCurrencyPair(), PAIR);
    assertEquals(test1.getSensitivity(), SENSI_VALUE);
    assertEquals(test1.getStrike(), STRIKE);
    FxOptionSensitivity test2 = base.withCurrency(GBP);
    assertEquals(test2, base);
  }

  public void test_withSensitivity() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    double newSensi = 22.5;
    FxOptionSensitivity test = base.withSensitivity(newSensi);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiryDateTime(), EXPIRY_DATE_TIME);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getCurrencyPair(), PAIR);
    assertEquals(test.getSensitivity(), newSensi);
    assertEquals(test.getStrike(), STRIKE);
  }

  public void test_compareExcludingSensitivity() {
    FxOptionSensitivity a1 = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity a2 = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity b = FxOptionSensitivity.of(
        CurrencyPair.of(EUR, USD), EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity c = FxOptionSensitivity.of(PAIR, ZonedDateTime.of(
        LocalDate.of(2015, 2, 23), LocalTime.of(10, 15), ZoneId.of("Z")), STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity d = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, 0.96, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity e = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, 0.81, GBP, SENSI_VALUE);
    FxOptionSensitivity f = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, EUR, SENSI_VALUE);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, LocalDate.of(2015, 9, 27), 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(b.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(e) < 0, true);
    assertEquals(e.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(f) > 0, true);
    assertEquals(f.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  public void test_multipliedBy() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    double factor = 5.2d;
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE * factor);
    FxOptionSensitivity test = base.multipliedBy(factor);
    assertEquals(test, expected);
  }

  public void test_mapSensitivity() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity expected = FxOptionSensitivity.of(
        PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, 1.0 / SENSI_VALUE);
    FxOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  public void test_normalize() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test = base.normalize();
    assertSame(test, base);
  }

  public void test_buildInto() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  public void test_build() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  public void test_cloned() {
    FxOptionSensitivity base = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    FxOptionSensitivity test = base.cloned();
    assertSame(test, base);
  }

  public void coverage() {
    FxOptionSensitivity test1 = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    coverImmutableBean(test1);
    FxOptionSensitivity test2 = FxOptionSensitivity.of(CurrencyPair.of(EUR, USD), EXPIRY_DATE_TIME, 0.8, 0.9, EUR, 1.1);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxOptionSensitivity test = FxOptionSensitivity.of(PAIR, EXPIRY_DATE_TIME, STRIKE, FORWARD, GBP, SENSI_VALUE);
    assertSerialization(test);
  }

}
