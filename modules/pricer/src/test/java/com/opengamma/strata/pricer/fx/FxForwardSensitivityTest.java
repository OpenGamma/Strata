/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
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
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link FxForwardSensitivity}.
 */
@Test
public class FxForwardSensitivityTest {

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);
  private static final LocalDate REFERENCE_DATE = LocalDate.of(2015, 11, 23);
  private static final double SENSITIVITY = 1.34d;

  public void test_of_withoutCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  public void test_of_withCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, REFERENCE_DATE, USD, SENSITIVITY);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), GBP);
    assertEquals(test.getReferenceCurrency(), EUR);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  public void test_of_wrongRefCurrency() {
    assertThrowsIllegalArg(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, REFERENCE_DATE, SENSITIVITY));
    assertThrowsIllegalArg(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, REFERENCE_DATE, USD, SENSITIVITY));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency_same() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(EUR);
    assertEquals(test, base);
  }

  public void test_withCurrency_other() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(USD);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withSensitivity(13.5d);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), 13.5d);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    FxForwardSensitivity a1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity a2 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity b = FxForwardSensitivity.of(CurrencyPair.of(GBP, USD), GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity c = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, REFERENCE_DATE, GBP, SENSITIVITY);
    FxForwardSensitivity d = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, JPY, SENSITIVITY);
    FxForwardSensitivity e = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, date(2015, 9, 27), SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, SENSITIVITY);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) < 0, true);
    assertEquals(b.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(e) > 0, true);
    assertEquals(e.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    double rate = 1.4d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(EUR, USD), rate);
    FxForwardSensitivity test1 = (FxForwardSensitivity) base.convertedTo(USD, matrix);
    FxForwardSensitivity expected = FxForwardSensitivity.of(
        CURRENCY_PAIR, GBP, REFERENCE_DATE, USD, SENSITIVITY * rate);
    assertEquals(test1, expected);
    FxForwardSensitivity test2 = (FxForwardSensitivity) base.convertedTo(EUR, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.multipliedBy(2.4d);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY * 2.4d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.mapSensitivity(s -> 1d / s);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1d / SENSITIVITY);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.normalize();
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    FxForwardSensitivity base1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity base2 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1.56d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxForwardSensitivity test1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    coverImmutableBean(test1);
    FxForwardSensitivity test2 = FxForwardSensitivity.of(CurrencyPair.of(USD, JPY), JPY, date(2015, 9, 27), 4.25d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertSerialization(test);
  }

}
