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
 * Test {@link SwaptionSurfaceExpiryStrikeParameterMetadata}.
 */
public class SwaptionSurfaceExpiryStrikeParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double STRIKE = 0.25d;

  @Test
  public void test_of_noLabel() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE).toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withLabel() {
    String label = "(1.5Y, 0.25)";
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE, label);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> builder =
        SwaptionSurfaceExpiryStrikeParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().strike(), STRIKE);
    SwaptionSurfaceExpiryStrikeParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(pair.toString());
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> builder =
        SwaptionSurfaceExpiryStrikeParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    String label = "(1.5Y, 0.25)";
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().strike(), STRIKE);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpiryStrikeParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test1 =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    coverImmutableBean(test1);
    SwaptionSurfaceExpiryStrikeParameterMetadata test2 =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    assertSerialization(test);
  }

}
