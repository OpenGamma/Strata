/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.meta;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link GenericVolatilitySurfaceYearFractionMetadata}.
 */
@Test
public class GenericVolatilitySurfaceYearFractionMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final LogMoneynessStrike STRIKE1 = LogMoneynessStrike.of(0.98d);
  private static final SimpleStrike STRIKE2 = SimpleStrike.of(1.05);

  public void test_of_withStrikeType() {
    GenericVolatilitySurfaceYearFractionMetadata test =
        GenericVolatilitySurfaceYearFractionMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE1));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE2);
    String label = "(1.5, 1.35)";
    GenericVolatilitySurfaceYearFractionMetadata test =
        GenericVolatilitySurfaceYearFractionMetadata.of(TIME_TO_EXPIRY, STRIKE2, label);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE2);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionMetadata> builder =
        GenericVolatilitySurfaceYearFractionMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    builder.set(GenericVolatilitySurfaceYearFractionMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfaceYearFractionMetadata.meta().strike(), STRIKE1);
    GenericVolatilitySurfaceYearFractionMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionMetadata> builder =
        GenericVolatilitySurfaceYearFractionMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(1.5, 0.75)";
    builder.set(GenericVolatilitySurfaceYearFractionMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfaceYearFractionMetadata.meta().strike(), STRIKE1);
    builder.set(GenericVolatilitySurfaceYearFractionMetadata.meta().label(), label);
    GenericVolatilitySurfaceYearFractionMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionMetadata> builder1 =
        GenericVolatilitySurfaceYearFractionMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder1.build());
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionMetadata> builder2 =
        GenericVolatilitySurfaceYearFractionMetadata.meta().builder();
    builder2.set(GenericVolatilitySurfaceYearFractionMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    assertThrowsIllegalArg(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericVolatilitySurfaceYearFractionMetadata test1 =
        GenericVolatilitySurfaceYearFractionMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    coverImmutableBean(test1);
    GenericVolatilitySurfaceYearFractionMetadata test2 =
        GenericVolatilitySurfaceYearFractionMetadata.of(3d, MoneynessStrike.of(1.1d));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    GenericVolatilitySurfaceYearFractionMetadata test =
        GenericVolatilitySurfaceYearFractionMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertSerialization(test);
  }

}
