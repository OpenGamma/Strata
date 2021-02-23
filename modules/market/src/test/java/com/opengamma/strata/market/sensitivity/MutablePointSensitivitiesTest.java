/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Test {@link MutablePointSensitivities}.
 */
public class MutablePointSensitivitiesTest {

  private static final PointSensitivity CS1 = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 12d);
  private static final PointSensitivity CS2 = DummyPointSensitivity.of(GBP, date(2015, 7, 30), 22d);
  private static final PointSensitivity CS3 = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 32d);
  private static final PointSensitivity CS3B = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 3d);
  private static final PointSensitivity CS3C = DummyPointSensitivity.of(GBP, date(2015, 8, 30), 10d);
  private static final PointSensitivity CS3D = DummyPointSensitivity.of(GBP, date(2015, 8, 30), -2d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_size_add_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).isEmpty();
    test.add(CS1);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsExactly(CS1);
    test.add(CS2);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(CS1, CS2);
  }

  @Test
  public void test_size_addAll_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    assertThat(test.getSensitivities()).isEmpty();
    test.addAll(Lists.newArrayList(CS2, CS1));
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(CS2, CS1);
  }

  @Test
  public void test_construcor_getSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities(Lists.newArrayList(CS2, CS1));
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(CS2, CS1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_addAll() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS2, CS1));
    MutablePointSensitivities test2 = new MutablePointSensitivities();
    test2.addAll(Lists.newArrayList(CS3));
    test.addAll(test2);
    assertThat(test.getSensitivities()).containsExactly(CS2, CS1, CS3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.withCurrency(USD);
    assertThat(test.getSensitivities())
        .containsExactly(CS3.withCurrency(USD), CS2.withCurrency(USD), CS1.withCurrency(USD));
  }

  @Test
  public void test_multiplyBy() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.multipliedBy(2d);
    assertThat(test.getSensitivities())
        .containsExactly(CS3.withSensitivity(64d), CS2.withSensitivity(44d), CS1.withSensitivity(24d));
  }

  @Test
  public void test_mapSensitivities() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.mapSensitivity(s -> s / 2);
    assertThat(test.getSensitivities())
        .containsExactly(CS3.withSensitivity(16d), CS2.withSensitivity(11d), CS1.withSensitivity(6d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    MutablePointSensitivities base1 = new MutablePointSensitivities(CS1);
    MutablePointSensitivities base2 = new MutablePointSensitivities(CS2);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.addAll(base1).addAll(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    MutablePointSensitivities base = new MutablePointSensitivities(CS1);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(CS1);
  }

  @Test
  public void test_buildInto_same() {
    MutablePointSensitivities base = new MutablePointSensitivities(CS1);
    MutablePointSensitivities test = base.buildInto(base);
    assertThat(test).isSameAs(base);
    assertThat(test.getSensitivities()).containsExactly(CS1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    MutablePointSensitivities base = new MutablePointSensitivities();
    PointSensitivities test = base.build();
    assertThat(test).isEqualTo(base.toImmutable());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    MutablePointSensitivities base = new MutablePointSensitivities();
    base.add(CS3);
    MutablePointSensitivities test = base.cloned();
    base.add(CS2);
    test.add(CS1);

    MutablePointSensitivities baseExpected = new MutablePointSensitivities();
    baseExpected.addAll(Lists.newArrayList(CS3, CS2));
    assertThat(base).isEqualTo(baseExpected);

    MutablePointSensitivities testExpected = new MutablePointSensitivities();
    testExpected.addAll(Lists.newArrayList(CS3, CS1));
    assertThat(test).isEqualTo(testExpected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_sort() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    test.sort();
    assertThat(test.getSensitivities()).containsExactly(CS1, CS2, CS3);
  }

  @Test
  public void test_normalize() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1, CS3B));
    test.normalize();
    assertThat(test.getSensitivities()).containsExactly(CS1, CS2, CS3.withSensitivity(35d));
  }

  @Test
  public void test_normalize_4() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS3D, CS1, CS3B, CS3C));
    test.normalize();
    assertThat(test.getSensitivities()).containsExactly(CS1, CS2, CS3.withSensitivity(43d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutable() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    assertThat(test.toImmutable()).isEqualTo(PointSensitivities.of(ImmutableList.of(CS3, CS2, CS1)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutablePointSensitivities test2 = new MutablePointSensitivities();
    test2.addAll(Lists.newArrayList(CS3, CS2, CS1));
    MutablePointSensitivities test3 = new MutablePointSensitivities();
    test3.addAll(Lists.newArrayList(CS3, CS1));
    assertThat(test.equals(test)).isTrue();
    assertThat(test.equals(test2)).isTrue();
    assertThat(test.equals(test3)).isFalse();
    assertThat(test.equals(ANOTHER_TYPE)).isFalse();
    assertThat(test.equals(null)).isFalse();
    assertThat(test.hashCode()).isEqualTo(test2.hashCode());
  }

  @Test
  public void test_toString() {
    ArrayList<PointSensitivity> list = Lists.newArrayList(CS3, CS2, CS1);
    MutablePointSensitivities test = new MutablePointSensitivities();
    test.addAll(list);
    assertThat(test.toString().contains(list.toString())).isTrue();
  }

}
