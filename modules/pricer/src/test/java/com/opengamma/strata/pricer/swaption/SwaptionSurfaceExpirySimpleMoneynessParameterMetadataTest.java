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
 * Test {@link SwaptionSurfaceExpirySimpleMoneynessParameterMetadata}.
 */
public class SwaptionSurfaceExpirySimpleMoneynessParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double SIMPLE_MONEYNESS = 0.25d;

  @Test
  public void test_of_noLabel() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS));
    assertThat(test.getLabel()).isEqualTo(Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS).toString());
    assertThat(test.getSimpleMoneyness()).isEqualTo(SIMPLE_MONEYNESS);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_of_withLabel() {
    String label = "(1.5Y, 0.25)";
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS, label);
    assertThat(test.getIdentifier()).isEqualTo(Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS));
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getSimpleMoneyness()).isEqualTo(SIMPLE_MONEYNESS);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpirySimpleMoneynessParameterMetadata> builder =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().simpleMoneyness(), SIMPLE_MONEYNESS);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(pair.toString());
    assertThat(test.getSimpleMoneyness()).isEqualTo(SIMPLE_MONEYNESS);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  @Test
  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpirySimpleMoneynessParameterMetadata> builder =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    String label = "(1.5Y, 0.25)";
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().simpleMoneyness(), SIMPLE_MONEYNESS);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test = builder.build();
    assertThat(test.getIdentifier()).isEqualTo(pair);
    assertThat(test.getLabel()).isEqualTo(label);
    assertThat(test.getSimpleMoneyness()).isEqualTo(SIMPLE_MONEYNESS);
    assertThat(test.getYearFraction()).isEqualTo(TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test1 =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    coverImmutableBean(test1);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test2 =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    assertSerialization(test);
  }

}
