/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.common;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.Period;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.option.MoneynessStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;

/**
 * Test {@link GenericVolatilitySurfacePeriodParameterMetadata}.
 */
@Test
public class GenericVolatilitySurfacePeriodParameterMetadataTest {

  private static final Period TIME_TO_EXPIRY = Period.ofYears(2);
  private static final LogMoneynessStrike STRIKE1 = LogMoneynessStrike.of(0.98d);
  private static final SimpleStrike STRIKE2 = SimpleStrike.of(1.05);

  public void test_of_withStrikeType() {
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, STRIKE1));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getPeriod(), TIME_TO_EXPIRY);
  }

  public void test_of_withLabel() {
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE2);
    String label = "(2, 1.35)";
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE2, label);
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE2);
    assertEquals(test.getPeriod(), TIME_TO_EXPIRY);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().strike(), STRIKE1);
    GenericVolatilitySurfacePeriodParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, STRIKE1.getLabel()).toString());
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getPeriod(), TIME_TO_EXPIRY);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    Pair<Period, Strike> pair = Pair.of(TIME_TO_EXPIRY, STRIKE1);
    String label = "(2, 0.75)";
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().strike(), STRIKE1);
    builder.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().label(), label);
    GenericVolatilitySurfacePeriodParameterMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getStrike(), STRIKE1);
    assertEquals(test.getPeriod(), TIME_TO_EXPIRY);
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder1 =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    assertThrowsIllegalArg(() -> builder1.build());
    BeanBuilder<? extends GenericVolatilitySurfacePeriodParameterMetadata> builder2 =
        GenericVolatilitySurfacePeriodParameterMetadata.meta().builder();
    builder2.set(GenericVolatilitySurfacePeriodParameterMetadata.meta().period(), TIME_TO_EXPIRY);
    assertThrowsIllegalArg(() -> builder2.build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericVolatilitySurfacePeriodParameterMetadata test1 =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    coverImmutableBean(test1);
    GenericVolatilitySurfacePeriodParameterMetadata test2 =
        GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofMonths(3), MoneynessStrike.of(1.1d));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    GenericVolatilitySurfacePeriodParameterMetadata test =
        GenericVolatilitySurfacePeriodParameterMetadata.of(TIME_TO_EXPIRY, STRIKE1);
    assertSerialization(test);
  }

}
