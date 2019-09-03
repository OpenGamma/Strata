/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link IborRateSensitivity}.
 */
public class IborRateSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE = date(2015, 8, 27);
  private static final LocalDate DATE2 = date(2015, 9, 27);
  private static final IborIndexObservation GBP_LIBOR_3M_OBSERVATION =
      IborIndexObservation.of(GBP_LIBOR_3M, DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBSERVATION2 =
      IborIndexObservation.of(GBP_LIBOR_3M, DATE2, REF_DATA);

  @Test
  public void test_of_noCurrency() {
    IborRateSensitivity test = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
  }

  @Test
  public void test_of_withCurrency() {
    IborRateSensitivity test = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, GBP, 32d);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, USD, 32d);
    IborRateSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 20d);
    IborRateSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    IborRateSensitivity a1 = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity a2 = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity b = IborRateSensitivity.of(IborIndexObservation.of(USD_LIBOR_3M, DATE2, REF_DATA), 32d);
    IborRateSensitivity c = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, USD, 32d);
    IborRateSensitivity d = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION2, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    double sensi = 32d;
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IborRateSensitivity test1 = (IborRateSensitivity) base.convertedTo(USD, matrix);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, USD, sensi * rate);
    assertThat(test1).isEqualTo(expected);
    IborRateSensitivity test2 = (IborRateSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d * 3.5d);
    IborRateSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 1 / 32d);
    IborRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    IborRateSensitivity base1 = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity base2 = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION2, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    IborRateSensitivity base = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    IborRateSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborRateSensitivity test = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    coverImmutableBean(test);
    IborRateSensitivity test2 = IborRateSensitivity.of(
        IborIndexObservation.of(USD_LIBOR_3M, DATE2, REF_DATA), USD, 16d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborRateSensitivity test = IborRateSensitivity.of(GBP_LIBOR_3M_OBSERVATION, 32d);
    assertSerialization(test);
  }

}
