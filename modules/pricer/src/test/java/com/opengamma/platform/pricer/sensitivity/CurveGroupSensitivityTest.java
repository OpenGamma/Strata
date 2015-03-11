/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.platform.pricer.sensitivity.CurveGroupSensitivity;
import com.opengamma.platform.pricer.sensitivity.CurveSensitivity;
import com.opengamma.platform.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test.
 */
@Test
public class CurveGroupSensitivityTest {

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

  public void test_of() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS1, CS2));
    assertEquals(test.getSensitivities(), ImmutableList.of(CS1, CS2));
    assertEquals(test.size(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS2, CS1));
    CurveGroupSensitivity test2 = CurveGroupSensitivity.of(Lists.newArrayList(CS3));
    assertEquals(test.combinedWith(test2).getSensitivities(), ImmutableList.of(CS2, CS1, CS3));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(
        test.multipliedBy(2d).getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(64d), CS2.withSensitivity(44d), CS1.withSensitivity(24d)));
  }

  public void test_mapSensitivities() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(
        test.mapSensitivities(s -> s / 2).getSensitivities(),
        ImmutableList.of(CS3.withSensitivity(16d), CS2.withSensitivity(11d), CS1.withSensitivity(6d)));
  }

  //-------------------------------------------------------------------------
  public void test_cleaned_sorts() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.cleaned().getSensitivities(), ImmutableList.of(CS1, CS2, CS3));
  }

  public void test_cleaned_merges() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1, CS3B));
    assertEquals(test.cleaned().getSensitivities(), ImmutableList.of(CS1, CS2, CS3.withSensitivity(35d)));
  }

  public void test_cleaned_empty() {
    assertEquals(CurveGroupSensitivity.EMPTY.cleaned(), CurveGroupSensitivity.EMPTY);
  }

  //-------------------------------------------------------------------------
  public void test_toMutable() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    assertEquals(test.toMutable().getSensitivities(), ImmutableList.of(CS3, CS2, CS1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    coverImmutableBean(test);
    CurveGroupSensitivity test2 = CurveGroupSensitivity.of(Lists.newArrayList(CS1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupSensitivity test = CurveGroupSensitivity.of(Lists.newArrayList(CS3, CS2, CS1));
    assertSerialization(test);
  }

}
