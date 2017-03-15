/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SwaptionSurfaceExpirySimpleMoneynessParameterMetadata}.
 */
@Test
public class SwaptionSurfaceExpirySimpleMoneynessParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double SIMPLE_MONEYNESS = 0.25d;

  public void test_of_noLabel() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS).toString());
    assertEquals(test.getSimpleMoneyness(), SIMPLE_MONEYNESS);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 0.25)";
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS, label);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getSimpleMoneyness(), SIMPLE_MONEYNESS);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpirySimpleMoneynessParameterMetadata> builder =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().simpleMoneyness(), SIMPLE_MONEYNESS);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getSimpleMoneyness(), SIMPLE_MONEYNESS);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpirySimpleMoneynessParameterMetadata> builder =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    String label = "(1.5Y, 0.25)";
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().simpleMoneyness(), SIMPLE_MONEYNESS);
    builder.set(SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getSimpleMoneyness(), SIMPLE_MONEYNESS);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test1 =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    coverImmutableBean(test1);
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test2 =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSurfaceExpirySimpleMoneynessParameterMetadata test =
        SwaptionSurfaceExpirySimpleMoneynessParameterMetadata.of(TIME_TO_EXPIRY, SIMPLE_MONEYNESS);
    assertSerialization(test);
  }

}
