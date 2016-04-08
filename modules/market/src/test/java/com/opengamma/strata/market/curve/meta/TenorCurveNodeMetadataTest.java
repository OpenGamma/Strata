/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.meta;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test {@link TenorCurveNodeMetadata}.
 */
@Test
public class TenorCurveNodeMetadataTest {

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(TENOR_10Y);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getLabel()).isEqualTo("10Y");
    assertThat(test.getIdentifier()).isEqualTo(TENOR_10Y);
  }

  public void test_of_label() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(TENOR_10Y, "10 year");
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getLabel()).isEqualTo("10 year");
    assertThat(test.getIdentifier()).isEqualTo(TENOR_10Y);
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends TenorCurveNodeMetadata> builder = TenorCurveNodeMetadata.meta().builder();
    builder.set(TenorCurveNodeMetadata.meta().tenor(), TENOR_10Y);
    TenorCurveNodeMetadata test = builder.build();
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getLabel()).isEqualTo("10Y");
    assertThat(test.getIdentifier()).isEqualTo(TENOR_10Y);
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends TenorCurveNodeMetadata> builder = TenorCurveNodeMetadata.meta().builder();
    builder.set(TenorCurveNodeMetadata.meta().tenor(), TENOR_10Y);
    builder.set(TenorCurveNodeMetadata.meta().label(), "10 year");
    TenorCurveNodeMetadata test = builder.build();
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getLabel()).isEqualTo("10 year");
    assertThat(test.getIdentifier()).isEqualTo(TENOR_10Y);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends TenorCurveNodeMetadata> builder = TenorCurveNodeMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(TENOR_10Y);
    coverImmutableBean(test);
    TenorCurveNodeMetadata test2 = TenorCurveNodeMetadata.of(TENOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorCurveNodeMetadata test = TenorCurveNodeMetadata.of(TENOR_10Y);
    assertSerialization(test);
  }

}
