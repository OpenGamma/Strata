/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
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

/**
 * Test {@link FxIndexSensitivity}.
 */
@Test
public class FxIndexSensitivityTest {

  private static final LocalDate FIXING_DATE = date(2015, 8, 27);
  private static final double SENSITIVITY_VALUE = 1.342d;

  public void test_of() {
    FxIndexSensitivity test = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    assertEquals(test.getReferenceCurrency(), USD);
    assertEquals(test.getReferenceCounterCurrency(), GBP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getSensitivity(), SENSITIVITY_VALUE);
  }

  public void test_of_noCurrency() {
    FxIndexSensitivity test = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceCounterCurrency(), USD);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getSensitivity(), SENSITIVITY_VALUE);
  }

  public void test_of_wrongBaseCurrency() {
    assertThrowsIllegalArg(() -> FxIndexSensitivity.of(GBP_USD_WM, JPY, FIXING_DATE, SENSITIVITY_VALUE));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    assertSame(base.withCurrency(GBP), base);

    FxIndexSensitivity test = base.withCurrency(JPY);
    FxIndexSensitivity expected = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, JPY, SENSITIVITY_VALUE);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.withSensitivity(2.5d);
    FxIndexSensitivity expected = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, 2.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    FxIndexSensitivity a1 = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, USD, SENSITIVITY_VALUE);
    FxIndexSensitivity a2 = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, USD, SENSITIVITY_VALUE);
    FxIndexSensitivity b = FxIndexSensitivity.of(EUR_GBP_ECB, GBP, FIXING_DATE, USD, SENSITIVITY_VALUE);
    FxIndexSensitivity c = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity d = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, JPY, SENSITIVITY_VALUE);
    FxIndexSensitivity e = FxIndexSensitivity.of(GBP_USD_WM, USD, date(2015, 9, 27), SENSITIVITY_VALUE);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), SENSITIVITY_VALUE);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) > 0, true);
    assertEquals(b.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(c) > 0, true);
    assertEquals(c.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(d) > 0, true);
    assertEquals(d.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(e) > 0, true);
    assertEquals(e.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    double rate = 1.35d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(EUR, USD), rate);
    FxIndexSensitivity test1 = (FxIndexSensitivity) base.convertedTo(EUR, matrix);
    FxIndexSensitivity expected = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, EUR, SENSITIVITY_VALUE / rate);
    assertEquals(test1, expected);
    FxIndexSensitivity test2 = (FxIndexSensitivity) base.convertedTo(USD, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_toFxForwardSensitivity() {
    FxIndexSensitivity test = FxIndexSensitivity.of(GBP_USD_WM, GBP, FIXING_DATE, USD, SENSITIVITY_VALUE);
    FxForwardSensitivity expected = FxForwardSensitivity.of(
        CurrencyPair.of(GBP, USD), GBP, GBP_USD_WM.calculateMaturityFromFixing(FIXING_DATE), USD, SENSITIVITY_VALUE);
    assertEquals(test.toFxForwardSensitivity(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.multipliedBy(2.4d);
    FxIndexSensitivity expected = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE * 2.4d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.mapSensitivity(s -> 1d / s);
    FxIndexSensitivity expected = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, 1d / SENSITIVITY_VALUE);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.normalize();
    assertEquals(test, test);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    FxIndexSensitivity base1 = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity base2 = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, 4.25d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    FxIndexSensitivity base = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxIndexSensitivity test1 = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    coverImmutableBean(test1);
    FxIndexSensitivity test2 = FxIndexSensitivity.of(EUR_GBP_ECB, EUR, date(2015, 9, 27), GBP, 4.25d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxIndexSensitivity test = FxIndexSensitivity.of(GBP_USD_WM, USD, FIXING_DATE, GBP, SENSITIVITY_VALUE);
    assertSerialization(test);
  }

}
