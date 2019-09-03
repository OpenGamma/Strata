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

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class NoPointSensitivityTest {

  @Test
  public void test_withCurrency() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertThat(base.withCurrency(GBP)).isSameAs(base);  // no effect
    assertThat(base.withCurrency(USD)).isSameAs(base);  // no effect
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertThat(base.multipliedBy(2.0)).isSameAs(base);  // no effect
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    assertThat(base.mapSensitivity(s -> 2.0)).isSameAs(base);  // no effect
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder ibor = DummyPointSensitivity.of(GBP, date(2015, 6, 30), 2.0d);
    assertThat(base.combinedWith(ibor)).isSameAs(ibor);  // returns other
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    PointSensitivityBuilder base = PointSensitivityBuilder.none();
    PointSensitivityBuilder test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toString() {
    PointSensitivityBuilder test = PointSensitivityBuilder.none();
    assertThat(test.toString()).isEqualTo("NoPointSensitivity");
  }

}
