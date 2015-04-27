/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test.
 */
@Test
public class OvernightRateSensitivityTest {

  public void test_of_noCurrencyFindMaturityDate() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFixingDate(), date(2015, 8, 27));
    assertEquals(test.getEndDate(), date(2015, 8, 28));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
  }

  public void test_of() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(
        GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 10, 27), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFixingDate(), date(2015, 8, 27));
    assertEquals(test.getEndDate(), date(2015, 10, 27));
    assertEquals(test.getSensitivity(), 32d);
    assertEquals(test.getIndex(), GBP_SONIA);
  }

  public void test_badDateOrder() {
    assertThrowsIllegalArg(() -> OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 8, 27), 32d));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    assertSame(base.withCurrency(GBP), base);

    LocalDate mat = GBP_SONIA.calculateMaturityFromEffective(GBP_SONIA.calculateEffectiveFromFixing(date(2015, 8, 27)));
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, USD, date(2015, 8, 27), mat, 32d);
    OvernightRateSensitivity test = base.withCurrency(USD);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 20d);
    OvernightRateSensitivity test = base.withSensitivity(20d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    OvernightRateSensitivity a1 = OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 10, 27), 32d);
    OvernightRateSensitivity a2 = OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 10, 27), 32d);
    OvernightRateSensitivity b = OvernightRateSensitivity.of(USD_FED_FUND, GBP, date(2015, 8, 27), date(2015, 10, 27), 32d);
    OvernightRateSensitivity c = OvernightRateSensitivity.of(GBP_SONIA, USD, date(2015, 8, 27), date(2015, 10, 27), 32d);
    OvernightRateSensitivity d = OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 9, 27), date(2015, 10, 27), 32d);
    OvernightRateSensitivity e = OvernightRateSensitivity.of(GBP_SONIA, GBP, date(2015, 8, 27), date(2015, 11, 27), 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), 32d);
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b) < 0, true);
    assertEquals(b.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(c) < 0, true);
    assertEquals(c.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(e) < 0, true);
    assertEquals(d.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(d) < 0, true);
    assertEquals(e.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(other) < 0, true);
    assertEquals(other.compareExcludingSensitivity(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d * 3.5d);
    OvernightRateSensitivity test = base.multipliedBy(3.5d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 1 / 32d);
    OvernightRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    OvernightRateSensitivity base1 = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    OvernightRateSensitivity base2 = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 10, 27), 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    coverImmutableBean(test);
    OvernightRateSensitivity test2 = OvernightRateSensitivity.of(USD_FED_FUND, date(2015, 9, 27), 16d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA, date(2015, 8, 27), 32d);
    assertSerialization(test);
  }

}
