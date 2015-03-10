/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.platform.pricer.sensitivity.CurveGroupSensitivity;
import com.opengamma.platform.pricer.sensitivity.CurveSensitivity;
import com.opengamma.platform.pricer.sensitivity.MutableCurveGroupSensitivity;
import com.opengamma.platform.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test.
 */
@Test
public class MutableCurveGroupSensitivityTest {

  private static final CurveSensitivity CS1 = ZeroRateSensitivity.builder()
      .currency(GBP)
      .date(date(2015, 6, 30))
      .sensitivity(12d)
      .build();
  private static final CurveSensitivity CS2 = ZeroRateSensitivity.builder()
      .currency(GBP)
      .date(date(2015, 7, 30))
      .sensitivity(22d)
      .build();
  private static final CurveSensitivity CS3 = ZeroRateSensitivity.builder()
      .currency(GBP)
      .date(date(2015, 8, 30))
      .sensitivity(32d)
      .build();
  private static final CurveSensitivity CS3B = ZeroRateSensitivity.builder()
      .currency(GBP)
      .date(date(2015, 8, 30))
      .sensitivity(3d)
      .build();

  //-------------------------------------------------------------------------
  public void test_size_add_getSensitivities() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
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
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    assertEquals(test.getSensitivities(), ImmutableList.of());
    test.addAll(Lists.newArrayList(CS2, CS1));
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1));
  }

  public void test_construcor_getSensitivities() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity(Lists.newArrayList(CS2, CS1));
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1));
  }

  //-------------------------------------------------------------------------
  public void test_merge() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS2, CS1));
    MutableCurveGroupSensitivity test2 = new MutableCurveGroupSensitivity();
    test2.addAll(Lists.newArrayList(CS3));
    test.merge(test2);
    assertEquals(test.getSensitivities(), ImmutableList.of(CS2, CS1, CS3));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.multipliedBy(2d);
    assertEquals(
        test.getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(64d), CS2.withSensitivity(44d), CS1.withSensitivity(24d)));
  }

  public void test_mapSensitivities() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.mapSensitivities(s -> s / 2);
    assertEquals(
        test.getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(16d), CS2.withSensitivity(11d), CS1.withSensitivity(6d)));
  }

  //-------------------------------------------------------------------------
  public void test_sort() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.sort();
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2, CS3));
  }

  public void test_clean() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1, CS3B));
    test.clean();
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2, CS3.withSensitivity(35d)));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutable() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.toImmutable(), CurveGroupSensitivity.of(ImmutableList.of(CS3, CS2, CS1)));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutableCurveGroupSensitivity test2 = new MutableCurveGroupSensitivity();
    test2.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutableCurveGroupSensitivity test3 = new MutableCurveGroupSensitivity();
    test3.addAll(Lists.newArrayList(CS3, CS1));
    assertEquals(test.equals(test), true);
    assertEquals(test.equals(test2), true);
    assertEquals(test.equals(test3), false);
    assertEquals(test.equals("Bad"), false);
    assertEquals(test.equals(null), false);
    assertEquals(test.hashCode(), test2.hashCode());
  }

  public void test_toString() {
    ArrayList<CurveSensitivity> list = Lists.newArrayList(CS3, CS2, CS1);
    MutableCurveGroupSensitivity test = new MutableCurveGroupSensitivity();
    test.addAll(list);
    assertEquals(test.toString().contains(list.toString()), true);
  }

}
