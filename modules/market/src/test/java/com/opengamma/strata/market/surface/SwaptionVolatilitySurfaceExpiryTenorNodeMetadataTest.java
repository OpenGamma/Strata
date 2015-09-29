/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SwaptionVolatilitySurfaceExpiryTenorNodeMetadata}.
 */
@Test
public class SwaptionVolatilitySurfaceExpiryTenorNodeMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double TENOR = 36d;

  public void test_of_noLabel() {
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, TENOR).toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 36M)";
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, label);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SwaptionVolatilitySurfaceExpiryTenorNodeMetadata> builder =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    builder.set(SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SwaptionVolatilitySurfaceExpiryTenorNodeMetadata> builder =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    String label = "(1.5Y, 36M)";
    builder.set(SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    builder.set(SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.meta().label(), label);
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test1 =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    coverImmutableBean(test1);
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test2 =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(2.5d, 60d, "(2.5, 60)");
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionVolatilitySurfaceExpiryTenorNodeMetadata test =
        SwaptionVolatilitySurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR);
    assertSerialization(test);
  }

}
