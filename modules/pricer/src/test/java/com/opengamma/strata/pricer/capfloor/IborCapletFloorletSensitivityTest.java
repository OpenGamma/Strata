/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link IborCapletFloorletSensitivity}.
 */
@Test
public class IborCapletFloorletSensitivityTest {

  private static final ZonedDateTime EXPIRY_DATE = dateUtc(2015, 8, 27);
  private static final double FORWARD = 0.015d;
  private static final double STRIKE = 0.001d;
  private static final double SENSITIVITY = 0.54d;

  public void test_of_noCurrency() {
    IborCapletFloorletSensitivity test =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, SENSITIVITY);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), EXPIRY_DATE);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  public void test_of_withCurrency() {
    IborCapletFloorletSensitivity test =
        IborCapletFloorletSensitivity.of(USD_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), EXPIRY_DATE);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getForward(), FORWARD);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, SENSITIVITY);
    assertSame(base.withCurrency(GBP), base);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, USD, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(USD_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, SENSITIVITY);
    double sensi = 23.5;
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(USD_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, sensi);
    IborCapletFloorletSensitivity test = base.withSensitivity(sensi);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    IborCapletFloorletSensitivity a1 =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity a2 =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity b =
        IborCapletFloorletSensitivity.of(USD_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity c =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE.plusDays(2), STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity d =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, 0.009, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity e =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, 0.005, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity f =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, USD, SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(b.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(e) > 0, true);
    assertEquals(e.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(f) < 0, true);
    assertEquals(f.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IborCapletFloorletSensitivity test1 = base.convertedTo(USD, matrix);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, USD, SENSITIVITY * rate);
    assertEquals(test1, expected);
    IborCapletFloorletSensitivity test2 = base.convertedTo(GBP, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    double factor = 3.5d;
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY * factor);
    IborCapletFloorletSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity expected =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, 1d / SENSITIVITY);
    IborCapletFloorletSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    IborCapletFloorletSensitivity base =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    IborCapletFloorletSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborCapletFloorletSensitivity test1 =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    coverImmutableBean(test1);
    IborCapletFloorletSensitivity test2 =
        IborCapletFloorletSensitivity.of(USD_LIBOR_3M, dateUtc(2015, 8, 28), 0.98, 0.99, 32d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborCapletFloorletSensitivity test =
        IborCapletFloorletSensitivity.of(GBP_LIBOR_3M, EXPIRY_DATE, STRIKE, FORWARD, GBP, SENSITIVITY);
    assertSerialization(test);
  }

}
