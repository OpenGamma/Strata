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
 * Test {@link SwaptionSurfaceExpiryTenorParameterMetadata}.
 */
@Test
public class SwaptionSurfaceExpiryTenorParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double TENOR = 36d;

  public void test_of_noLabel() {
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, TENOR).toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 36M)";
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR, label);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorParameterMetadata> builder =
        SwaptionSurfaceExpiryTenorParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().tenor(), TENOR);
    SwaptionSurfaceExpiryTenorParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorParameterMetadata> builder =
        SwaptionSurfaceExpiryTenorParameterMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    String label = "(1.5Y, 36M)";
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().tenor(), TENOR);
    builder.set(SwaptionSurfaceExpiryTenorParameterMetadata.meta().label(), label);
    SwaptionSurfaceExpiryTenorParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSurfaceExpiryTenorParameterMetadata test1 =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    coverImmutableBean(test1);
    SwaptionSurfaceExpiryTenorParameterMetadata test2 =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSurfaceExpiryTenorParameterMetadata test =
        SwaptionSurfaceExpiryTenorParameterMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertSerialization(test);
  }

}
