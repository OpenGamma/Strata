/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link InflationRateSensitivity}.
 */
@Test
public class InflationRateSensitivityTest {

  private static final YearMonth REFERENCE_MONTH = YearMonth.of(2015, 6);
  private static final PriceIndexObservation GB_HICP_OBS = PriceIndexObservation.of(GB_HICP, REFERENCE_MONTH);
  private static final PriceIndexObservation CH_CPI_OBS = PriceIndexObservation.of(CH_CPI, REFERENCE_MONTH);

  public void test_of_withoutCurrency() {
    InflationRateSensitivity test = InflationRateSensitivity.of(GB_HICP_OBS, 1.0);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getCurrency(), GB_HICP.getCurrency());
    assertEquals(test.getObservation(), GB_HICP_OBS);
    assertEquals(test.getSensitivity(), 1.0);
  }

  public void test_of_withCurrency() {
    InflationRateSensitivity test = InflationRateSensitivity.of(CH_CPI_OBS, GBP, 3.5);
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getObservation(), CH_CPI_OBS);
    assertEquals(test.getSensitivity(), 3.5);
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    InflationRateSensitivity base = InflationRateSensitivity.of(CH_CPI_OBS, 3.5);
    assertEquals(base.withCurrency(CHF), base);
    InflationRateSensitivity expected = InflationRateSensitivity.of(CH_CPI_OBS, USD, 3.5);
    InflationRateSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    InflationRateSensitivity base = InflationRateSensitivity.of(CH_CPI_OBS, 3.5);
    InflationRateSensitivity expected = InflationRateSensitivity.of(CH_CPI_OBS, 23.4);
    InflationRateSensitivity test = base.withSensitivity(23.4);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareKey() {
    InflationRateSensitivity a1 = InflationRateSensitivity.of(GB_HICP_OBS, 32d);
    InflationRateSensitivity a2 = InflationRateSensitivity.of(GB_HICP_OBS, 32d);
    InflationRateSensitivity b = InflationRateSensitivity.of(CH_CPI_OBS, 32d);
    InflationRateSensitivity c = InflationRateSensitivity.of(GB_HICP_OBS, USD, 32d);
    InflationRateSensitivity d = InflationRateSensitivity.of(PriceIndexObservation.of(GB_HICP, YearMonth.of(2015, 10)), 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertEquals(a1.compareKey(a2), 0);
    assertEquals(a1.compareKey(b) > 0, true);
    assertEquals(b.compareKey(a1) < 0, true);
    assertEquals(a1.compareKey(c) < 0, true);
    assertEquals(c.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(d) < 0, true);
    assertEquals(d.compareKey(a1) > 0, true);
    assertEquals(a1.compareKey(other) < 0, true);
    assertEquals(other.compareKey(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    double sensi = 32d;
    InflationRateSensitivity base = InflationRateSensitivity.of(GB_HICP_OBS, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    InflationRateSensitivity test1 = (InflationRateSensitivity) base.convertedTo(USD, matrix);
    InflationRateSensitivity expected = InflationRateSensitivity.of(GB_HICP_OBS, USD, sensi * rate);
    assertEquals(test1, expected);
    InflationRateSensitivity test2 = (InflationRateSensitivity) base.convertedTo(GBP, matrix);
    assertEquals(test2, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    InflationRateSensitivity base = InflationRateSensitivity.of(CH_CPI_OBS, 5.0);
    InflationRateSensitivity expected = InflationRateSensitivity.of(CH_CPI_OBS, 2.6 * 5.0);
    InflationRateSensitivity test = base.multipliedBy(2.6d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    InflationRateSensitivity base = InflationRateSensitivity.of(CH_CPI_OBS, 5.0);
    InflationRateSensitivity expected = InflationRateSensitivity.of(CH_CPI_OBS, 1.0 / 5.0);
    InflationRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    InflationRateSensitivity base = InflationRateSensitivity.of(GB_HICP_OBS, 3.5);
    InflationRateSensitivity test = base.normalize();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    InflationRateSensitivity base1 = InflationRateSensitivity.of(CH_CPI_OBS, 5.0);
    InflationRateSensitivity base2 = InflationRateSensitivity.of(CH_CPI_OBS, 7.0);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    InflationRateSensitivity base = InflationRateSensitivity.of(CH_CPI_OBS, 5.0);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    InflationRateSensitivity base = InflationRateSensitivity.of(GB_HICP_OBS, 3.5);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertEquals(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    InflationRateSensitivity base = InflationRateSensitivity.of(GB_HICP_OBS, 3.5);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    InflationRateSensitivity base = InflationRateSensitivity.of(GB_HICP_OBS, 3.5);
    InflationRateSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationRateSensitivity test1 = InflationRateSensitivity.of(GB_HICP_OBS, 1.0);
    coverImmutableBean(test1);
    InflationRateSensitivity test2 = InflationRateSensitivity.of(GB_HICP_OBS, GBP, 22.0);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationRateSensitivity test = InflationRateSensitivity.of(GB_HICP_OBS, 1.0);
    assertSerialization(test);
  }

}
