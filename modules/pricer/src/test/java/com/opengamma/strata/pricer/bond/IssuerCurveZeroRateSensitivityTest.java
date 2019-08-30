/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;

/**
 * Test {@link IssuerCurveZeroRateSensitivity}.
 */
public class IssuerCurveZeroRateSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double YEARFRAC = 2d;
  private static final double YEARFRAC2 = 3d;
  private static final double VALUE = 32d;
  private static final Currency CURRENCY = USD;
  private static final LegalEntityGroup GROUP = LegalEntityGroup.of("ISSUER1");

  @Test
  public void test_of_withSensitivityCurrency() {
    Currency sensiCurrency = GBP;
    IssuerCurveZeroRateSensitivity test = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, sensiCurrency, GROUP, VALUE);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurveCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getCurrency()).isEqualTo(sensiCurrency);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
  }

  @Test
  public void test_of_withoutSensitivityCurrency() {
    IssuerCurveZeroRateSensitivity test = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurveCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
  }

  @Test
  public void test_of_zeroRateSensitivity() {
    Currency sensiCurrency = GBP;
    ZeroRateSensitivity zeroSensi = ZeroRateSensitivity.of(CURRENCY, YEARFRAC, sensiCurrency, VALUE);
    IssuerCurveZeroRateSensitivity test = IssuerCurveZeroRateSensitivity.of(zeroSensi, GROUP);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurveCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getCurrency()).isEqualTo(sensiCurrency);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity test = base.withCurrency(GBP);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurveCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(VALUE);
  }

  @Test
  public void test_withSensitivity() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    double newValue = 53d;
    IssuerCurveZeroRateSensitivity test = base.withSensitivity(newValue);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurveCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getCurrency()).isEqualTo(CURRENCY);
    assertThat(test.getYearFraction()).isEqualTo(YEARFRAC);
    assertThat(test.getSensitivity()).isEqualTo(newValue);
  }

  @Test
  public void test_compareKey() {
    IssuerCurveZeroRateSensitivity a1 = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity a2 = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity b = IssuerCurveZeroRateSensitivity.of(GBP, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity c = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC2, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity d =
        IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, LegalEntityGroup.of("ISSUER2"), VALUE);
    IborRateSensitivity other = IborRateSensitivity.of(
        IborIndexObservation.of(GBP_LIBOR_3M, date(2015, 8, 27), REF_DATA), 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) > 0).isTrue();
    assertThat(b.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) > 0).isTrue();
    assertThat(other.compareKey(a1) < 0).isTrue();
  }

  @Test
  public void test_convertedTo() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IssuerCurveZeroRateSensitivity test1 = base.convertedTo(USD, matrix);
    assertThat(test1).isEqualTo(base);
    IssuerCurveZeroRateSensitivity test2 = base.convertedTo(GBP, matrix);
    IssuerCurveZeroRateSensitivity expected =
        IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GBP, GROUP, VALUE / rate);
    assertThat(test2).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    double rate = 2.4d;
    IssuerCurveZeroRateSensitivity test = base.multipliedBy(rate);
    IssuerCurveZeroRateSensitivity expected = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE * rate);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_mapSensitivity() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity expected = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, 1d / VALUE);
    IssuerCurveZeroRateSensitivity test = base.mapSensitivity(s -> 1d / s);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_normalize() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity test = base.normalize();
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_buildInto() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  @Test
  public void test_cloned() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    IssuerCurveZeroRateSensitivity test = base.cloned();
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_createZeroRateSensitivity() {
    IssuerCurveZeroRateSensitivity base = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GBP, GROUP, VALUE);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(CURRENCY, YEARFRAC, GBP, VALUE);
    ZeroRateSensitivity test = base.createZeroRateSensitivity();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IssuerCurveZeroRateSensitivity test1 = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    coverImmutableBean(test1);
    IssuerCurveZeroRateSensitivity test2 =
        IssuerCurveZeroRateSensitivity.of(GBP, YEARFRAC2, LegalEntityGroup.of("ISSUER1"), 12d);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IssuerCurveZeroRateSensitivity test = IssuerCurveZeroRateSensitivity.of(CURRENCY, YEARFRAC, GROUP, VALUE);
    assertSerialization(test);
  }

}
