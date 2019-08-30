/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Test {@link OvernightRateSensitivity}.
 */
public class OvernightRateSensitivityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE = date(2015, 8, 27);
  private static final LocalDate DATE2 = date(2015, 9, 27);
  private static final OvernightIndexObservation GBP_SONIA_OBSERVATION =
      OvernightIndexObservation.of(GBP_SONIA, DATE, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_OBSERVATION2 =
      OvernightIndexObservation.of(GBP_SONIA, DATE2, REF_DATA);
  private static final OvernightIndexObservation USD_FED_FUND_OBSERVATION2 =
      OvernightIndexObservation.of(USD_FED_FUND, DATE2, REF_DATA);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_noCurrency() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getEndDate()).isEqualTo(date(2015, 8, 28));
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
  }

  @Test
  public void test_of_currency() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, USD, 32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getEndDate()).isEqualTo(date(2015, 8, 28));
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
  }

  @Test
  public void test_ofPeriod() {
    OvernightRateSensitivity test = OvernightRateSensitivity.ofPeriod(
        GBP_SONIA_OBSERVATION, date(2015, 10, 27), GBP, 32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getEndDate()).isEqualTo(date(2015, 10, 27));
    assertThat(test.getSensitivity()).isEqualTo(32d);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
  }

  @Test
  public void test_badDateOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, DATE, GBP, 32d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withCurrency() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    assertThat(base.withCurrency(GBP)).isSameAs(base);

    LocalDate mat = GBP_SONIA_OBSERVATION.getMaturityDate();
    OvernightRateSensitivity expected = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, mat, USD, 32d);
    OvernightRateSensitivity test = base.withCurrency(USD);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 20d);
    OvernightRateSensitivity test = base.withSensitivity(20d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareKey() {
    OvernightRateSensitivity a1 = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, date(2015, 10, 27), GBP, 32d);
    OvernightRateSensitivity a2 = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, date(2015, 10, 27), GBP, 32d);
    OvernightRateSensitivity b = OvernightRateSensitivity.ofPeriod(USD_FED_FUND_OBSERVATION2, date(2015, 10, 27), GBP, 32d);
    OvernightRateSensitivity c = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, date(2015, 10, 27), USD, 32d);
    OvernightRateSensitivity d = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION2, date(2015, 10, 27), GBP, 32d);
    OvernightRateSensitivity e = OvernightRateSensitivity.ofPeriod(GBP_SONIA_OBSERVATION, date(2015, 11, 27), GBP, 32d);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, 2d, 32d);
    assertThat(a1.compareKey(a2)).isEqualTo(0);
    assertThat(a1.compareKey(b) < 0).isTrue();
    assertThat(b.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(c) < 0).isTrue();
    assertThat(c.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(e) < 0).isTrue();
    assertThat(d.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(d) < 0).isTrue();
    assertThat(e.compareKey(a1) > 0).isTrue();
    assertThat(a1.compareKey(other) < 0).isTrue();
    assertThat(other.compareKey(a1) > 0).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    LocalDate fixingDate = DATE;
    LocalDate endDate = date(2015, 10, 27);
    double sensi = 32d;
    OvernightRateSensitivity base = OvernightRateSensitivity.ofPeriod(
        OvernightIndexObservation.of(GBP_SONIA, fixingDate, REF_DATA), endDate, GBP, sensi);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    OvernightRateSensitivity test1 = (OvernightRateSensitivity) base.convertedTo(USD, matrix);
    OvernightRateSensitivity expected = OvernightRateSensitivity.ofPeriod(
        OvernightIndexObservation.of(GBP_SONIA, fixingDate, REF_DATA), endDate, USD, rate * sensi);
    assertThat(test1).isEqualTo(expected);
    OvernightRateSensitivity test2 = (OvernightRateSensitivity) base.convertedTo(GBP, matrix);
    assertThat(test2).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d * 3.5d);
    OvernightRateSensitivity test = base.multipliedBy(3.5d);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapSensitivity() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 1 / 32d);
    OvernightRateSensitivity test = base.mapSensitivity(s -> 1 / s);
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity test = base.normalize();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    OvernightRateSensitivity base1 = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity base2 = OvernightRateSensitivity.of(
        OvernightIndexObservation.of(GBP_SONIA, date(2015, 10, 27), REF_DATA), 22d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_mutable() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_buildInto() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertThat(test).isSameAs(combo);
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_build() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    PointSensitivities test = base.build();
    assertThat(test.getSensitivities()).containsExactly(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cloned() {
    OvernightRateSensitivity base = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    OvernightRateSensitivity test = base.cloned();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    coverImmutableBean(test);
    OvernightRateSensitivity test2 = OvernightRateSensitivity.of(USD_FED_FUND_OBSERVATION2, 16d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightRateSensitivity test = OvernightRateSensitivity.of(GBP_SONIA_OBSERVATION, 32d);
    assertSerialization(test);
  }

}
