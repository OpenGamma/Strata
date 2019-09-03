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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link FxForwardSensitivity}.
 */
public class FxForwardSensitivityTest {

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);
  private static final LocalDate REFERENCE_DATE = LocalDate.of(2015, 11, 23);
  private static final double SENSITIVITY = 1.34d;

  @Test
  public void test_of_withoutCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getReferenceCounterCurrency()).isEqualTo(EUR);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
    assertThat(test.getReferenceDate()).isEqualTo(REFERENCE_DATE);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  @Test
  public void test_of_withCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, REFERENCE_DATE, USD, SENSITIVITY);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getReferenceCounterCurrency()).isEqualTo(GBP);
    assertThat(test.getReferenceCurrency()).isEqualTo(EUR);
    assertThat(test.getReferenceDate()).isEqualTo(REFERENCE_DATE);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  @Test
  public void test_of_wrongRefCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, REFERENCE_DATE, SENSITIVITY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, REFERENCE_DATE, USD, SENSITIVITY));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency_same() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(EUR);
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_withCurrency_other() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(USD);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getReferenceCounterCurrency()).isEqualTo(EUR);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
    assertThat(test.getReferenceDate()).isEqualTo(REFERENCE_DATE);
    assertThat(test.getSensitivity()).isEqualTo(SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withSensitivity(13.5d);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getReferenceCounterCurrency()).isEqualTo(EUR);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
    assertThat(test.getReferenceDate()).isEqualTo(REFERENCE_DATE);
    assertThat(test.getSensitivity()).isEqualTo(13.5d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    FxForwardSensitivity a1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity a2 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity b = FxForwardSensitivity.of(CurrencyPair.of(GBP, USD), GBP, REFERENCE_DATE, EUR, SENSITIVITY);
    FxForwardSensitivity c = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, REFERENCE_DATE, GBP, SENSITIVITY);
    FxForwardSensitivity d = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, JPY, SENSITIVITY);
    FxForwardSensitivity e = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, date(2015, 9, 27), SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, SENSITIVITY);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) > 0).isTrue();
    assertThat(e.compareKey(a1) < 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    double rate = 1.4d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(EUR, USD), rate);
    FxForwardSensitivity test1 = (FxForwardSensitivity) base.convertedTo(USD, matrix);
    FxForwardSensitivity expected = FxForwardSensitivity.of(
        CURRENCY_PAIR, GBP, REFERENCE_DATE, USD, SENSITIVITY * rate);
    assertThat(test1).isEqualTo(expected);
    FxForwardSensitivity test2 = (FxForwardSensitivity) base.convertedTo(EUR, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.multipliedBy(2.4d);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY * 2.4d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.mapSensitivity(s -> 1d / s);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1d / SENSITIVITY);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.normalize();
    assertThat(test).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    FxForwardSensitivity base1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity base2 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1.56d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxForwardSensitivity test1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    coverImmutableBean(test1);
    FxForwardSensitivity test2 = FxForwardSensitivity.of(CurrencyPair.of(USD, JPY), JPY, date(2015, 9, 27), 4.25d);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertSerialization(test);
  }

}
