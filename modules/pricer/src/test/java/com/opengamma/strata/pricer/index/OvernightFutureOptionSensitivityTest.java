/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Tests {@link OvernightFutureOptionSensitivity}.
 */
public class OvernightFutureOptionSensitivityTest {

  private static final OvernightFutureOptionVolatilitiesName NAME = OvernightFutureOptionVolatilitiesName.of("Test");
  private static final OvernightFutureOptionVolatilitiesName NAME2 = OvernightFutureOptionVolatilitiesName.of("Test2");

  @Test
  public void test_of() {
    OvernightFutureOptionSensitivity test = OvernightFutureOptionSensitivity.of(
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
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    OvernightFutureOptionSensitivity expected = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, USD, 32d);
    OvernightFutureOptionSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity expected = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 20d);
    OvernightFutureOptionSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    OvernightFutureOptionSensitivity a1 = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity a2 = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity b = OvernightFutureOptionSensitivity.of(
        NAME2, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity c = OvernightFutureOptionSensitivity.of(
        NAME, 13d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity d = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 9, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity e = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.99, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity f = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 1.00, GBP, 32d);
    OvernightFutureOptionSensitivity g = OvernightFutureOptionSensitivity.of(
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
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, GBP, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    OvernightFutureOptionSensitivity test1 = (OvernightFutureOptionSensitivity) base.convertedTo(USD, matrix);
    OvernightFutureOptionSensitivity expected = OvernightFutureOptionSensitivity.of(
        NAME, 12d, fixingDate, strike, forward, USD, sensi * rate);
    assertThat(test1).isEqualTo(expected);
    OvernightFutureOptionSensitivity test2 = (OvernightFutureOptionSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity expected = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d * 3.5d);
    OvernightFutureOptionSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity expected = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 1 / 32d);
    OvernightFutureOptionSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    OvernightFutureOptionSensitivity base = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    OvernightFutureOptionSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightFutureOptionSensitivity test = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    coverImmutableBean(test);
    OvernightFutureOptionSensitivity test2 = OvernightFutureOptionSensitivity.of(
        NAME2, 13d, date(2015, 8, 29), 0.99, 0.995, USD, 33d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightFutureOptionSensitivity test = OvernightFutureOptionSensitivity.of(
        NAME, 12d, date(2015, 8, 28), 0.98, 0.99, GBP, 32d);
    assertSerialization(test);
  }

}
