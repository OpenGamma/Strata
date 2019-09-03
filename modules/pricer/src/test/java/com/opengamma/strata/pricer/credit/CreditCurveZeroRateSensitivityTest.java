/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

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
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;

/**
 * Test {@link CreditCurveZeroRateSensitivity}.
 */
public class CreditCurveZeroRateSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final double YEAR_FRACTION = 5d;
  private static final double VALUE = 1.5d;

  @Test
  public void test_of_full() {
    CreditCurveZeroRateSensitivity test = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, GBP, VALUE);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getCurveCurrency()).isEqualTo(USD);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  public void test_of() {
    CreditCurveZeroRateSensitivity test = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, VALUE);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurveCurrency()).isEqualTo(USD);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  public void test_of_ZeroRateSensitivity() {
    ZeroRateSensitivity zeroPoint = ZeroRateSensitivity.of(USD, YEAR_FRACTION, GBP, VALUE);
    CreditCurveZeroRateSensitivity test = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, zeroPoint);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getCurveCurrency()).isEqualTo(USD);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(test.toZeroRateSensitivity()).isEqualTo(zeroPoint);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, VALUE);
    assertThat(base.withCurrency(USD)).isSameAs(base);
    assertThat(base.withCurrency(GBP)).isEqualTo(CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, GBP, VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, VALUE);
    CreditCurveZeroRateSensitivity expected = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, 20d);
    CreditCurveZeroRateSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    CreditCurveZeroRateSensitivity a1 = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity a2 = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity b = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, 10d, 32d);
    CreditCurveZeroRateSensitivity c = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity d = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, USD, YEAR_FRACTION, GBP, 32d);
    IborRateSensitivity other = IborRateSensitivity.of(IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA), 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    double sensi = 32d;
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    CreditCurveZeroRateSensitivity test1 = base.convertedTo(USD, matrix);
    CreditCurveZeroRateSensitivity expected =
        CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, USD, rate * sensi);
    assertThat(test1).isEqualTo(expected);
    CreditCurveZeroRateSensitivity test2 = base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity expected = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d * 3.5d);
    CreditCurveZeroRateSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity expected = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 1 / 32d);
    CreditCurveZeroRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    CreditCurveZeroRateSensitivity base1 = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity base2 = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    CreditCurveZeroRateSensitivity base = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    CreditCurveZeroRateSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CreditCurveZeroRateSensitivity test = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    coverImmutableBean(test);
    CreditCurveZeroRateSensitivity test2 = CreditCurveZeroRateSensitivity.of(StandardId.of("OG", "AAA"), USD, YEAR_FRACTION, 16d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    CreditCurveZeroRateSensitivity test = CreditCurveZeroRateSensitivity.of(LEGAL_ENTITY, GBP, YEAR_FRACTION, 32d);
    assertSerialization(test);
  }

}
