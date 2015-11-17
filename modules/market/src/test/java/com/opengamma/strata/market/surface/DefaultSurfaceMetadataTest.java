/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.market.ValueType;

/**
 * Test {@link DefaultSurfaceMetadata}.
 */
@Test
public class DefaultSurfaceMetadataTest {

  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);

  //-------------------------------------------------------------------------
  public void test_of_String_noMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_builder() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .zValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(ImmutableList.of(SurfaceParameterMetadata.empty()))
        .build();
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getZValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getDayCount().get()).isEqualTo(ACT_365F);
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(SurfaceParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void test_withParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME)
        .withParameterMetadata(ImmutableList.of(SurfaceParameterMetadata.empty()));
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(SurfaceParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    coverImmutableBean(test);
    DefaultSurfaceMetadata test2 = DefaultSurfaceMetadata.builder()
        .surfaceName(SurfaceName.of("Test"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .zValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(SurfaceParameterMetadata.empty())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertSerialization(test);
  }

}
