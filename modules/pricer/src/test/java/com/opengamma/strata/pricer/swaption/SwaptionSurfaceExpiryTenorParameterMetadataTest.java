/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.BeanBuilder;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SwaptionSurfaceExpiryTenorParameterMetadata}.
 */
public class SwaptionSurfaceExpiryTenorParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double TENOR = 36d;

  @Test
  public void test_of_noLabel() {
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, TENOR));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, TENOR).toString());
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withLabel() {
    String label = "(1.5Y, 36M)";
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR, label);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, TENOR));
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorParameterMetadata> builder =
        SwaptionSurfaceExpiryTenorParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().tenor(), TENOR);
    SwaptionSurfaceExpiryTenorParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(pair.toString());
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorParameterMetadata> builder =
        SwaptionSurfaceExpiryTenorParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    String label = "(1.5Y, 36M)";
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().tenor(), TENOR);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpiryTenorParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionSurfaceExpiryTenorParameterMetadata test1 =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    coverImmutableBean(test1);
    SwaptionSurfaceExpiryTenorParameterMetadata test2 =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertSerialization(test);
  }

}
