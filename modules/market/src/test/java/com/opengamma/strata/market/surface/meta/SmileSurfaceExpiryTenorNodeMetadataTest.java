/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link SmileSurfaceExpiryTenorNodeMetadata}.
 */
@Test
public class SmileSurfaceExpiryTenorNodeMetadataTest {

  private static final double TIME_TO_EXPIRY = 1.5d;
  private static final double TENOR = 36d;
  private static final DoubleArray SENSI = DoubleArray.of(10.0, 11.0, 12.0, 13.4, 14.5);

  public void test_of_noLabel() {
    SmileSurfaceExpiryTenorNodeMetadata test =
        SmileSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, SENSI);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), Pair.of(TIME_TO_EXPIRY, TENOR).toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
    assertEquals(test.getModelParameterSensitivity(), SENSI);
  }

  public void test_of_withLabel() {
    String label = "(1.5Y, 36M)";
    SmileSurfaceExpiryTenorNodeMetadata test =
        SmileSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, label, SENSI);
    assertEquals(test.getIdentifier(), Pair.of(TIME_TO_EXPIRY, TENOR));
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
    assertEquals(test.getModelParameterSensitivity(), SENSI);
  }

  public void test_builder_noLabel() {
    BeanBuilder<? extends SmileSurfaceExpiryTenorNodeMetadata> builder =
        SmileSurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().modelParameterSensitivity(), SENSI);
    SmileSurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), pair.toString());
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
    assertEquals(test.getModelParameterSensitivity(), SENSI);
  }

  public void test_builder_withLabel() {
    BeanBuilder<? extends SmileSurfaceExpiryTenorNodeMetadata> builder =
        SmileSurfaceExpiryTenorNodeMetadata.meta().builder();
    Pair<Double, Double> pair = Pair.of(TIME_TO_EXPIRY, TENOR);
    String label = "(1.5Y, 36M)";
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().yearFraction(), TIME_TO_EXPIRY);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().tenor(), TENOR);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().label(), label);
    builder.set(SmileSurfaceExpiryTenorNodeMetadata.meta().modelParameterSensitivity(), SENSI);
    SmileSurfaceExpiryTenorNodeMetadata test = builder.build();
    assertEquals(test.getIdentifier(), pair);
    assertEquals(test.getLabel(), label);
    assertEquals(test.getTenor(), TENOR);
    assertEquals(test.getYearFraction(), TIME_TO_EXPIRY);
    assertEquals(test.getModelParameterSensitivity(), SENSI);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SmileSurfaceExpiryTenorNodeMetadata test1 =
        SmileSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, SENSI);
    coverImmutableBean(test1);
    SmileSurfaceExpiryTenorNodeMetadata test2 =
        SmileSurfaceExpiryTenorNodeMetadata.of(2.5d, 60d, "(2.5, 60)", DoubleArray.of(11.0, 12.0, 13.4, 14.5));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SmileSurfaceExpiryTenorNodeMetadata test =
        SmileSurfaceExpiryTenorNodeMetadata.of(TIME_TO_EXPIRY, TENOR, SENSI);
    assertSerialization(test);
  }
  
  
  
}
