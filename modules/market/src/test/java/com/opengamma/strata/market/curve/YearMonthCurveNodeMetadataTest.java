/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

import org.testng.annotations.Test;

/**
 * Test {@link YearMonthCurveNodeMetadata}.
 */
@Test
public class YearMonthCurveNodeMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);
  private static final YearMonth JAN2015 = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015);
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getDescription()).isEqualTo("Jan15");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015);
    coverImmutableBean(test);
    YearMonthCurveNodeMetadata test2 = YearMonthCurveNodeMetadata.of(date(2014, 1, 1), YearMonth.of(2016, 2));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015);
    assertSerialization(test);
  }

  public void test_identifier() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015);
    assertThat(test.getIdentifier()).isEqualTo(YearMonth.of(2015, Month.JANUARY));
  }
}
