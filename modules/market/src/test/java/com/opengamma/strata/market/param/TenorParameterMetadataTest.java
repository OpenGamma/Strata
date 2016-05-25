/**
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
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test {@link TenorParameterMetadata}.
 */
@Test
public class TenorParameterMetadataTest {

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    TenorParameterMetadata test = TenorParameterMetadata.of(TENOR_10Y);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10Y");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_of_label() {
    TenorParameterMetadata test = TenorParameterMetadata.of(TENOR_10Y, "10 year");
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10 year");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends TenorParameterMetadata> builder = TenorParameterMetadata.meta().builder();
    builder.set(TenorParameterMetadata.meta().tenor(), TENOR_10Y);
    TenorParameterMetadata test = builder.build();
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10Y");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends TenorParameterMetadata> builder = TenorParameterMetadata.meta().builder();
    builder.set(TenorParameterMetadata.meta().tenor(), TENOR_10Y);
    builder.set(TenorParameterMetadata.meta().label(), "10 year");
    TenorParameterMetadata test = builder.build();
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getLabel(), "10 year");
    assertEquals(test.getIdentifier(), TENOR_10Y);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends TenorParameterMetadata> builder = TenorParameterMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorParameterMetadata test = TenorParameterMetadata.of(TENOR_10Y);
    coverImmutableBean(test);
    TenorParameterMetadata test2 = TenorParameterMetadata.of(TENOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorParameterMetadata test = TenorParameterMetadata.of(TENOR_10Y);
    assertSerialization(test);
  }

}
