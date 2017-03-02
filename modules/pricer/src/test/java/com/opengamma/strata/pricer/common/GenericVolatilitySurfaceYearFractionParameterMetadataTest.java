/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.common;

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
 * Test {@link GenericVolatilitySurfaceYearFractionParameterMetadata}.
 */
@Test
public class GenericVolatilitySurfaceYearFractionParameterMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final LogMoneynessStrike STRIKE1 = LogMoneynessStrike.of(0.98d);
  private static final SimpleStrike STRIKE2 = SimpleStrike.of(1.05);

  public void test_of_withStrikeType() {
    GenericVolatilitySurfaceYearFractionParameterMetadata test =
        GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE1));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE2);
    String label = "(1.5, 1.35)";
    GenericVolatilitySurfaceYearFractionParameterMetadata test =
        GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE2, label);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE2);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionParameterMetadata> builder =
        GenericVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    builder.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().strike(), STRIKE1);
    GenericVolatilitySurfaceYearFractionParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionParameterMetadata> builder =
        GenericVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    Pair<Double, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(1.5, 0.75)";
    builder.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().strike(), STRIKE1);
    builder.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().label(), label);
    GenericVolatilitySurfaceYearFractionParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionParameterMetadata> builder1 =
        GenericVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder1.build());
    BeanBuilder<? extends GenericVolatilitySurfaceYearFractionParameterMetadata> builder2 =
        GenericVolatilitySurfaceYearFractionParameterMetadata.meta().builder();
    builder2.set(GenericVolatilitySurfaceYearFractionParameterMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    assertThrowsIllegalArg(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericVolatilitySurfaceYearFractionParameterMetadata test1 =
        GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    coverImmutableBean(test1);
    GenericVolatilitySurfaceYearFractionParameterMetadata test2 =
        GenericVolatilitySurfaceYearFractionParameterMetadata.of(3d, MoneynessStrike.of(1.1d));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    GenericVolatilitySurfaceYearFractionParameterMetadata test =
        GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertSerialization(test);
  }

}
