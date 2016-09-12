/**
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
 * Test {@link SwaptionSurfaceExpiryStrikeParameterMetadata}.
 */
@Test
public class SwaptionSurfaceExpiryStrikeParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double STRIKE = 0.25d;

  public void test_of_noLabel() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE).toString());
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 0.25)";
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE, label);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> builder =
        SwaptionSurfaceExpiryStrikeParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().strike(), STRIKE);
    SwaptionSurfaceExpiryStrikeParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryStrikeParameterMetadata> builder =
        SwaptionSurfaceExpiryStrikeParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, STRIKE);
    String label = "(1.5Y, 0.25)";
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().strike(), STRIKE);
    builder.set(SwaptionSurfaceExpiryStrikeParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpiryStrikeParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test1 =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    coverImmutableBean(test1);
    SwaptionSurfaceExpiryStrikeParameterMetadata test2 =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSurfaceExpiryStrikeParameterMetadata test =
        SwaptionSurfaceExpiryStrikeParameterMetadata.of(TIME_TO_EXPIRY, STRIKE);
    assertSerialization(test);
  }

}
