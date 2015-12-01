/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.meta;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test {@link YearMonthCurveNodeMetadata}.
 */
@Test
public class YearMonthCurveNodeMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);
  private static final YearMonth JAN2015 = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015);
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan15");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  public void test_of_label() {
    YearMonthCurveNodeMetadata test = YearMonthCurveNodeMetadata.of(DATE, JAN2015, "Jan 2015");
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan 2015");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends YearMonthCurveNodeMetadata> builder = YearMonthCurveNodeMetadata.meta().builder();
    builder.set(YearMonthCurveNodeMetadata.meta().date(), DATE);
    builder.set(YearMonthCurveNodeMetadata.meta().yearMonth(), JAN2015);
    YearMonthCurveNodeMetadata test = builder.build();
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan15");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends YearMonthCurveNodeMetadata> builder = YearMonthCurveNodeMetadata.meta().builder();
    builder.set(YearMonthCurveNodeMetadata.meta().date(), DATE);
    builder.set(YearMonthCurveNodeMetadata.meta().yearMonth(), JAN2015);
    builder.set(YearMonthCurveNodeMetadata.meta().label(), "Jan 2015");
    YearMonthCurveNodeMetadata test = builder.build();
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan 2015");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends YearMonthCurveNodeMetadata> builder = YearMonthCurveNodeMetadata.meta().builder();
    builder.set(YearMonthCurveNodeMetadata.meta().date(), DATE);
    assertThrowsIllegalArg(() -> builder.build());
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

}
