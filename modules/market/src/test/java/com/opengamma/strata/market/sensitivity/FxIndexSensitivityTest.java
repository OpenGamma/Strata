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
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_GBP;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
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

/**
 * Test {@link FxIndexSensitivity}.
 */
@Test
public class FxIndexSensitivityTest {

  private static final LocalDate FIXING_DATE = date(2015, 8, 27);
  private static final double SENSITIVITY_VALUE = 1.342d;

  public void test_of() {
    FxIndexSensitivity test = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    assertEquals(test.getBaseCurrency(), USD);
    assertEquals(test.getCounterCurrency(), GBP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getSensitivity(), SENSITIVITY_VALUE);
  }

  public void test_of_noCurrency() {
    FxIndexSensitivity test = FxIndexSensitivity.of(WM_GBP_USD, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    assertEquals(test.getBaseCurrency(), GBP);
    assertEquals(test.getCounterCurrency(), USD);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getSensitivity(), SENSITIVITY_VALUE);
  }

  public void test_of_wrongBaseCurrency() {
    assertThrowsIllegalArg(() -> FxIndexSensitivity.of(WM_GBP_USD, JPY, FIXING_DATE, SENSITIVITY_VALUE));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    assertSame(base.withCurrency(GBP), base);

    FxIndexSensitivity test = base.withCurrency(JPY);
    FxIndexSensitivity expected = FxIndexSensitivity.of(WM_GBP_USD, JPY, USD, FIXING_DATE, SENSITIVITY_VALUE);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.withSensitivity(2.5d);
    FxIndexSensitivity expected = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, 2.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    FxIndexSensitivity a1 = FxIndexSensitivity.of(WM_GBP_USD, USD, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity a2 = FxIndexSensitivity.of(WM_GBP_USD, USD, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity b = FxIndexSensitivity.of(ECB_EUR_GBP, USD, GBP, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity c = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity d = FxIndexSensitivity.of(WM_GBP_USD, JPY, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity e = FxIndexSensitivity.of(WM_GBP_USD, USD, date(2015, 9, 27), SENSITIVITY_VALUE);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), SENSITIVITY_VALUE);
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b) > 0, true);
    assertEquals(b.compareExcludingSensitivity(a1) < 0, true);
    assertEquals(a1.compareExcludingSensitivity(c) > 0, true);
    assertEquals(c.compareExcludingSensitivity(a1) < 0, true);
    assertEquals(a1.compareExcludingSensitivity(d) > 0, true);
    assertEquals(d.compareExcludingSensitivity(a1) < 0, true);
    assertEquals(a1.compareExcludingSensitivity(e) > 0, true);
    assertEquals(e.compareExcludingSensitivity(a1) < 0, true);
    assertEquals(a1.compareExcludingSensitivity(other) < 0, true);
    assertEquals(other.compareExcludingSensitivity(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.multipliedBy(2.4d);
    FxIndexSensitivity expected = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE * 2.4d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.mapSensitivity(s -> 1d / s);
    FxIndexSensitivity expected = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, 1d / SENSITIVITY_VALUE);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.normalize();
    assertEquals(test, test);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    FxIndexSensitivity base1 = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity base2 = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, 4.25d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    FxIndexSensitivity base = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    FxIndexSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxIndexSensitivity test1 = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    coverImmutableBean(test1);
    FxIndexSensitivity test2 = FxIndexSensitivity.of(ECB_EUR_GBP, GBP, EUR, date(2015, 9, 27), 4.25d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxIndexSensitivity test = FxIndexSensitivity.of(WM_GBP_USD, GBP, USD, FIXING_DATE, SENSITIVITY_VALUE);
    assertSerialization(test);
  }

}
