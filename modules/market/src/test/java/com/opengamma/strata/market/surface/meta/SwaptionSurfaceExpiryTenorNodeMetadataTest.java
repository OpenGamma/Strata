/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.meta;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SwaptionSurfaceExpiryTenorNodeMetadata}.
 */
@Test
public class SwaptionSurfaceExpiryTenorNodeMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double TENOR = 36d;

  public void test_of_noLabel() {
    SwaptionSurfaceExpiryTenorNodeMetadata test =
        SwaptionSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, TENOR).toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 36M)";
    SwaptionSurfaceExpiryTenorNodeMetadata test =
        SwaptionSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, label);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorNodeMetadata> builder =
        SwaptionSurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    builder.set(SwaptionSurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    SwaptionSurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionSurfaceExpiryTenorNodeMetadata> builder =
        SwaptionSurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    String label = "(1.5Y, 36M)";
    builder.set(SwaptionSurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionSurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    builder.set(SwaptionSurfaceExpiryTenorNodeMetadata.meta().label(), label);
    SwaptionSurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSurfaceExpiryTenorNodeMetadata test1 =
        SwaptionSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    coverImmutableBean(test1);
    SwaptionSurfaceExpiryTenorNodeMetadata test2 =
        SwaptionSurfaceExpiryTenorNodeMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSurfaceExpiryTenorNodeMetadata test =
        SwaptionSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertSerialization(test);
  }

}
