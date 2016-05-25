/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link DefaultSurfaceMetadata}.
 */
@Test
public class DefaultSurfaceMetadataTest {

  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);
  private static final SurfaceInfoType<String> DESCRIPTION = SurfaceInfoType.of("Description");

  //-------------------------------------------------------------------------
  public void test_of_String_noMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_builder1() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME.toString())
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .zValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(DESCRIPTION, "Hello")
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .build();
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getZValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getInfo(SurfaceInfoType.DAY_COUNT)).isEqualTo(ACT_365F);
    assertThat(test.findInfo(SurfaceInfoType.DAY_COUNT)).isEqualTo(Optional.of(ACT_365F));
    assertThat(test.getInfo(DESCRIPTION)).isEqualTo("Hello");
    assertThat(test.findInfo(DESCRIPTION)).isEqualTo(Optional.of("Hello"));
    assertThat(test.findInfo(SurfaceInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  public void test_builder2() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .zValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .addInfo(SurfaceInfoType.DAY_COUNT, null)
        .addInfo(DESCRIPTION, "Hello")
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .build();
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getZValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThrowsIllegalArg(() -> test.getInfo(SurfaceInfoType.DAY_COUNT));
    assertThat(test.findInfo(SurfaceInfoType.DAY_COUNT)).isEmpty();
    assertThat(test.getInfo(DESCRIPTION)).isEqualTo("Hello");
    assertThat(test.findInfo(DESCRIPTION)).isEqualTo(Optional.of("Hello"));
    assertThat(test.findInfo(SurfaceInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  public void test_builder3() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .clearParameterMetadata()
        .build();
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  //-------------------------------------------------------------------------
  public void test_withInfo() {
    DefaultSurfaceMetadata base = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertThat(base.findInfo(SurfaceInfoType.DAY_COUNT).isPresent()).isFalse();
    DefaultSurfaceMetadata test = base.withInfo(SurfaceInfoType.DAY_COUNT, ACT_360);
    assertThat(base.findInfo(SurfaceInfoType.DAY_COUNT).isPresent()).isFalse();
    assertThat(test.findInfo(SurfaceInfoType.DAY_COUNT).isPresent()).isTrue();
  }

  //-------------------------------------------------------------------------
  public void test_withParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME)
        .withParameterMetadata(ImmutableList.of(ParameterMetadata.empty()));
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  public void test_withParameterMetadata_clearWhenEmpty() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME).withParameterMetadata(null);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_withParameterMetadata_clearWhenNonEmpty() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME)
        .withParameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .withParameterMetadata(null);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getZValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
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
        .parameterMetadata(ParameterMetadata.empty())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertSerialization(test);
  }

}
