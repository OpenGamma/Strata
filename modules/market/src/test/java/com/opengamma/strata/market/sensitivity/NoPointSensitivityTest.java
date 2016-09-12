/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test.
 */
@Test
public class NoPointSensitivityTest {

  public void test_withCurrency() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertSame(base.withCurrency(GBP), base);  // no effect
    assertSame(base.withCurrency(USD), base);  // no effect
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertSame(base.multipliedBy(2.0), base);  // no effect
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertSame(base.mapSensitivity(s -> 2.0), base);  // no effect
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder ibor = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 2.0d);
    assertSame(base.combinedWith(ibor), ibor);  // returns other
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_toString() {
    PointSensitivityBuilder test = PointSensitivityBuilder.none();
    assertEquals(test.toString(), "NoPointSensitivity");
  }

}
