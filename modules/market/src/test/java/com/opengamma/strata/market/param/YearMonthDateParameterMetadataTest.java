/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test {@link YearMonthDateParameterMetadata}.
 */
public class YearMonthDateParameterMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);
  private static final YearMonth JAN2015 = YearMonth.of(2015, 1);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_noLabel() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan15");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  @Test
  public void test_of_label() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015, "Jan 2015");
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan 2015");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  @Test
  public void test_builder_defaultLabel() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    builder.set(YearMonthDateParameterMetadata.meta().yearMonth(), JAN2015);
    YearMonthDateParameterMetadata test = builder.build();
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan15");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  @Test
  public void test_builder_specifyLabel() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    builder.set(YearMonthDateParameterMetadata.meta().yearMonth(), JAN2015);
    builder.set(YearMonthDateParameterMetadata.meta().label(), "Jan 2015");
    YearMonthDateParameterMetadata test = builder.build();
    assertThat(test.getDate()).isEqualTo(DATE);
    assertThat(test.getYearMonth()).isEqualTo(JAN2015);
    assertThat(test.getLabel()).isEqualTo("Jan 2015");
    assertThat(test.getIdentifier()).isEqualTo(JAN2015);
  }

  @Test
  public void test_builder_incomplete() {
    BeanBuilder<? extends YearMonthDateParameterMetadata> builder = YearMonthDateParameterMetadata.meta().builder();
    builder.set(YearMonthDateParameterMetadata.meta().date(), DATE);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    coverImmutableBean(test);
    YearMonthDateParameterMetadata test2 = YearMonthDateParameterMetadata.of(date(2014, 1, 1), YearMonth.of(2016, 2));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    YearMonthDateParameterMetadata test = YearMonthDateParameterMetadata.of(DATE, JAN2015);
    assertSerialization(test);
  }

}
