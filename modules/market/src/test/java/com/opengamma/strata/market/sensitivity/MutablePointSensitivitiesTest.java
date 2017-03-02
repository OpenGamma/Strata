/*
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

import java.util.ArrayList;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Test {@link MutablePointSensitivities}.
 */
@Test
public class MutablePointSensitivitiesTest {

  private static final PointSensitivity CS1 = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 12d);
  private static final PointSensitivity CS2 = DummyPointSensitivity.of(GBP, date(2015, 7, 30), 22d);
  private static final PointSensitivity CS3 = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 32d);
  private static final PointSensitivity CS3B = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 3d);

  //-------------------------------------------------------------------------
  public void test_size_add_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities(), ImmutableList.of());
    test.add(CS1);
    assertEquals(test.size(), 1);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1));
    test.add(CS2);
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2));
  }

  public void test_size_addAll_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    assertEquals(test.getSensitivities(), ImmutableList.of());
    test.addAll(Lists.newArrayList(CS2, CS1));
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1));
  }

  public void test_construcor_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities(Lists.newArrayList(CS2, CS1));
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1));
  }

  //-------------------------------------------------------------------------
  public void test_addAll() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS2, CS1));
    MutablePointSensitivities test2 = new MutablePointSensitivities();
    test2.addAll(Lists.newArrayList(CS3));
    test.addAll(test2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1, CS3));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.withCurrency(USD);
    assertEquals(
        test.getSensitivities(),
        ImmutableList.of(CS3.withCurrency(USD), CS2.withCurrency(USD), CS1.withCurrency(USD)));
  }

  public void test_multiplyBy() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.multipliedBy(2d);
    assertEquals(
        test.getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(64d), CS2.withSensitivity(44d), CS1.withSensitivity(24d)));
  }

  public void test_mapSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.mapSensitivity(s -> s / 2);
    assertEquals(
        test.getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(16d), CS2.withSensitivity(11d), CS1.withSensitivity(6d)));
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    MutablePointSensitivities base1 = new MutablePointSensitivities(CS1);
    MutablePointSensitivities base2 = new MutablePointSensitivities(CS2);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.addAll(base1).addAll(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    MutablePointSensitivities base = new MutablePointSensitivities(CS1);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1));
  }

  public void test_buildInto_same() {
    MutablePointSensitivities base = new MutablePointSensitivities(CS1);
    MutablePointSensitivities test = base.buildInto(base);
    assertSame(test, base);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    MutablePointSensitivities base = new MutablePointSensitivities();
    PointSensitivities test = base.build();
    assertEquals(test, base.toImmutable());
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    MutablePointSensitivities base = new MutablePointSensitivities();
    base.add(CS3);
    MutablePointSensitivities test = base.cloned();
    base.add(CS2);
    test.add(CS1);

    MutablePointSensitivities baseExpected = new MutablePointSensitivities();
    baseExpected.addAll(Lists.newArrayList(CS3, CS2));
    assertEquals(base, baseExpected);

    MutablePointSensitivities testExpected = new MutablePointSensitivities();
    testExpected.addAll(Lists.newArrayList(CS3, CS1));
    assertEquals(test, testExpected);
  }

  //-------------------------------------------------------------------------
  public void test_sort() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.sort();
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2, CS3));
  }

  public void test_normalize() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1, CS3B));
    test.normalize();
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2, CS3.withSensitivity(35d)));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutable() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.toImmutable(), PointSensitivities.of(ImmutableList.of(CS3, CS2, CS1)));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutablePointSensitivities test2 = new MutablePointSensitivities();
    test2.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutablePointSensitivities test3 = new MutablePointSensitivities();
    test3.addAll(Lists.newArrayList(CS3, CS1));
    assertEquals(test.equals(test), true);
    assertEquals(test.equals(test2), true);
    assertEquals(test.equals(test3), false);
    assertEquals(test.equals("Bad"), false);
    assertEquals(test.equals(null), false);
    assertEquals(test.hashCode(), test2.hashCode());
  }

  public void test_toString() {
    ArrayList<PointSensitivity> list = Lists.newArrayList(CS3, CS2, CS1);
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(list);
    assertEquals(test.toString().contains(list.toString()), true);
  }

}
