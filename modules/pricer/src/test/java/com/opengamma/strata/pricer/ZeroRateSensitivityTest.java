/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;

/**
 * Test {@link ZeroRateSensitivity}.
 */
public class ZeroRateSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double YEARFRAC = 2d;
  private static final double YEARFRAC2 = 3d;

  @Test
  public void test_of() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);
    assertThat(base.withCurrency(USD)).isEqualTo(ZeroRateSensitivity.of(GBP, YEARFRAC, USD, 32d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, YEARFRAC, 20d);
    ZeroRateSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    ZeroRateSensitivity a1 = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity a2 = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity b = ZeroRateSensitivity.of(USD, YEARFRAC, 32d);
    ZeroRateSensitivity c = ZeroRateSensitivity.of(GBP, YEARFRAC2, 32d);
    IborRateSensitivity other = IborRateSensitivity.of(IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA), 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) > 0).isTrue();
    assertThat(other.compareKey(a1) < 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    double sensi = 32d;
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    ZeroRateSensitivity test1 = (ZeroRateSensitivity) base.convertedTo(USD, matrix);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, YEARFRAC, USD, rate * sensi);
    assertThat(test1).isEqualTo(expected);
    ZeroRateSensitivity test2 = (ZeroRateSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d * 3.5d);
    ZeroRateSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, YEARFRAC, 1 / 32d);
    ZeroRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    ZeroRateSensitivity base1 = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity base2 = ZeroRateSensitivity.of(GBP, YEARFRAC2, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    ZeroRateSensitivity base = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    ZeroRateSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    coverImmutableBean(test);
    ZeroRateSensitivity test2 = ZeroRateSensitivity.of(USD, YEARFRAC2, 16d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ZeroRateSensitivity test = ZeroRateSensitivity.of(GBP, YEARFRAC, 32d);
    assertSerialization(test);
  }

}
