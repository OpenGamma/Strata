/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests {@link IborFutureOptionSensitivity}.
 */
@Test
public class IborFutureOptionSensitivityTest {

  public void test_of_noCurrency() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, 32d);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiryDate(), date(2015, 8, 27));
    assertEquals(test.getFixingDate(), date(2015, 8, 28));
    assertEquals(test.getStrikePrice(), 0.98);
    assertEquals(test.getFuturePrice(), 0.99);
    assertEquals(test.getSensitivity(), 32d);
  }

  public void test_of_withCurrency() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiryDate(), date(2015, 8, 27));
    assertEquals(test.getFixingDate(), date(2015, 8, 28));
    assertEquals(test.getStrikePrice(), 0.98);
    assertEquals(test.getFuturePrice(), 0.99);
    assertEquals(test.getSensitivity(), 32d);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSame(base.withCurrency(GBP), base);

    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    IborFutureOptionSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 20d);
    IborFutureOptionSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    IborFutureOptionSensitivity a1 = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity a2 = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity b = IborFutureOptionSensitivity.of(USD_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity c = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 9, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity d = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 9, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity e = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.99, 0.99, GBP, 32d);
    IborFutureOptionSensitivity f = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 1.00, GBP, 32d);
    IborFutureOptionSensitivity g = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), 32d);
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b) < 0, true);
    assertEquals(b.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(c) < 0, true);
    assertEquals(c.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(d) < 0, true);
    assertEquals(d.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(e) < 0, true);
    assertEquals(e.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(f) < 0, true);
    assertEquals(f.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(g) < 0, true);
    assertEquals(g.compareExcludingSensitivity(a1) > 0, true);    
    assertEquals(a1.compareExcludingSensitivity(other) < 0, true);    
    assertEquals(other.compareExcludingSensitivity(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d * 3.5d);
    IborFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 1 / 32d);
    IborFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    coverImmutableBean(test);
    IborFutureOptionSensitivity test2 = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(GBP_LIBOR_3M, date(2015, 8, 27), 
        date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSerialization(test);
  }
  
}
