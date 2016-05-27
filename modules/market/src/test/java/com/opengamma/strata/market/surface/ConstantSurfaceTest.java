/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link ConstantSurface}.
 */
@Test
public class ConstantSurfaceTest {

  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.of(SURFACE_NAME);
  private static final SurfaceMetadata METADATA2 = DefaultSurfaceMetadata.of("Test2");
  private static final double VALUE = 6d;

  //-------------------------------------------------------------------------
  public void test_of_String() {
    ConstantSurface test = ConstantSurface.of(NAME, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getZValue()).isEqualTo(VALUE);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantSurface.of(NAME, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isEqualTo(ConstantSurface.of(NAME, VALUE + 1d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantSurface.of(METADATA2, VALUE));
  }

  public void test_of_SurfaceName() {
    ConstantSurface test = ConstantSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getZValue()).isEqualTo(VALUE);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantSurface.of(NAME, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isEqualTo(ConstantSurface.of(NAME, VALUE + 1d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantSurface.of(METADATA2, VALUE));
  }

  public void test_of_SurfaceMetadata() {
    ConstantSurface test = ConstantSurface.of(METADATA, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getZValue()).isEqualTo(VALUE);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantSurface.of(NAME, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isEqualTo(ConstantSurface.of(NAME, VALUE + 1d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantSurface.of(METADATA2, VALUE));
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    ConstantSurface test = ConstantSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.zValue(0d, 0d)).isEqualTo(VALUE);
    assertThat(test.zValue(-10d, 10d)).isEqualTo(VALUE);
    assertThat(test.zValue(100d, -100d)).isEqualTo(VALUE);

    assertThat(test.zValueParameterSensitivity(0d, 0d).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(-10d, 10d).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(100d, -100d).getSensitivity().get(0)).isEqualTo(1d);
  }

  public void test_lookup_byPair() {
    ConstantSurface test = ConstantSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.zValue(DoublesPair.of(0d, 0d))).isEqualTo(VALUE);
    assertThat(test.zValue(DoublesPair.of(-10d, 10d))).isEqualTo(VALUE);
    assertThat(test.zValue(DoublesPair.of(100d, -100d))).isEqualTo(VALUE);

    assertThat(test.zValueParameterSensitivity(DoublesPair.of(0d, 0d)).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(DoublesPair.of(-10d, 10d)).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(DoublesPair.of(100d, -100d)).getSensitivity().get(0)).isEqualTo(1d);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ConstantSurface test = ConstantSurface.of(SURFACE_NAME, VALUE);
    coverImmutableBean(test);
    ConstantSurface test2 = ConstantSurface.of("Coverage", 9d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ConstantSurface test = ConstantSurface.of(SURFACE_NAME, VALUE);
    assertSerialization(test);
  }

}
