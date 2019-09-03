/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Tests {@link IborFutureOptionSensitivity}.
 */
public class IborFutureOptionSensitivityTest {

  private static final IborFutureOptionVolatilitiesName NAME = IborFutureOptionVolatilitiesName.of("Test");
  private static final IborFutureOptionVolatilitiesName NAME2 = IborFutureOptionVolatilitiesName.of("Test2");

  @Test
  public void test_of() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertThat(test.getVolatilitiesName()).isEqualTo(NAME);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getExpiry()).isEqualTo(12d);
    assertThat(test.getFixingDate()).isEqualTo(date(2015, 8, 28));
    assertThat(test.getStrikePrice()).isEqualTo(0.98);
    assertThat(test.getFuturePrice()).isEqualTo(0.99);
    assertThat(test.getSensitivity()).isEqualTo(32d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    IborFutureOptionSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 20d);
    IborFutureOptionSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    IborFutureOptionSensitivity a1 = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity a2 = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity b = IborFutureOptionSensitivity.of(
        NAME2, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity c = IborFutureOptionSensitivity.of(
        NAME, 13d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity d = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 9, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity e = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.99, 0.99, GBP, 32d);
    IborFutureOptionSensitivity f = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 1.00, GBP, 32d);
    IborFutureOptionSensitivity g = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(e.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(f) < 0).isTrue();
    assertThat(f.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(g) < 0).isTrue();
    assertThat(g.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    LocalDate fixingDate = date(2015, 8, 28);
    double strike = 0.98d;
    double forward = 0.99d;
    double sensi = 32d;
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, GBP, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    IborFutureOptionSensitivity test1 = (IborFutureOptionSensitivity) base.convertedTo(USD, matrix);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, USD, sensi * rate);
    assertThat(test1).isEqualTo(expected);
    IborFutureOptionSensitivity test2 = (IborFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d * 3.5d);
    IborFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity expected = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 1 / 32d);
    IborFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    IborFutureOptionSensitivity base = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    IborFutureOptionSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    coverImmutableBean(test);
    IborFutureOptionSensitivity test2 = IborFutureOptionSensitivity.of(
        NAME2, 13d, date(2015, 8, 29), 0.99, 0.995, USD, 33d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborFutureOptionSensitivity test = IborFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSerialization(test);
  }

}
