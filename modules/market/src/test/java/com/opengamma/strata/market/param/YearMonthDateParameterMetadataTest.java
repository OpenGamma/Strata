/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test {@link YearMonthDateParameterMetadata}.
 */
@Test
public class YearMonthDateParameterMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);
  private static final YearMonth JAN2015 = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getYearMonth(), JAN2015);
    assertEquals(test.getLabel(), "Jan15");
    assertEquals(test.getIdentifier(), JAN2015);
  }

  public void test_of_label() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015, "Jan 2015");
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getYearMonth(), JAN2015);
    assertEquals(test.getLabel(), "Jan 2015");
    assertEquals(test.getIdentifier(), JAN2015);
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    builder.set(YearMonthDateParameterMetadata.meta().yearMonth(), JAN2015);
    YearMonthDateParameterMetadata test = builder.build();
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getYearMonth(), JAN2015);
    assertEquals(test.getLabel(), "Jan15");
    assertEquals(test.getIdentifier(), JAN2015);
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    builder.set(YearMonthDateParameterMetadata.meta().yearMonth(), JAN2015);
    builder.set(YearMonthDateParameterMetadata.meta().label(), "Jan 2015");
    YearMonthDateParameterMetadata test = builder.build();
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getYearMonth(), JAN2015);
    assertEquals(test.getLabel(), "Jan 2015");
    assertEquals(test.getIdentifier(), JAN2015);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    assertThrowsIllegalArg(() -> builder.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    coverImmutableBean(test);
    YearMonthDateParameterMetadata test2 = YearMonthDateParameterMetadata.of(date(2014, 1, 1), YearMonth.of(2016, 2));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    assertSerialization(test);
  }

}
