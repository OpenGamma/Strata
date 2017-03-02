/*
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;

/**
 * Test {@link PointSensitivities}.
 */
@Test
public class PointSensitivitiesTest {

  private static final PointSensitivity CS1 = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 12d);
  private static final PointSensitivity CS2 = DummyPointSensitivity.of(GBP, date(2015, 7, 30), 22d);
  private static final PointSensitivity CS3 = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 32d);
  private static final PointSensitivity CS3B = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 3d);
  private static final PointSensitivity CS4 = DummyPointSensitivity.of(GBP, date(2015, 8, 30), USD, 4d);

  public void test_of_array() {
    PointSensitivities test = PointSensitivities.of(CS1, CS2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2));
    assertEquals(test.size(), 2);
  }

  public void test_of_List() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS1, CS2));
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2));
    assertEquals(test.size(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS2, CS1));
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS3));
    assertEquals(test.combinedWith(test2).getSensitivities(), ImmutableList.of(CS2, CS1, CS3));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(
        test.multipliedBy(2d).getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(64d), CS2.withSensitivity(44d), CS1.withSensitivity(24d)));
  }

  public void test_mapSensitivities() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(
        test.mapSensitivities(s -> s / 2).getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(16d), CS2.withSensitivity(11d), CS1.withSensitivity(6d)));
  }

  //-------------------------------------------------------------------------
  public void test_normalized_sorts() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.normalized().getSensitivities(), ImmutableList.of(CS1, CS2, CS3));
  }

  public void test_normalized_merges() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1, CS3B));
    assertEquals(test.normalized().getSensitivities(), ImmutableList.of(CS1, CS2, CS3.withSensitivity(35d)));
  }

  public void test_normalized_empty() {
    assertEquals(PointSensitivities.empty().normalized(), PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance_length() {
    PointSensitivities test1 = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1)).normalized();
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS3, CS2)).normalized();
    assertFalse(test1.equalWithTolerance(test2, 1.0E+1));
  }

  public void test_equalWithTolerance_date() {
    PointSensitivities test1 = PointSensitivities.of(Lists.newArrayList(CS3, CS1)).normalized();
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS3, CS2)).normalized();
    assertFalse(test1.equalWithTolerance(test2, 1.0E+1));
  }

  public void test_equalWithTolerance_value() {
    PointSensitivities test1 = PointSensitivities.of(Lists.newArrayList(CS3, CS1)).normalized();
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS3B, CS1)).normalized();
    assertFalse(test1.equalWithTolerance(test2, 1.0E+1));
  }

  public void test_equalWithTolerance_true() {
    PointSensitivity cs1b = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 12.1d);
    PointSensitivities test1 = PointSensitivities.of(Lists.newArrayList(CS3, CS1)).normalized();
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS3, cs1b)).normalized();
    assertTrue(test1.equalWithTolerance(test2, 1.0E-1));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    PointSensitivities base = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    PointSensitivities test1 = base.convertedTo(USD, matrix);
    PointSensitivity c1Conv = CS1.convertedTo(USD, matrix);
    PointSensitivity c2Conv = CS2.convertedTo(USD, matrix);
    PointSensitivity c3Conv = CS3.convertedTo(USD, matrix);
    PointSensitivities expected = PointSensitivities.of(Lists.newArrayList(c3Conv, c2Conv, c1Conv));
    assertEquals(test1.normalized(), expected.normalized());
    PointSensitivities test2 = base.convertedTo(GBP, matrix);
    assertEquals(test2.normalized(), base.normalized());
  }

  public void test_convertedTo_multipleCurrency() {
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    PointSensitivities base = PointSensitivities.of(Lists.newArrayList(CS4, CS3, CS1));
    PointSensitivities test1 = base.convertedTo(USD, matrix);
    PointSensitivity c1Conv = CS1.convertedTo(USD, matrix);
    PointSensitivity c3Conv = CS3.convertedTo(USD, matrix);
    PointSensitivity c3c4Usd = c3Conv.withSensitivity(c3Conv.getSensitivity() + CS4.getSensitivity());
    PointSensitivities expected1 = PointSensitivities.of(Lists.newArrayList(c3c4Usd, c1Conv));
    assertEquals(test1.normalized(), expected1.normalized());
    PointSensitivities test2 = base.convertedTo(GBP, matrix);
    PointSensitivity c4Conv = CS4.convertedTo(GBP, matrix);
    PointSensitivity c3c4GBP = CS3.withSensitivity(CS3.getSensitivity() + c4Conv.getSensitivity());
    PointSensitivities expected2 = PointSensitivities.of(Lists.newArrayList(c3c4GBP, CS1));
    assertEquals(test2.normalized(), expected2.normalized());
  }

  public void test_convertedTo_empty() {
    assertEquals(PointSensitivities.empty().convertedTo(GBP, FxMatrix.empty()), PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_toMutable() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.toMutable().getSensitivities(), ImmutableList.of(CS3, CS2, CS1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    coverImmutableBean(test);
    PointSensitivities test2 = PointSensitivities.of(Lists.newArrayList(CS1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PointSensitivities test = PointSensitivities.of(Lists.newArrayList(CS3, CS2, CS1));
    assertSerialization(test);
  }

}
