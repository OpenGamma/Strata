/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test {@link TenorDateParameterMetadata}.
 */
@Test
public class TenorDateParameterMetadataTest {

  private static final LocalDate DATE = date(2015, 7, 30);

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    TenorDateParameterMetadata test = TenorDateParameterMetadata.of(DATE, TENOR_10Y);
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10Y");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_of_label() {
    TenorDateParameterMetadata test = TenorDateParameterMetadata.of(DATE, TENOR_10Y, "10 year");
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10 year");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends TenorDateParameterMetadata> builder = TenorDateParameterMetadata.meta().builder();
    builder.set(TenorDateParameterMetadata.meta().date(), DATE);
    builder.set(TenorDateParameterMetadata.meta().tenor(), TENOR_10Y);
    TenorDateParameterMetadata test = builder.build();
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10Y");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends TenorDateParameterMetadata> builder = TenorDateParameterMetadata.meta().builder();
    builder.set(TenorDateParameterMetadata.meta().date(), DATE);
    builder.set(TenorDateParameterMetadata.meta().tenor(), TENOR_10Y);
    builder.set(TenorDateParameterMetadata.meta().label(), "10 year");
    TenorDateParameterMetadata test = builder.build();
    assertEquals(test.getDate(), DATE);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10 year");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends TenorDateParameterMetadata> builder = TenorDateParameterMetadata.meta().builder();
    builder.set(TenorDateParameterMetadata.meta().date(), DATE);
    assertThrowsIllegalArg(() -> builder.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorDateParameterMetadata test = TenorDateParameterMetadata.of(DATE, TENOR_10Y);
    coverImmutableBean(test);
    TenorDateParameterMetadata test2 = TenorDateParameterMetadata.of(date(2014, 1, 1), TENOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorDateParameterMetadata test = TenorDateParameterMetadata.of(DATE, TENOR_10Y);
    assertSerialization(test);
  }

}
